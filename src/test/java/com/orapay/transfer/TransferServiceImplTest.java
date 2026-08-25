package com.orapay.transfer;

import com.orapay.common.event.EventPublisher;
import com.orapay.common.exception.BusinessRuleException;
import com.orapay.common.exception.InsufficientFundsException;
import com.orapay.transfer.dto.request.TransferRequestDto;
import com.orapay.transfer.dto.response.TransferResponseDto;
import com.orapay.transfer.event.TransferCompletedEvent;
import com.orapay.transfer.event.TransferInitiatedEvent;
import com.orapay.transfer.mapper.TransferMapper;
import com.orapay.transfer.model.Transaction;
import com.orapay.transfer.model.TransferStatus;
import com.orapay.transfer.repository.TransactionRepository;
import com.orapay.transfer.service.impl.TransferServiceImpl;
import com.orapay.wallet.model.Wallet;
import com.orapay.wallet.repository.WalletRepository;
import com.orapay.wallet.service.impl.WalletLockManager;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TransferServiceImplTest {

    private WalletRepository walletRepository;
    private TransactionRepository transactionRepository;
    private WalletLockManager walletLockManager;
    private TransferMapper transferMapper;
    private EventPublisher eventPublisher;
    private MeterRegistry meterRegistry;
    private TransferServiceImpl transferService;

    private UUID senderWalletId;
    private UUID recipientWalletId;
    private Wallet senderWallet;
    private Wallet recipientWallet;

    @BeforeEach
    void setUp() {
        walletRepository = mock(WalletRepository.class);
        transactionRepository = mock(TransactionRepository.class);
        walletLockManager = mock(WalletLockManager.class);
        transferMapper = new TransferMapper();
        eventPublisher = mock(EventPublisher.class);
        meterRegistry = new SimpleMeterRegistry();

        transferService = new TransferServiceImpl(
                walletRepository,
                transactionRepository,
                walletLockManager,
                transferMapper,
                eventPublisher,
                meterRegistry
        );

        senderWalletId = UUID.randomUUID();
        recipientWalletId = UUID.randomUUID();

        senderWallet = new Wallet();
        senderWallet.setWalletId(senderWalletId);
        senderWallet.setAvailableBalanceInMinorUnits(10000L); // 100.00 NGN
        senderWallet.setCurrencyCode("NGN");
        senderWallet.setActive(true);

        recipientWallet = new Wallet();
        recipientWallet.setWalletId(recipientWalletId);
        recipientWallet.setAvailableBalanceInMinorUnits(5000L); // 50.00 NGN
        recipientWallet.setCurrencyCode("NGN");
        recipientWallet.setActive(true);
    }

    @Test
    @DisplayName("Should execute P2P transfer successfully when balance is sufficient")
    void testSuccessfulP2PTransfer() {
        when(walletRepository.resolveRecipientByIdentifier(recipientWalletId.toString()))
                .thenReturn(Optional.of(recipientWallet));

        Map<UUID, Wallet> lockedWallets = new HashMap<>();
        lockedWallets.put(senderWalletId, senderWallet);
        lockedWallets.put(recipientWalletId, recipientWallet);
        when(walletLockManager.acquireLocksAsMap(anyList())).thenReturn(lockedWallets);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransferRequestDto request = TransferRequestDto.builder()
                .senderWalletId(senderWalletId)
                .recipientIdentifier(recipientWalletId.toString())
                .amountInMinorUnits(4000L)
                .currencyCode("NGN")
                .narration("Dinner split")
                .build();

        TransferResponseDto response = transferService.processTransfer(request);

        assertNotNull(response);
        assertEquals("COMPLETED", response.getStatus());

        // Check balances updated correctly
        assertEquals(6000L, senderWallet.getAvailableBalanceInMinorUnits());
        assertEquals(9000L, recipientWallet.getAvailableBalanceInMinorUnits());

        // Verify events published
        verify(eventPublisher, times(1)).publishEvent(any(TransferInitiatedEvent.class));
        verify(eventPublisher, times(1)).publishEvent(any(TransferCompletedEvent.class));
    }

    @Test
    @DisplayName("Should throw InsufficientFundsException when sender balance is lower than transfer amount")
    void testInsufficientBalanceRejection() {
        when(walletRepository.resolveRecipientByIdentifier(recipientWalletId.toString()))
                .thenReturn(Optional.of(recipientWallet));

        Map<UUID, Wallet> lockedWallets = new HashMap<>();
        lockedWallets.put(senderWalletId, senderWallet);
        lockedWallets.put(recipientWalletId, recipientWallet);
        when(walletLockManager.acquireLocksAsMap(anyList())).thenReturn(lockedWallets);

        TransferRequestDto request = TransferRequestDto.builder()
                .senderWalletId(senderWalletId)
                .recipientIdentifier(recipientWalletId.toString())
                .amountInMinorUnits(20000L) // Exceeds 10000L available
                .currencyCode("NGN")
                .build();

        assertThrows(InsufficientFundsException.class, () -> transferService.processTransfer(request));
    }

    @Test
    @DisplayName("Should reject self-transfer when sender and recipient are identical")
    void testSelfTransferRejection() {
        when(walletRepository.resolveRecipientByIdentifier(senderWalletId.toString()))
                .thenReturn(Optional.of(senderWallet));

        TransferRequestDto request = TransferRequestDto.builder()
                .senderWalletId(senderWalletId)
                .recipientIdentifier(senderWalletId.toString())
                .amountInMinorUnits(1000L)
                .currencyCode("NGN")
                .build();

        assertThrows(BusinessRuleException.class, () -> transferService.processTransfer(request));
    }
}
