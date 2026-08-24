package com.orapay.wallet.service;

import com.orapay.common.event.EventPublisher;
import com.orapay.common.exception.InsufficientFundsException;
import com.orapay.wallet.dto.request.FundWalletRequestDto;
import com.orapay.wallet.dto.request.HoldFundsRequestDto;
import com.orapay.wallet.dto.response.WalletResponseDto;
import com.orapay.wallet.event.FundsLockedEvent;
import com.orapay.wallet.event.FundsUnlockedEvent;
import com.orapay.wallet.event.WalletFundedEvent;
import com.orapay.wallet.mapper.WalletMapper;
import com.orapay.wallet.model.Wallet;
import com.orapay.wallet.repository.WalletRepository;
import com.orapay.wallet.service.impl.WalletLockManager;
import com.orapay.wallet.service.impl.WalletServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletMapper walletMapper;

    @Mock
    private EventPublisher domainEventPublisher;

    @Mock
    private WalletLockManager walletLockManager;

    @InjectMocks
    private WalletServiceImpl walletService;

    private Wallet wallet;
    private UUID walletId;

    @BeforeEach
    void setUp() {
        walletId = UUID.randomUUID();
        wallet = new Wallet();
        wallet.setWalletId(walletId);
        wallet.setCurrencyCode("NGN");
        wallet.setAvailableBalanceInMinorUnits(10000L); // 100 NGN
        wallet.setLockedBalanceInMinorUnits(0L);
        wallet.setActive(true);
    }

    @Test
    @DisplayName("Should successfully fund wallet and publish WalletFundedEvent")
    void fundWallet_Success() {
        given(walletLockManager.acquireLock(walletId)).willReturn(wallet);
        given(walletRepository.save(any(Wallet.class))).willAnswer(invocation -> invocation.getArgument(0));

        WalletResponseDto dummyDto = WalletResponseDto.builder()
                .walletId(walletId)
                .availableBalanceInMinorUnits(15000L)
                .build();
        given(walletMapper.mapToWalletResponseDto(any(Wallet.class))).willReturn(dummyDto);

        FundWalletRequestDto request = FundWalletRequestDto.builder()
                .amountInMinorUnits(5000L)
                .currencyCode("NGN")
                .reference("FUND-123")
                .build();

        WalletResponseDto response = walletService.fundWallet(walletId, request);

        assertThat(response.getAvailableBalanceInMinorUnits()).isEqualTo(15000L);
        assertThat(wallet.getAvailableBalanceInMinorUnits()).isEqualTo(15000L);

        ArgumentCaptor<WalletFundedEvent> eventCaptor = ArgumentCaptor.forClass(WalletFundedEvent.class);
        verify(domainEventPublisher).publishEvent(eventCaptor.capture());
        WalletFundedEvent event = eventCaptor.getValue();
        assertThat(event.getWalletId()).isEqualTo(walletId);
        assertThat(event.getAmountInMinorUnits()).isEqualTo(5000L);
        assertThat(event.getNewAvailableBalanceInMinorUnits()).isEqualTo(15000L);
        assertThat(event.getReference()).isEqualTo("FUND-123");
    }

    @Test
    @DisplayName("Should successfully hold/reserve funds and publish FundsLockedEvent")
    void holdFunds_Success() {
        given(walletLockManager.acquireLock(walletId)).willReturn(wallet);
        given(walletRepository.save(any(Wallet.class))).willAnswer(invocation -> invocation.getArgument(0));

        HoldFundsRequestDto request = HoldFundsRequestDto.builder()
                .amountInMinorUnits(4000L)
                .currencyCode("NGN")
                .reference("HOLD-456")
                .build();

        walletService.holdFunds(walletId, request);

        assertThat(wallet.getAvailableBalanceInMinorUnits()).isEqualTo(6000L);
        assertThat(wallet.getLockedBalanceInMinorUnits()).isEqualTo(4000L);

        ArgumentCaptor<FundsLockedEvent> eventCaptor = ArgumentCaptor.forClass(FundsLockedEvent.class);
        verify(domainEventPublisher).publishEvent(eventCaptor.capture());
        FundsLockedEvent event = eventCaptor.getValue();
        assertThat(event.getWalletId()).isEqualTo(walletId);
        assertThat(event.getAmountInMinorUnits()).isEqualTo(4000L);
        assertThat(event.getRemainingAvailableBalanceInMinorUnits()).isEqualTo(6000L);
        assertThat(event.getTotalLockedBalanceInMinorUnits()).isEqualTo(4000L);
    }

    @Test
    @DisplayName("Should throw InsufficientFundsException when holding more than available balance")
    void holdFunds_InsufficientBalance_ThrowsException() {
        given(walletLockManager.acquireLock(walletId)).willReturn(wallet);

        HoldFundsRequestDto request = HoldFundsRequestDto.builder()
                .amountInMinorUnits(20000L)
                .currencyCode("NGN")
                .build();

        assertThatThrownBy(() -> walletService.holdFunds(walletId, request))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Insufficient available balance");
    }

    @Test
    @DisplayName("Should release hold funds and publish FundsUnlockedEvent")
    void releaseHold_Success() {
        wallet.setAvailableBalanceInMinorUnits(6000L);
        wallet.setLockedBalanceInMinorUnits(4000L);

        given(walletLockManager.acquireLock(walletId)).willReturn(wallet);
        given(walletRepository.save(any(Wallet.class))).willAnswer(invocation -> invocation.getArgument(0));

        HoldFundsRequestDto request = HoldFundsRequestDto.builder()
                .amountInMinorUnits(4000L)
                .currencyCode("NGN")
                .reference("RELEASE-789")
                .build();

        walletService.releaseHold(walletId, request);

        assertThat(wallet.getAvailableBalanceInMinorUnits()).isEqualTo(10000L);
        assertThat(wallet.getLockedBalanceInMinorUnits()).isEqualTo(0L);

        ArgumentCaptor<FundsUnlockedEvent> eventCaptor = ArgumentCaptor.forClass(FundsUnlockedEvent.class);
        verify(domainEventPublisher).publishEvent(eventCaptor.capture());
        FundsUnlockedEvent event = eventCaptor.getValue();
        assertThat(event.getWalletId()).isEqualTo(walletId);
        assertThat(event.getAmountInMinorUnits()).isEqualTo(4000L);
        assertThat(event.getUpdatedAvailableBalanceInMinorUnits()).isEqualTo(10000L);
        assertThat(event.getUpdatedLockedBalanceInMinorUnits()).isEqualTo(0L);
    }
}
