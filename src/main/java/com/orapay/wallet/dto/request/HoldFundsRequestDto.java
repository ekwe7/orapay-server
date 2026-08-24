package com.orapay.wallet.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HoldFundsRequestDto {

    @NotNull(message = "Amount in minor units is mandatory")
    @Positive(message = "Amount must be strictly positive")
    private Long amountInMinorUnits;

    @Builder.Default
    private String currencyCode = "NGN";

    private String reference;
}
