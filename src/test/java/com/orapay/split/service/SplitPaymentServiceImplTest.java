package com.orapay.split.service;

import com.orapay.common.event.EventPublisher;
import com.orapay.split.dto.request.CreateSplitTemplateRequestDto;
import com.orapay.split.dto.request.MerchantCheckoutRequestDto;
import com.orapay.split.dto.response.SplitPaymentResponseDto;
import com.orapay.split.dto.response.SplitTemplateResponseDto;
import com.orapay.split.mapper.SplitPaymentMapper;
import com.orapay.split.model.SplitTemplate;
import com.orapay.split.repository.SplitOrderRepository;
import com.orapay.split.repository.SplitTemplateRepository;
import com.orapay.split.service.impl.SplitPaymentServiceImpl;
import com.orapay.split.strategy.FixedFeeSplitStrategy;
import com.orapay.split.strategy.PercentageSplitStrategy;
import com.orapay.split.util.RemainderBalancer;
import com.orapay.wallet.model.Wallet;
import com.orapay.wallet.repository.WalletRepository;
import com.orapay.wallet.service.impl.WalletLockManager;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SplitPaymentServiceImplTest {

    private WalletLockManager walletLockManager;
    private WalletRepository walletRepository;
    private SplitOrderRepository splitOrderRepository;
    private SplitTemplateRepository splitTemplateRepository;
    private SplitPaymentMapper splitPaymentMapper;
    private EventPublisher eventPublisher;
    private MeterRegistry meterRegistry;
    private SplitPaymentServiceImpl splitPaymentService;

    private UUID studentWalletId;
    private UUID schoolWalletId;
    private UUID stateGovtWalletId;
    private UUID lgaWalletId;

    private Wallet studentWallet;
    private Wallet schoolWallet;
    private Wallet stateGovtWallet;
    private Wallet lgaWallet;

    @BeforeEach
    void setUp() {
        walletLockManager = mock(WalletLockManager.class);
        walletRepository = mock(WalletRepository.class);
        splitOrderRepository = mock(SplitOrderRepository.class);
        splitTemplateRepository = mock(SplitTemplateRepository.class);
        splitPaymentMapper = new SplitPaymentMapper();
        eventPublisher = mock(EventPublisher.class);
        meterRegistry = new SimpleMeterRegistry();

        RemainderBalancer remainderBalancer = new RemainderBalancer();
        PercentageSplitStrategy percentageSplitStrategy = new PercentageSplitStrategy(remainderBalancer);
        FixedFeeSplitStrategy fixedFeeSplitStrategy = new FixedFeeSplitStrategy();

        splitPaymentService = new SplitPaymentServiceImpl(
                walletLockManager,
                walletRepository,
                splitOrderRepository,
                splitTemplateRepository,
                splitPaymentMapper,
                eventPublisher,
                percentageSplitStrategy,
                fixedFeeSplitStrategy,
                meterRegistry
        );

        studentWalletId = UUID.randomUUID();
        schoolWalletId = UUID.randomUUID();
        stateGovtWalletId = UUID.randomUUID();
        lgaWalletId = UUID.randomUUID();

        studentWallet = new Wallet();
        studentWallet.setWalletId(studentWalletId);
        studentWallet.setAvailableBalanceInMinorUnits(15000000L); // 150,000 NGN

        schoolWallet = new Wallet();
        schoolWallet.setWalletId(schoolWalletId);
        schoolWallet.setAvailableBalanceInMinorUnits(0L);

        stateGovtWallet = new Wallet();
        stateGovtWallet.setWalletId(stateGovtWalletId);
        stateGovtWallet.setAvailableBalanceInMinorUnits(0L);

        lgaWallet = new Wallet();
        lgaWallet.setWalletId(lgaWalletId);
        lgaWallet.setAvailableBalanceInMinorUnits(0L);
    }

    @Test
    @DisplayName("Should register a school split template successfully")
    void testCreateSplitTemplate() {
        when(walletRepository.findById(schoolWalletId)).thenReturn(Optional.of(schoolWallet));
        when(walletRepository.findById(stateGovtWalletId)).thenReturn(Optional.of(stateGovtWallet));
        when(walletRepository.findById(lgaWalletId)).thenReturn(Optional.of(lgaWallet));
        when(splitTemplateRepository.save(any(SplitTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateSplitTemplateRequestDto request = CreateSplitTemplateRequestDto.builder()
                .merchantWalletId(schoolWalletId)
                .templateName("UNILAG Tuition Split 2026")
                .feeCategory("TUITION_FEES")
                .rules(Arrays.asList(
                        CreateSplitTemplateRequestDto.TemplateRuleDto.builder().recipientWalletId(stateGovtWalletId).recipientName("State Govt").percentage(new BigDecimal("15")).build(),
                        CreateSplitTemplateRequestDto.TemplateRuleDto.builder().recipientWalletId(lgaWalletId).recipientName("LGA Govt").percentage(new BigDecimal("10")).build(),
                        CreateSplitTemplateRequestDto.TemplateRuleDto.builder().recipientWalletId(schoolWalletId).recipientName("School Main").percentage(new BigDecimal("75")).build()
                ))
                .build();

        SplitTemplateResponseDto response = splitPaymentService.createSplitTemplate(request);

        assertNotNull(response);
        assertEquals("UNILAG Tuition Split 2026", response.getTemplateName());
        assertEquals(3, response.getRules().size());
    }

    @Test
    @DisplayName("Should execute automated merchant checkout (school fee payment) using split template")
    void testAutomatedMerchantCheckout() {
        // Setup Template
        SplitTemplate template = new SplitTemplate();
        template.setMerchantWallet(schoolWallet);
        template.setTemplateName("UNILAG Tuition Split 2026");
        template.setFeeCategory("TUITION_FEES");

        com.orapay.split.model.SplitTemplateRule r1 = new com.orapay.split.model.SplitTemplateRule();
        r1.setRecipientWallet(stateGovtWallet);
        r1.setPercentage(new BigDecimal("15"));

        com.orapay.split.model.SplitTemplateRule r2 = new com.orapay.split.model.SplitTemplateRule();
        r2.setRecipientWallet(lgaWallet);
        r2.setPercentage(new BigDecimal("10"));

        com.orapay.split.model.SplitTemplateRule r3 = new com.orapay.split.model.SplitTemplateRule();
        r3.setRecipientWallet(schoolWallet);
        r3.setPercentage(new BigDecimal("75"));

        template.setRules(Arrays.asList(r1, r2, r3));

        when(splitTemplateRepository.findByMerchantWallet_WalletIdAndFeeCategoryAndActiveTrue(schoolWalletId, "TUITION_FEES"))
                .thenReturn(Optional.of(template));

        Map<UUID, Wallet> lockedWallets = new HashMap<>();
        lockedWallets.put(studentWalletId, studentWallet);
        lockedWallets.put(schoolWalletId, schoolWallet);
        lockedWallets.put(stateGovtWalletId, stateGovtWallet);
        lockedWallets.put(lgaWalletId, lgaWallet);

        when(walletLockManager.acquireLocksAsMap(anySet())).thenReturn(lockedWallets);
        when(splitOrderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        MerchantCheckoutRequestDto checkoutDto = MerchantCheckoutRequestDto.builder()
                .payerWalletId(studentWalletId)
                .merchantWalletId(schoolWalletId)
                .totalAmountInMinorUnits(10000000L) // 100,000 NGN
                .feeCategory("TUITION_FEES")
                .description("School fee payment 2026")
                .build();

        SplitPaymentResponseDto response = splitPaymentService.processMerchantCheckout(checkoutDto);

        assertNotNull(response);
        // Student debited 100,000 NGN (150,000 -> 50,000 NGN)
        assertEquals(5000000L, studentWallet.getAvailableBalanceInMinorUnits());
        // State Govt credited 15% (15,000 NGN)
        assertEquals(1500000L, stateGovtWallet.getAvailableBalanceInMinorUnits());
        // LGA credited 10% (10,000 NGN)
        assertEquals(1000000L, lgaWallet.getAvailableBalanceInMinorUnits());
        // School main credited 75% (75,000 NGN)
        assertEquals(7500000L, schoolWallet.getAvailableBalanceInMinorUnits());
    }
}
