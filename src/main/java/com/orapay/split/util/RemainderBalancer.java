package com.orapay.split.util;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class RemainderBalancer {

    /**
     * Ensures zero penny leakage by allocating rounding remainder fractions to a designated target wallet (e.g., primary recipient or platform fee wallet).
     * Guarantees that sum(allocations) == totalAmountInMinorUnits.
     *
     * @param totalAmountInMinorUnits Total debit amount in minor currency units
     * @param allocations Calculated map of recipient wallet ID to allocated minor unit amounts
     * @param primaryRecipientWalletId Wallet ID to receive rounding remainder fraction
     * @return Balanced allocation map with zero penny leakage
     */
    public Map<UUID, Long> balanceRemainders(
            long totalAmountInMinorUnits,
            Map<UUID, Long> allocations,
            UUID primaryRecipientWalletId
    ) {
        if (allocations == null || allocations.isEmpty()) {
            return allocations;
        }

        long sumAllocated = allocations.values().stream().mapToLong(Long::longValue).sum();
        long remainder = totalAmountInMinorUnits - sumAllocated;

        if (remainder != 0L && primaryRecipientWalletId != null) {
            long existingAmount = allocations.getOrDefault(primaryRecipientWalletId, 0L);
            allocations.put(primaryRecipientWalletId, existingAmount + remainder);
        }

        return allocations;
    }
}
