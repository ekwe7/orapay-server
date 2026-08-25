package com.orapay.split.service.impl;

import com.orapay.common.event.EventPublisher;
import com.orapay.common.exception.BusinessRuleException;
import com.orapay.common.exception.InsufficientFundsException;
import com.orapay.split.dto.request.CreateSplitTemplateRequestDto;
import com.orapay.split.dto.request.MerchantCheckoutRequestDto;
import com.orapay.split.dto.request.SplitAllocationRuleDto;
import com.orapay.split.dto.request.SplitPaymentRequestDto;
import com.orapay.split.dto.response.SplitPaymentResponseDto;
import com.orapay.split.dto.response.SplitTemplateResponseDto;
import com.orapay.split.event.SplitPaymentCompletedEvent;
import com.orapay.split.event.SplitPaymentFailedEvent;
import com.orapay.split.event.SplitPaymentInitiatedEvent;
import com.orapay.split.mapper.SplitPaymentMapper;
import com.orapay.split.model.SplitAllocation;
import com.orapay.split.model.SplitOrder;
import com.orapay.split.model.SplitTemplate;
import com.orapay.split.model.SplitTemplateRule;
import com.orapay.split.repository.SplitOrderRepository;
import com.orapay.split.repository.SplitTemplateRepository;
import com.orapay.split.service.SplitPaymentService;
import com.orapay.split.strategy.SplitCalculationStrategy;
import com.orapay.wallet.model.Wallet;
import com.orapay.wallet.repository.WalletRepository;
import com.orapay.wallet.service.impl.WalletLockManager;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SplitPaymentServiceImpl implements SplitPaymentService {

    private final WalletLockManager walletLockManager;
    private final WalletRepository walletRepository;
    private final SplitOrderRepository splitOrderRepository;
    private final SplitTemplateRepository splitTemplateRepository;
    private final SplitPaymentMapper splitPaymentMapper;
    private final EventPublisher eventPublisher;
    private final SplitCalculationStrategy percentageSplitStrategy;
    private final SplitCalculationStrategy fixedFeeSplitStrategy;

    private final Counter splitRequestsCounter;
    private final Counter splitSuccessCounter;
    private final Counter splitFailedCounter;
    private final Timer splitExecutionTimer;
    private final MeterRegistry meterRegistry;

    public SplitPaymentServiceImpl(
            WalletLockManager walletLockManager,
            WalletRepository walletRepository,
            SplitOrderRepository splitOrderRepository,
            SplitTemplateRepository splitTemplateRepository,
            SplitPaymentMapper splitPaymentMapper,
            EventPublisher eventPublisher,
            @Qualifier("percentageSplitStrategy") SplitCalculationStrategy percentageSplitStrategy,
            @Qualifier("fixedFeeSplitStrategy") SplitCalculationStrategy fixedFeeSplitStrategy,
            MeterRegistry meterRegistry
    ) {
        this.walletLockManager = walletLockManager;
        this.walletRepository = walletRepository;
        this.splitOrderRepository = splitOrderRepository;
        this.splitTemplateRepository = splitTemplateRepository;
        this.splitPaymentMapper = splitPaymentMapper;
        this.eventPublisher = eventPublisher;
        this.percentageSplitStrategy = percentageSplitStrategy;
        this.fixedFeeSplitStrategy = fixedFeeSplitStrategy;
        this.meterRegistry = meterRegistry;

        this.splitRequestsCounter = Counter.builder("split.requests.total")
                .description("Total number of split payment requests")
                .register(meterRegistry);

        this.splitSuccessCounter = Counter.builder("split.success.total")
                .description("Total number of successful split payments")
                .register(meterRegistry);

        this.splitFailedCounter = Counter.builder("split.failed.total")
                .description("Total number of failed split payments")
                .register(meterRegistry);

        this.splitExecutionTimer = Timer.builder("split.execution.duration")
                .description("Duration spent processing split payments")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
    }

    @Override
    @Transactional
    public SplitPaymentResponseDto processSplitPayment(SplitPaymentRequestDto requestDto) {
        Timer.Sample sample = Timer.start(meterRegistry);
        splitRequestsCounter.increment();

        if (requestDto == null || requestDto.getAllocations() == null || requestDto.getAllocations().isEmpty()) {
            throw new BusinessRuleException("Split payment request must contain at least one allocation rule");
        }

        SplitOrder pendingSplitOrder = null;
        try {
            // Phase 1: Gather involved wallet IDs & acquire locks in sorted order
            Set<UUID> allWalletIds = new HashSet<>();
            allWalletIds.add(requestDto.getPayerWalletId());
            for (SplitAllocationRuleDto rule : requestDto.getAllocations()) {
                allWalletIds.add(rule.getRecipientWalletId());
            }

            Map<UUID, Wallet> lockedWallets = walletLockManager.acquireLocksAsMap(allWalletIds);

            Wallet payerWallet = lockedWallets.get(requestDto.getPayerWalletId());
            if (payerWallet == null) {
                throw new BusinessRuleException("Payer wallet not found with ID: " + requestDto.getPayerWalletId());
            }

            // Phase 2: Select strategy
            SplitCalculationStrategy strategy = resolveStrategy(requestDto.getAllocations());

            // Phase 3: Compute split allocations with zero penny leakage
            Map<UUID, Long> calculatedSplits = strategy.calculateSplits(
                    requestDto.getTotalAmountInMinorUnits(),
                    requestDto.getAllocations()
            );

            // Phase 4: Validate funds
            if (payerWallet.getAvailableBalanceInMinorUnits() < requestDto.getTotalAmountInMinorUnits()) {
                throw new InsufficientFundsException(String.format(
                        "Insufficient funds in payer wallet. Required: %d, Available: %d",
                        requestDto.getTotalAmountInMinorUnits(),
                        payerWallet.getAvailableBalanceInMinorUnits()
                ));
            }

            // Phase 5: Create PENDING SplitOrder & publish SplitPaymentInitiatedEvent
            pendingSplitOrder = new SplitOrder();
            pendingSplitOrder.setPayerWallet(payerWallet);
            pendingSplitOrder.setTotalAmountInMinorUnits(requestDto.getTotalAmountInMinorUnits());
            pendingSplitOrder.setCurrencyCode(requestDto.getCurrencyCode());
            pendingSplitOrder.setStatus(SplitOrder.SplitOrderStatus.PENDING);
            pendingSplitOrder = splitOrderRepository.save(pendingSplitOrder);

            eventPublisher.publishEvent(new SplitPaymentInitiatedEvent(this, pendingSplitOrder));

            // Phase 6: Perform atomic balance transfers (Debit Payer, Credit Recipients)
            payerWallet.setAvailableBalanceInMinorUnits(
                    payerWallet.getAvailableBalanceInMinorUnits() - requestDto.getTotalAmountInMinorUnits()
            );

            for (Map.Entry<UUID, Long> entry : calculatedSplits.entrySet()) {
                Wallet recipientWallet = lockedWallets.get(entry.getKey());
                if (recipientWallet == null) {
                    throw new BusinessRuleException("Recipient wallet not found with ID: " + entry.getKey());
                }
                recipientWallet.setAvailableBalanceInMinorUnits(
                        recipientWallet.getAvailableBalanceInMinorUnits() + entry.getValue()
                );
            }

            // Phase 7: Save allocations & update SplitOrder status SETTLED
            final SplitOrder targetOrder = pendingSplitOrder;
            List<SplitAllocation> allocations = calculatedSplits.entrySet().stream()
                    .map(entry -> {
                        SplitAllocation allocation = new SplitAllocation();
                        allocation.setSplitOrder(targetOrder);
                        allocation.setRecipientWallet(lockedWallets.get(entry.getKey()));
                        allocation.setAllocatedAmountInMinorUnits(entry.getValue());
                        return allocation;
                    })
                    .collect(Collectors.toList());

            pendingSplitOrder.setAllocations(allocations);
            pendingSplitOrder.setStatus(SplitOrder.SplitOrderStatus.SETTLED);
            SplitOrder settledSplitOrder = splitOrderRepository.save(pendingSplitOrder);

            // Phase 8: Publish SplitPaymentCompletedEvent
            eventPublisher.publishEvent(new SplitPaymentCompletedEvent(this, settledSplitOrder));

            splitSuccessCounter.increment();
            return splitPaymentMapper.mapToSplitPaymentResponseDto(settledSplitOrder);

        } catch (Exception ex) {
            splitFailedCounter.increment();
            log.error("Split payment failed for payer wallet ID: [{}]", requestDto.getPayerWalletId(), ex);

            if (pendingSplitOrder != null && pendingSplitOrder.getSplitOrderId() != null) {
                pendingSplitOrder.setStatus(SplitOrder.SplitOrderStatus.FAILED);
                splitOrderRepository.save(pendingSplitOrder);
                eventPublisher.publishEvent(new SplitPaymentFailedEvent(this, pendingSplitOrder, ex.getMessage()));
            }

            throw ex;
        } finally {
            sample.stop(splitExecutionTimer);
        }
    }

    @Override
    @Transactional
    public SplitTemplateResponseDto createSplitTemplate(CreateSplitTemplateRequestDto requestDto) {
        Wallet merchantWallet = walletRepository.findById(requestDto.getMerchantWalletId())
                .orElseThrow(() -> new BusinessRuleException("Merchant wallet not found with ID: " + requestDto.getMerchantWalletId()));

        SplitTemplate template = new SplitTemplate();
        template.setMerchantWallet(merchantWallet);
        template.setTemplateName(requestDto.getTemplateName());
        template.setFeeCategory(requestDto.getFeeCategory());
        template.setActive(true);

        List<SplitTemplateRule> rules = requestDto.getRules().stream().map(ruleDto -> {
            Wallet recipientWallet = walletRepository.findById(ruleDto.getRecipientWalletId())
                    .orElseThrow(() -> new BusinessRuleException("Recipient wallet not found with ID: " + ruleDto.getRecipientWalletId()));

            SplitTemplateRule rule = new SplitTemplateRule();
            rule.setSplitTemplate(template);
            rule.setRecipientWallet(recipientWallet);
            rule.setRecipientName(ruleDto.getRecipientName());
            rule.setPercentage(ruleDto.getPercentage());
            rule.setFixedAmountInMinorUnits(ruleDto.getFixedAmountInMinorUnits());
            return rule;
        }).collect(Collectors.toList());

        template.setRules(rules);
        SplitTemplate savedTemplate = splitTemplateRepository.save(template);

        return mapToTemplateResponseDto(savedTemplate);
    }

    @Override
    @Transactional
    public SplitPaymentResponseDto processMerchantCheckout(MerchantCheckoutRequestDto checkoutDto) {
        SplitTemplate template = splitTemplateRepository
                .findByMerchantWallet_WalletIdAndFeeCategoryAndActiveTrue(checkoutDto.getMerchantWalletId(), checkoutDto.getFeeCategory())
                .or(() -> splitTemplateRepository.findByMerchantWallet_WalletIdAndActiveTrue(checkoutDto.getMerchantWalletId()))
                .orElseThrow(() -> new BusinessRuleException("No active split agreement template found for merchant: " + checkoutDto.getMerchantWalletId()));

        List<SplitAllocationRuleDto> allocationRules = template.getRules().stream()
                .map(rule -> SplitAllocationRuleDto.builder()
                        .recipientWalletId(rule.getRecipientWallet().getWalletId())
                        .percentage(rule.getPercentage())
                        .fixedAmountInMinorUnits(rule.getFixedAmountInMinorUnits())
                        .build())
                .collect(Collectors.toList());

        SplitPaymentRequestDto splitPaymentRequest = SplitPaymentRequestDto.builder()
                .payerWalletId(checkoutDto.getPayerWalletId())
                .totalAmountInMinorUnits(checkoutDto.getTotalAmountInMinorUnits())
                .currencyCode(checkoutDto.getCurrencyCode())
                .allocations(allocationRules)
                .description(checkoutDto.getDescription())
                .build();

        return processSplitPayment(splitPaymentRequest);
    }

    private SplitCalculationStrategy resolveStrategy(List<SplitAllocationRuleDto> rules) {
        SplitAllocationRuleDto firstRule = rules.get(0);
        if (firstRule.getPercentage() != null) {
            return percentageSplitStrategy;
        } else if (firstRule.getFixedAmountInMinorUnits() != null) {
            return fixedFeeSplitStrategy;
        }
        throw new BusinessRuleException("Invalid allocation rule: specify either percentage or fixedAmountInMinorUnits");
    }

    private SplitTemplateResponseDto mapToTemplateResponseDto(SplitTemplate template) {
        return SplitTemplateResponseDto.builder()
                .templateId(template.getTemplateId())
                .merchantWalletId(template.getMerchantWallet().getWalletId())
                .templateName(template.getTemplateName())
                .feeCategory(template.getFeeCategory())
                .active(template.isActive())
                .createdAt(template.getCreatedAt())
                .rules(template.getRules().stream()
                        .map(rule -> SplitTemplateResponseDto.TemplateRuleResponseDto.builder()
                                .ruleId(rule.getRuleId())
                                .recipientWalletId(rule.getRecipientWallet().getWalletId())
                                .recipientName(rule.getRecipientName())
                                .percentage(rule.getPercentage())
                                .fixedAmountInMinorUnits(rule.getFixedAmountInMinorUnits())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
