package com.orapay.split.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantCheckoutRequestDto {

    @NotNull(message = "Payer wallet ID is mandatory")
    private UUID payerWalletId;

    @NotNull(message = "Merchant wallet ID is mandatory")
    private UUID merchantWalletId;

    @NotNull(message = "Total amount in minor units is mandatory")
    @Positive(message = "Total amount must be positive")
    private Long totalAmountInMinorUnits;

    @NotBlank(message = "Fee category is mandatory")
    private String feeCategory;

    @Builder.Default
    private String currencyCode = "NGN";

    private String description;
}
