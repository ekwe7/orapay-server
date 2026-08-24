    package com.orapay.wallet.service;
    
    import com.orapay.common.event.EventPublisher;
    import com.orapay.common.exception.BusinessRuleException;
    import com.orapay.user.model.User;
    import com.orapay.user.repository.UserRepository;
    import com.orapay.wallet.dto.CreateWalletRequestDto;
    import com.orapay.wallet.dto.WalletResponseDto;
    import com.orapay.wallet.event.WalletCreatedEvent;
    import com.orapay.wallet.mapper.WalletMapper;
    import com.orapay.wallet.model.Wallet;
    import com.orapay.wallet.repository.WalletRepository;
    import com.orapay.wallet.util.AccountNumberGenerator;
    import lombok.RequiredArgsConstructor;
    import org.springframework.stereotype.Service;
    import org.springframework.transaction.annotation.
  Transactional;
    
    import java.util.UUID;
    
    @Service
    @RequiredArgsConstructor
    public class WalletServiceImpl implements WalletService {
    
        private final WalletRepository walletRepository;
        private final UserRepository userRepository;
        private final WalletMapper walletMapper;
        private final EventPublisher domainEventPublisher;
    
        @Override
        @Transactional
        public WalletResponseDto
  createWallet(CreateWalletRequestDto requestDto) {
            User user = userRepository.findById(requestDto.
  getUserId())
                    .orElseThrow(() -> new
  BusinessRuleException("User not found with ID: " + requestDto.
  getUserId()));
    
            if (walletRepository.
  existsByUser_UserUniqueIdAndCurrencyCode(user.getUserUniqueId(),
  requestDto.getCurrencyCode())) {
                throw new BusinessRuleException(
                    String.format("Wallet already exists for user in currency [%s]", requestDto.getCurrencyCode())
                );
            }
    
            String accountNumber = AccountNumberGenerator.
  derive10DigitAccountNumberFromPhone(user.getPhoneNumber());
    
            Wallet wallet = new Wallet();
            wallet.setUser(user);
            wallet.setAccountNumber(accountNumber);
            wallet.setCurrencyCode(requestDto.getCurrencyCode());
            wallet.setAvailableBalanceInMinorUnits(0L);
            wallet.setLockedBalanceInMinorUnits(0L);
            wallet.setActive(true);
    
            Wallet savedWallet = walletRepository.save(wallet);
    
            domainEventPublisher.publishEvent(new
  WalletCreatedEvent(
                    this,
                    savedWallet.getWalletId(),
                    user.getUserUniqueId(),
                    savedWallet.getAccountNumber(),
                    savedWallet.getCurrencyCode()
            ));
    
            return walletMapper.
  mapToWalletResponseDto(savedWallet);
        }
    
        @Override
        @Transactional(readOnly = true)
        public WalletResponseDto getWalletByIdentifier(String
  identifier) {
            // Try direct UUID lookup first
            try {
                UUID walletUuid = UUID.fromString(identifier);
                Wallet wallet = walletRepository.
  findById(walletUuid)
                        .orElseGet(() -> walletRepository.
  findByUser_UserUniqueId(walletUuid).orElse(null));
                if (wallet != null) {
                    return walletMapper.
  mapToWalletResponseDto(wallet);
                }
            } catch (IllegalArgumentException ignored) {
                // Not a valid UUID string, proceed to account number / phone lookup
            }
    
            Wallet resolvedWallet = walletRepository.
  resolveRecipientByIdentifier(identifier)
                    .orElseThrow(() -> new
  BusinessRuleException("Wallet not found for identifier: " +
  identifier));
    
            return walletMapper.
  mapToWalletResponseDto(resolvedWallet);
        }
    
        @Override
        @Transactional
        public void provisionWalletForUser(UUID userId, String
  phoneNumber, String currencyCode) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new
  BusinessRuleException("User not found: " + userId));
    
            if (walletRepository.
  existsByUser_UserUniqueIdAndCurrencyCode(userId, currencyCode)) {
                return;
            }
    
            String accountNumber = AccountNumberGenerator.
  derive10DigitAccountNumberFromPhone(phoneNumber);
    
            Wallet wallet = new Wallet();
            wallet.setUser(user);
            wallet.setAccountNumber(accountNumber);
            wallet.setCurrencyCode(currencyCode);
            wallet.setAvailableBalanceInMinorUnits(0L);
            wallet.setLockedBalanceInMinorUnits(0L);
    
            Wallet savedWallet = walletRepository.save(wallet);
    
            domainEventPublisher.publishEvent(new WalletCreatedEvent(
                    this,
                    savedWallet.getWalletId(),
                    userId,
                    savedWallet.getAccountNumber(),
                    savedWallet.getCurrencyCode()
            ));
        }
    }