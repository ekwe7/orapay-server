package com.orapay.wallet.service;
    
    import com.orapay.wallet.dto.CreateWalletRequestDto;
    import com.orapay.wallet.dto.WalletResponseDto;
    
    import java.util.UUID;
    
    public interface WalletService {
    
        WalletResponseDto createWallet(CreateWalletRequestDto
  requestDto);
    
        WalletResponseDto getWalletByIdentifier(String
  identifier);
    
        void provisionWalletForUser(UUID userId, String
  phoneNumber, String currencyCode);
    }
