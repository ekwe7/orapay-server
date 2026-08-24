package com.orapay.split.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SplitPaymentRequestDto {

    @NotNull(message = "Payer wallet ID is mandatory")
    private UUID payerWalletId;

    @NotNull(message = "Total amount in minor units is mandatory")
    @Positive(message = "Total amount must be positive")
    private Long totalAmountInMinorUnits;

    @Builder.Default
    private String currencyCode = "NGN";

    @NotEmpty(message = "Allocations list cannot be empty")
    private List<SplitAllocationRuleDto> allocations;

    private String description;
}
