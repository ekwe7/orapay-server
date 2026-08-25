package com.orapay.split.strategy;

import com.orapay.common.exception.BusinessRuleException;
import com.orapay.split.dto.request.SplitAllocationRuleDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FixedFeeSplitStrategyTest {

    private FixedFeeSplitStrategy strategy;
    private UUID walletA;
    private UUID walletB;

    @BeforeEach
    void setUp() {
        strategy = new FixedFeeSplitStrategy();
        walletA = UUID.randomUUID();
        walletB = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should split amount based on fixed fee rules matching total sum")
    void testValidFixedFeeSplit() {
        long totalAmount = 5000L;
        List<SplitAllocationRuleDto> rules = Arrays.asList(
                SplitAllocationRuleDto.builder().recipientWalletId(walletA).fixedAmountInMinorUnits(3000L).build(),
                SplitAllocationRuleDto.builder().recipientWalletId(walletB).fixedAmountInMinorUnits(2000L).build()
        );

        Map<UUID, Long> splits = strategy.calculateSplits(totalAmount, rules);

        assertEquals(3000L, splits.get(walletA));
        assertEquals(2000L, splits.get(walletB));
        assertEquals(5000L, splits.values().stream().mapToLong(Long::longValue).sum());
    }

    @Test
    @DisplayName("Should throw BusinessRuleException if fixed amounts do not equal total amount")
    void testMismatchedFixedSumThrowsException() {
        long totalAmount = 5000L;
        List<SplitAllocationRuleDto> rules = Arrays.asList(
                SplitAllocationRuleDto.builder().recipientWalletId(walletA).fixedAmountInMinorUnits(3000L).build(),
                SplitAllocationRuleDto.builder().recipientWalletId(walletB).fixedAmountInMinorUnits(1000L).build()
        );

        assertThrows(BusinessRuleException.class, () -> strategy.calculateSplits(totalAmount, rules));
    }
}
