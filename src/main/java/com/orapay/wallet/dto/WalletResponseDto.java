package com.orapay.wallet.dto;
    
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
    public class WalletResponseDto {
    
        private UUID walletId;
        private UUID userId;
        private String userFullName;
        private String accountNumber;
        private String currencyCode;
        private long availableBalanceInMinorUnits;
        private long lockedBalanceInMinorUnits;
        private boolean isActive;
        private Instant createdAt;
    }
