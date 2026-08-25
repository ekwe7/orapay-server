package com.orapay.split.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RemainderBalancerTest {

    private RemainderBalancer remainderBalancer;
    private UUID primaryWalletId;
    private UUID secondaryWalletId;

    @BeforeEach
    void setUp() {
        remainderBalancer = new RemainderBalancer();
        primaryWalletId = UUID.randomUUID();
        secondaryWalletId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should allocate rounding fraction penny to primary recipient wallet with zero penny leakage")
    void testZeroPennyLeakageRemainderAllocation() {
        long totalAmount = 10000L; // 10,000 cents
        Map<UUID, Long> allocations = new LinkedHashMap<>();
        allocations.put(secondaryWalletId, 3333L);
        allocations.put(primaryWalletId, 6666L); // Sum = 9999, remainder = 1

        Map<UUID, Long> balanced = remainderBalancer.balanceRemainders(totalAmount, allocations, primaryWalletId);

        assertEquals(6667L, balanced.get(primaryWalletId));
        assertEquals(3333L, balanced.get(secondaryWalletId));
        assertEquals(10000L, balanced.values().stream().mapToLong(Long::longValue).sum());
    }
}
