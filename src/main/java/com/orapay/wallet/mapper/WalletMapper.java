package com.orapay.wallet.mapper;
    
    import com.orapay.wallet.dto.WalletResponseDto;
    import com.orapay.wallet.model.Wallet;
    import org.springframework.stereotype.Component;
    
    @Component
    public class WalletMapper {
    
        public WalletResponseDto mapToWalletResponseDto(Wallet
  wallet) {
            if (wallet == null) return null;
    
            String fullName = wallet.getUser() != null
                ? wallet.getUser().getUserFirstName() + " " +
  wallet.getUser().getUserLastName()
                : null;
    
            return WalletResponseDto.builder()
                    .walletId(wallet.getWalletId())
                    .userId(wallet.getUser() != null ? wallet.
  getUser().getUserUniqueId() : null)
                    .userFullName(fullName)
                    .accountNumber(wallet.getAccountNumber())
                    .currencyCode(wallet.getCurrencyCode())
                    .availableBalanceInMinorUnits(wallet.
  getAvailableBalanceInMinorUnits())
                    .lockedBalanceInMinorUnits(wallet.
  getLockedBalanceInMinorUnits())
                    .isActive(wallet.isActive())
                    .createdAt(wallet.getCreatedAt())
                    .build();
        }
    }
