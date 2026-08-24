package com.orapay.split.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SplitAllocationRuleDto {

    @NotNull(message = "Recipient wallet ID is mandatory")
    private UUID recipientWalletId;

    private BigDecimal percentage;
    private Long fixedAmountInMinorUnits;
}
