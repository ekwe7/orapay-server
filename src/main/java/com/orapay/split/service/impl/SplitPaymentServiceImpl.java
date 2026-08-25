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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

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

    public SplitPaymentServiceImpl(
            WalletLockManager walletLockManager,
            WalletRepository walletRepository,
            SplitOrderRepository splitOrderRepository,
            SplitTemplateRepository splitTemplateRepository,
            SplitPaymentMapper splitPaymentMapper,
            EventPublisher eventPublisher,
            @Qualifier("percentageSplitStrategy") SplitCalculationStrategy percentageSplitStrategy,
            @Qualifier("fixedFeeSplitStrategy") SplitCalculationStrategy fixedFeeSplitStrategy
    ) {
        this.walletLockManager = walletLockManager;
        this.walletRepository = walletRepository;
        this.splitOrderRepository = splitOrderRepository;
        this.splitTemplateRepository = splitTemplateRepository;
        this.splitPaymentMapper = splitPaymentMapper;
        this.eventPublisher = eventPublisher;
        this.percentageSplitStrategy = percentageSplitStrategy;
        this.fixedFeeSplitStrategy = fixedFeeSplitStrategy;
    }

    /**
     * Core execution engine for multi-party split payments.
     * Takes an explicit request with allocations, locks involved wallets in deterministic order,
     * computes the exact split amounts, transfers balances, persists records, and emits a domain event.
     */
    @Override
    @Transactional
    public SplitPaymentResponseDto processSplitPayment(SplitPaymentRequestDto requestDto) {
        if (requestDto == null || requestDto.getAllocations() == null || requestDto.getAllocations().isEmpty()) {
            throw new BusinessRuleException("Split payment request must contain at least one allocation rule");
        }

        // ----------------------------------------------------------------------------------
        // Phase 1: Gather involved wallet IDs & acquire locks in sorted order (Deadlock Avoidance)
        // ----------------------------------------------------------------------------------
        Set<UUID> allWalletIds = new HashSet<>();
        allWalletIds.add(requestDto.getPayerWalletId());
        for (SplitAllocationRuleDto rule : requestDto.getAllocations()) {
            allWalletIds.add(rule.getRecipientWalletId());
        }

        // Acquire pessimistic write locks in deterministic sorted primary-key order
        Map<UUID, Wallet> lockedWallets = walletLockManager.acquireLocksAsMap(allWalletIds);

        Wallet payerWallet = lockedWallets.get(requestDto.getPayerWalletId());
        if (payerWallet == null) {
            throw new BusinessRuleException("Payer wallet not found with ID: " + requestDto.getPayerWalletId());
        }

        // ----------------------------------------------------------------------------------
        // Phase 2: Resolve & select calculation strategy (Percentage vs Fixed Fee)
        // ----------------------------------------------------------------------------------
        SplitCalculationStrategy strategy = resolveStrategy(requestDto.getAllocations());

        // ----------------------------------------------------------------------------------
        // Phase 3: Compute minor unit split allocations (Zero-Float Precision)
        // ----------------------------------------------------------------------------------
        Map<UUID, Long> calculatedSplits = strategy.calculateSplits(
                requestDto.getTotalAmountInMinorUnits(),
                requestDto.getAllocations()
        );

        // ----------------------------------------------------------------------------------
        // Phase 4: Validate payer's available balance
        // ----------------------------------------------------------------------------------
        if (payerWallet.getAvailableBalanceInMinorUnits() < requestDto.getTotalAmountInMinorUnits()) {
            throw new InsufficientFundsException(String.format(
                    "Insufficient funds in payer wallet. Required: %d, Available: %d",
                    requestDto.getTotalAmountInMinorUnits(),
                    payerWallet.getAvailableBalanceInMinorUnits()
            ));
        }

        // ----------------------------------------------------------------------------------
        // Phase 5: Execute atomic balance updates (Debit Payer, Credit Recipients)
        // ----------------------------------------------------------------------------------
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

        // ----------------------------------------------------------------------------------
        // Phase 6: Persist SplitOrder and SplitAllocation records
        // ----------------------------------------------------------------------------------
        SplitOrder splitOrder = new SplitOrder();
        splitOrder.setPayerWallet(payerWallet);
        splitOrder.setTotalAmountInMinorUnits(requestDto.getTotalAmountInMinorUnits());
        splitOrder.setCurrencyCode(requestDto.getCurrencyCode());
        splitOrder.setStatus(SplitOrder.SplitOrderStatus.SETTLED);

        List<SplitAllocation> allocations = calculatedSplits.entrySet().stream()
                .map(entry -> {
                    SplitAllocation allocation = new SplitAllocation();
                    allocation.setSplitOrder(splitOrder);
                    allocation.setRecipientWallet(lockedWallets.get(entry.getKey()));
                    allocation.setAllocatedAmountInMinorUnits(entry.getValue());
                    return allocation;
                })
                .collect(Collectors.toList());

        splitOrder.setAllocations(allocations);
        SplitOrder savedSplitOrder = splitOrderRepository.save(splitOrder);

        // ----------------------------------------------------------------------------------
        // Phase 7: Publish SplitPaymentCompletedEvent for Double-Entry Ledger Posting
        // ----------------------------------------------------------------------------------
        eventPublisher.publishEvent(new SplitPaymentCompletedEvent(this, savedSplitOrder));

        return splitPaymentMapper.mapToSplitPaymentResponseDto(savedSplitOrder);
    }

    /**
     * Registers a new merchant/school split agreement template (e.g. State 15%, LGA 10%, School 75%).
     */
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

    /**
     * Processes an automated merchant/school fee checkout payment.
     * The payer (student) pays into one merchant account without needing to know internal sub-accounts.
     */
    @Override
    @Transactional
    public SplitPaymentResponseDto processMerchantCheckout(MerchantCheckoutRequestDto checkoutDto) {
        // ----------------------------------------------------------------------------------
        // Phase 1: Fetch the pre-configured split template for this merchant & fee category
        // ----------------------------------------------------------------------------------
        SplitTemplate template = splitTemplateRepository
                .findByMerchantWallet_WalletIdAndFeeCategoryAndActiveTrue(checkoutDto.getMerchantWalletId(), checkoutDto.getFeeCategory())
                .or(() -> splitTemplateRepository.findByMerchantWallet_WalletIdAndActiveTrue(checkoutDto.getMerchantWalletId()))
                .orElseThrow(() -> new BusinessRuleException("No active split agreement template found for merchant: " + checkoutDto.getMerchantWalletId()));

        // ----------------------------------------------------------------------------------
        // Phase 2: Convert template rules into allocation rules (Percentages or Fixed Fees)
        // ----------------------------------------------------------------------------------
        List<SplitAllocationRuleDto> allocationRules = template.getRules().stream()
                .map(rule -> SplitAllocationRuleDto.builder()
                        .recipientWalletId(rule.getRecipientWallet().getWalletId())
                        .percentage(rule.getPercentage())
                        .fixedAmountInMinorUnits(rule.getFixedAmountInMinorUnits())
                        .build())
                .collect(Collectors.toList());

        // ----------------------------------------------------------------------------------
        // Phase 3: Construct internal SplitPaymentRequestDto with extracted rules
        // ----------------------------------------------------------------------------------
        SplitPaymentRequestDto splitPaymentRequest = SplitPaymentRequestDto.builder()
                .payerWalletId(checkoutDto.getPayerWalletId())
                .totalAmountInMinorUnits(checkoutDto.getTotalAmountInMinorUnits())
                .currencyCode(checkoutDto.getCurrencyCode())
                .allocations(allocationRules)
                .description(checkoutDto.getDescription())
                .build();

        // ----------------------------------------------------------------------------------
        // Phase 4: Delegate execution to core split payment engine (handles locks, math & ledger)
        // ----------------------------------------------------------------------------------
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
