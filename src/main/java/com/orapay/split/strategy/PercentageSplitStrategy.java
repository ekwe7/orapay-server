package com.orapay.split.strategy;

import com.orapay.common.exception.BusinessRuleException;
import com.orapay.split.dto.request.SplitAllocationRuleDto;
import com.orapay.split.util.RemainderBalancer;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component("percentageSplitStrategy")
public class PercentageSplitStrategy implements SplitCalculationStrategy {

    private final RemainderBalancer remainderBalancer;

    public PercentageSplitStrategy(RemainderBalancer remainderBalancer) {
        this.remainderBalancer = remainderBalancer;
    }

    public PercentageSplitStrategy() {
        this.remainderBalancer = new RemainderBalancer();
    }

    @Override
    public Map<UUID, Long> calculateSplits(long totalAmountInMinorUnits, List<SplitAllocationRuleDto> rules) {
        if (rules == null || rules.isEmpty()) {
            throw new BusinessRuleException("Split allocation rules cannot be empty");
        }

        Map<UUID, Long> allocations = new LinkedHashMap<>();
        BigDecimal hundred = new BigDecimal("100");
        BigDecimal totalPercentageSum = BigDecimal.ZERO;

        UUID primaryRecipientId = rules.get(rules.size() - 1).getRecipientWalletId();

        for (SplitAllocationRuleDto rule : rules) {
            BigDecimal pct = rule.getPercentage();
            if (pct == null || pct.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRuleException("Percentage allocation rule must have a positive percentage");
            }

            // Normalization: if percentage is expressed as fraction (<= 1.0), convert to percentage scale (e.g. 0.5 -> 50)
            if (pct.compareTo(BigDecimal.ONE) <= 0 && rules.stream().map(SplitAllocationRuleDto::getPercentage).reduce(BigDecimal.ZERO, BigDecimal::add).compareTo(BigDecimal.ONE) <= 0) {
                pct = pct.multiply(hundred);
            }

            totalPercentageSum = totalPercentageSum.add(pct);

            BigDecimal amountBD = BigDecimal.valueOf(totalAmountInMinorUnits);
            long calculatedAmount = amountBD.multiply(pct)
                    .divide(hundred, 0, RoundingMode.HALF_DOWN)
                    .longValueExact();

            allocations.put(rule.getRecipientWalletId(), allocations.getOrDefault(rule.getRecipientWalletId(), 0L) + calculatedAmount);
        }

        if (totalPercentageSum.compareTo(hundred) != 0 && totalPercentageSum.compareTo(BigDecimal.ONE) != 0) {
            throw new BusinessRuleException("Total percentage allocations must sum up to 100%");
        }

        // Remainder balancer ensures zero-penny leakage by allocating fractions to the primary recipient
        return remainderBalancer.balanceRemainders(totalAmountInMinorUnits, allocations, primaryRecipientId);
    }
}
