package com.orapay.split.strategy;

import com.orapay.common.exception.BusinessRuleException;
import com.orapay.split.dto.request.SplitAllocationRuleDto;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component("fixedFeeSplitStrategy")
public class FixedFeeSplitStrategy implements SplitCalculationStrategy {

    @Override
    public Map<UUID, Long> calculateSplits(long totalAmountInMinorUnits, List<SplitAllocationRuleDto> rules) {
        if (rules == null || rules.isEmpty()) {
            throw new BusinessRuleException("Split allocation rules cannot be empty");
        }

        Map<UUID, Long> allocations = new LinkedHashMap<>();
        long allocatedSum = 0L;

        for (SplitAllocationRuleDto rule : rules) {
            Long fixedAmount = rule.getFixedAmountInMinorUnits();
            if (fixedAmount == null || fixedAmount <= 0) {
                throw new BusinessRuleException("Fixed fee allocation rule must specify a positive fixed amount");
            }
            allocatedSum += fixedAmount;
            allocations.put(rule.getRecipientWalletId(), allocations.getOrDefault(rule.getRecipientWalletId(), 0L) + fixedAmount);
        }

        if (allocatedSum != totalAmountInMinorUnits) {
            throw new BusinessRuleException(String.format("Sum of fixed split allocations (%d) must equal total payment amount (%d)", allocatedSum, totalAmountInMinorUnits));
        }

        return allocations;
    }
}
