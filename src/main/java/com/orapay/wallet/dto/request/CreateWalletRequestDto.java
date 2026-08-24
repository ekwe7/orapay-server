package com.orapay.wallet.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateWalletRequestDto {

    @NotNull(message = "User unique ID is mandatory")
    private UUID userId;

    @Builder.Default
    private String currencyCode = "NGN";
}
