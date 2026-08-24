package com.orapay.split.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SplitPaymentResponseDto {

    private UUID splitOrderId;
    private UUID payerWalletId;
    private long totalAmountInMinorUnits;
    private String currencyCode;
    private String status;
    private List<SplitAllocationResponseDto> allocations;
    private Instant createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SplitAllocationResponseDto {
        private UUID allocationId;
        private UUID recipientWalletId;
        private long allocatedAmountInMinorUnits;
    }
}
