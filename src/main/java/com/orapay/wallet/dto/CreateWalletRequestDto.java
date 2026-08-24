package com.orapay.wallet.dto;
    
    import jakarta.validation.constraints.NotNull;
    import lombok.AllArgsConstructor;
    import lombok.Data;
    import lombok.NoArgsConstructor;
    
    import java.util.UUID;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public class CreateWalletRequestDto {
    
        @NotNull(message = "User unique ID is mandatory")
        private UUID userId;
    
        private String currencyCode = "NGN";
    }
