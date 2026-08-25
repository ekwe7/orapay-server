package com.orapay.split.strategy;

import com.orapay.split.dto.request.SplitAllocationRuleDto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface SplitCalculationStrategy {

    /**
     * Calculates the minor unit amounts to allocate to each recipient wallet.
     * Ensures total sum of allocated amounts equals totalAmountInMinorUnits without float loss.
     *
     * @param totalAmountInMinorUnits Total amount to split in minor units (e.g., cents/kobo)
     * @param rules List of allocation rules (percentage or fixed amounts)
     * @return Map of recipient wallet ID to calculated minor unit allocation amount
     */
    Map<UUID, Long> calculateSplits(long totalAmountInMinorUnits, List<SplitAllocationRuleDto> rules);
}