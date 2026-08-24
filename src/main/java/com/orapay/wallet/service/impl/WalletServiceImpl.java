package com.orapay.wallet.service.impl;

import com.orapay.common.event.EventPublisher;
import com.orapay.common.exception.BusinessRuleException;
import com.orapay.common.exception.InsufficientFundsException;
import com.orapay.user.model.User;
import com.orapay.user.repository.UserRepository;
import com.orapay.wallet.dto.request.CreateWalletRequestDto;
import com.orapay.wallet.dto.request.FundWalletRequestDto;
import com.orapay.wallet.dto.request.HoldFundsRequestDto;
import com.orapay.wallet.dto.response.WalletResponseDto;
import com.orapay.wallet.event.FundsLockedEvent;
import com.orapay.wallet.event.FundsUnlockedEvent;
import com.orapay.wallet.event.WalletCreatedEvent;
import com.orapay.wallet.event.WalletFundedEvent;
import com.orapay.wallet.mapper.WalletMapper;
import com.orapay.wallet.model.Wallet;
import com.orapay.wallet.repository.WalletRepository;
import com.orapay.wallet.service.WalletService;
import com.orapay.wallet.util.AccountNumberGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final WalletMapper walletMapper;
    private final EventPublisher domainEventPublisher;
    private final WalletLockManager walletLockManager;

    @Override
    @Transactional
    public WalletResponseDto createWallet(CreateWalletRequestDto requestDto) {
        User user = userRepository.findById(requestDto.getUserId())
                .orElseThrow(() -> new BusinessRuleException("User not found with ID: " + requestDto.getUserId()));

        if (walletRepository.existsByUser_UserUniqueIdAndCurrencyCode(user.getUserUniqueId(), requestDto.getCurrencyCode())) {
            throw new BusinessRuleException(
                    String.format("Wallet already exists for user in currency [%s]", requestDto.getCurrencyCode())
            );
        }

        String accountNumber = AccountNumberGenerator.derive10DigitAccountNumberFromPhone(user.getPhoneNumber());

        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setAccountNumber(accountNumber);
        wallet.setCurrencyCode(requestDto.getCurrencyCode());
        wallet.setAvailableBalanceInMinorUnits(0L);
        wallet.setLockedBalanceInMinorUnits(0L);
        wallet.setActive(true);

        Wallet savedWallet = walletRepository.save(wallet);

        domainEventPublisher.publishEvent(new WalletCreatedEvent(
                this,
                savedWallet.getWalletId(),
                user.getUserUniqueId(),
                savedWallet.getAccountNumber(),
                savedWallet.getCurrencyCode()
        ));

        return walletMapper.mapToWalletResponseDto(savedWallet);
    }

    @Override
    @Transactional(readOnly = true)
    public WalletResponseDto getWalletByIdentifier(String identifier) {
        try {
            UUID walletUuid = UUID.fromString(identifier);
            Wallet wallet = walletRepository.findById(walletUuid)
                    .orElseGet(() -> walletRepository.findByUser_UserUniqueId(walletUuid).orElse(null));
            if (wallet != null) {
                return walletMapper.mapToWalletResponseDto(wallet);
            }
        } catch (IllegalArgumentException ignored) {
        }

        Wallet resolvedWallet = walletRepository.resolveRecipientByIdentifier(identifier)
                .orElseThrow(() -> new BusinessRuleException("Wallet not found for identifier: " + identifier));

        return walletMapper.mapToWalletResponseDto(resolvedWallet);
    }

    @Override
    @Transactional
    public void provisionWalletForUser(UUID userId, String phoneNumber, String currencyCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessRuleException("User not found: " + userId));

        if (walletRepository.existsByUser_UserUniqueIdAndCurrencyCode(userId, currencyCode)) {
            return;
        }

        String accountNumber = AccountNumberGenerator.derive10DigitAccountNumberFromPhone(phoneNumber);

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

    @Override
    @Transactional
    public WalletResponseDto fundWallet(UUID walletId, FundWalletRequestDto requestDto) {
        Wallet wallet = walletLockManager.acquireLock(walletId);

        if (!wallet.isActive()) {
            throw new BusinessRuleException("Cannot fund inactive wallet: " + walletId);
        }

        if (requestDto.getCurrencyCode() != null && !wallet.getCurrencyCode().equalsIgnoreCase(requestDto.getCurrencyCode())) {
            throw new BusinessRuleException(String.format("Currency mismatch. Wallet currency is %s but funding requested %s",
                    wallet.getCurrencyCode(), requestDto.getCurrencyCode()));
        }

        long amount = requestDto.getAmountInMinorUnits();
        long newBalance = wallet.getAvailableBalanceInMinorUnits() + amount;
        wallet.setAvailableBalanceInMinorUnits(newBalance);

        Wallet savedWallet = walletRepository.save(wallet);

        domainEventPublisher.publishEvent(new WalletFundedEvent(
                this,
                savedWallet.getWalletId(),
                amount,
                savedWallet.getCurrencyCode(),
                savedWallet.getAvailableBalanceInMinorUnits(),
                requestDto.getReference()
        ));

        return walletMapper.mapToWalletResponseDto(savedWallet);
    }

    @Override
    @Transactional
    public WalletResponseDto holdFunds(UUID walletId, HoldFundsRequestDto requestDto) {
        Wallet wallet = walletLockManager.acquireLock(walletId);

        if (!wallet.isActive()) {
            throw new BusinessRuleException("Cannot hold funds on inactive wallet: " + walletId);
        }

        if (requestDto.getCurrencyCode() != null && !wallet.getCurrencyCode().equalsIgnoreCase(requestDto.getCurrencyCode())) {
            throw new BusinessRuleException(String.format("Currency mismatch. Wallet currency is %s but hold requested %s",
                    wallet.getCurrencyCode(), requestDto.getCurrencyCode()));
        }

        long amount = requestDto.getAmountInMinorUnits();
        if (wallet.getAvailableBalanceInMinorUnits() < amount) {
            throw new InsufficientFundsException(String.format("Insufficient available balance (%d) to place hold of %d minor units",
                    wallet.getAvailableBalanceInMinorUnits(), amount));
        }

        wallet.setAvailableBalanceInMinorUnits(wallet.getAvailableBalanceInMinorUnits() - amount);
        wallet.setLockedBalanceInMinorUnits(wallet.getLockedBalanceInMinorUnits() + amount);

        Wallet savedWallet = walletRepository.save(wallet);

        domainEventPublisher.publishEvent(new FundsLockedEvent(
                this,
                savedWallet.getWalletId(),
                amount,
                savedWallet.getCurrencyCode(),
                savedWallet.getAvailableBalanceInMinorUnits(),
                savedWallet.getLockedBalanceInMinorUnits(),
                requestDto.getReference()
        ));

        return walletMapper.mapToWalletResponseDto(savedWallet);
    }

    @Override
    @Transactional
    public WalletResponseDto releaseHold(UUID walletId, HoldFundsRequestDto requestDto) {
        Wallet wallet = walletLockManager.acquireLock(walletId);

        if (!wallet.isActive()) {
            throw new BusinessRuleException("Cannot release hold on inactive wallet: " + walletId);
        }

        if (requestDto.getCurrencyCode() != null && !wallet.getCurrencyCode().equalsIgnoreCase(requestDto.getCurrencyCode())) {
            throw new BusinessRuleException(String.format("Currency mismatch. Wallet currency is %s but release requested %s",
                    wallet.getCurrencyCode(), requestDto.getCurrencyCode()));
        }

        long amount = requestDto.getAmountInMinorUnits();
        if (wallet.getLockedBalanceInMinorUnits() < amount) {
            throw new BusinessRuleException(String.format("Insufficient locked balance (%d) to release hold of %d minor units",
                    wallet.getLockedBalanceInMinorUnits(), amount));
        }

        wallet.setLockedBalanceInMinorUnits(wallet.getLockedBalanceInMinorUnits() - amount);
        wallet.setAvailableBalanceInMinorUnits(wallet.getAvailableBalanceInMinorUnits() + amount);

        Wallet savedWallet = walletRepository.save(wallet);

        domainEventPublisher.publishEvent(new FundsUnlockedEvent(
                this,
                savedWallet.getWalletId(),
                amount,
                savedWallet.getCurrencyCode(),
                savedWallet.getAvailableBalanceInMinorUnits(),
                savedWallet.getLockedBalanceInMinorUnits(),
                requestDto.getReference()
        ));

        return walletMapper.mapToWalletResponseDto(savedWallet);
    }
}
