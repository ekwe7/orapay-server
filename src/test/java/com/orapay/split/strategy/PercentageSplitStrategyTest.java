package com.orapay.split.strategy;

import com.orapay.common.exception.BusinessRuleException;
import com.orapay.split.dto.request.SplitAllocationRuleDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PercentageSplitStrategyTest {

    private PercentageSplitStrategy strategy;
    private UUID walletA;
    private UUID walletB;
    private UUID walletC;

    @BeforeEach
    void setUp() {
        strategy = new PercentageSplitStrategy();
        walletA = UUID.randomUUID();
        walletB = UUID.randomUUID();
        walletC = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should split amount evenly based on 50% - 50% percentages")
    void testEvenPercentageSplit() {
        long totalAmount = 10000L; // 100.00 NGN
        List<SplitAllocationRuleDto> rules = Arrays.asList(
                SplitAllocationRuleDto.builder().recipientWalletId(walletA).percentage(new BigDecimal("50")).build(),
                SplitAllocationRuleDto.builder().recipientWalletId(walletB).percentage(new BigDecimal("50")).build()
        );

        Map<UUID, Long> splits = strategy.calculateSplits(totalAmount, rules);

        assertEquals(5000L, splits.get(walletA));
        assertEquals(5000L, splits.get(walletB));
        assertEquals(10000L, splits.values().stream().mapToLong(Long::longValue).sum());
    }

    @Test
    @DisplayName("Should allocate integer rounding remainder to last recipient without float loss")
    void testRoundingRemainderAllocation() {
        long totalAmount = 10000L; // 100.00 divided 33.33%, 33.33%, 33.34%
        List<SplitAllocationRuleDto> rules = Arrays.asList(
                SplitAllocationRuleDto.builder().recipientWalletId(walletA).percentage(new BigDecimal("33.33")).build(),
                SplitAllocationRuleDto.builder().recipientWalletId(walletB).percentage(new BigDecimal("33.33")).build(),
                SplitAllocationRuleDto.builder().recipientWalletId(walletC).percentage(new BigDecimal("33.34")).build()
        );

        Map<UUID, Long> splits = strategy.calculateSplits(totalAmount, rules);

        assertEquals(10000L, splits.values().stream().mapToLong(Long::longValue).sum());
    }

    @Test
    @DisplayName("Should throw BusinessRuleException if percentages do not sum to 100%")
    void testInvalidPercentageSum() {
        long totalAmount = 10000L;
        List<SplitAllocationRuleDto> rules = Arrays.asList(
                SplitAllocationRuleDto.builder().recipientWalletId(walletA).percentage(new BigDecimal("40")).build(),
                SplitAllocationRuleDto.builder().recipientWalletId(walletB).percentage(new BigDecimal("40")).build()
        );

        assertThrows(BusinessRuleException.class, () -> strategy.calculateSplits(totalAmount, rules));
    }
}
