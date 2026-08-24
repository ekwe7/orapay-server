package com.orapay.transfer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferResponseDto {

    private UUID transactionId;
    private UUID senderWalletId;
    private UUID recipientWalletId;
    private long amountInMinorUnits;
    private String currencyCode;
    private String status;
    private String reference;
    private String narration;
    private Instant createdAt;
}
