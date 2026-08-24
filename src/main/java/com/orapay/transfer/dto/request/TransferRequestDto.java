package com.orapay.transfer.dto.request;

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
public class TransferRequestDto {

    @NotNull(message = "Sender wallet ID is mandatory")
    private UUID senderWalletId;

    @NotBlank(message = "Recipient identifier (UUID, Account Number, or Phone) is mandatory")
    private String recipientIdentifier;

    @NotNull(message = "Amount in minor units is mandatory")
    @Positive(message = "Amount must be strictly positive")
    private Long amountInMinorUnits;

    @Builder.Default
    private String currencyCode = "NGN";

    private String narration;
}
