package com.orapay.ledger.listener;

import com.orapay.common.event.EventPublisher;
import com.orapay.ledger.event.LedgerEntryPostedEvent;
import com.orapay.ledger.model.AllocationRole;
import com.orapay.ledger.model.EntryDirection;
import com.orapay.ledger.model.LedgerEntry;
import com.orapay.ledger.repository.LedgerRepository;
import com.orapay.ledger.util.LedgerIntegrityVerifier;
import com.orapay.transfer.event.TransferCompletedEvent;
import com.orapay.transfer.model.Transaction;
import com.orapay.wallet.model.Wallet;
import com.orapay.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferLedgerEventListenerTest {

    @Mock
    private LedgerRepository ledgerRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private LedgerIntegrityVerifier ledgerIntegrityVerifier;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private TransferLedgerEventListener listener;

    private UUID senderWalletId;
    private UUID recipientWalletId;
    private UUID transactionId;
    private Transaction transaction;

    @BeforeEach
    void setUp() {
        senderWalletId = UUID.randomUUID();
        recipientWalletId = UUID.randomUUID();
        transactionId = UUID.randomUUID();

        Wallet senderWallet = new Wallet();
        senderWallet.setWalletId(senderWalletId);
        senderWallet.setAvailableBalanceInMinorUnits(4000L);

        Wallet recipientWallet = new Wallet();
        recipientWallet.setWalletId(recipientWalletId);
        recipientWallet.setAvailableBalanceInMinorUnits(6000L);

        transaction = new Transaction();
        transaction.setTransactionId(transactionId);
        transaction.setSenderWallet(senderWallet);
        transaction.setRecipientWallet(recipientWallet);
        transaction.setAmountInMinorUnits(1000L);
        transaction.setCurrencyCode("NGN");
        transaction.setReference("TXN-12345");

        when(walletRepository.findById(senderWalletId)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findById(recipientWalletId)).thenReturn(Optional.of(recipientWallet));
        when(ledgerRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Should create balanced DEBIT and CREDIT ledger entries and emit LedgerEntryPostedEvent upon TransferCompletedEvent")
    void shouldCreateBalancedLedgerEntriesOnTransferCompleted() {
        TransferCompletedEvent event = new TransferCompletedEvent(this, transaction);

        listener.onTransferCompleted(event);

        verify(ledgerIntegrityVerifier).validateAndVerifyBalanced(eq(listener), eq(transactionId), any());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LedgerEntry>> entriesCaptor = ArgumentCaptor.forClass(List.class);
        verify(ledgerRepository).saveAll(entriesCaptor.capture());

        List<LedgerEntry> entries = entriesCaptor.getValue();
        assertThat(entries).hasSize(2);

        LedgerEntry debitEntry = entries.stream().filter(e -> e.getEntryDirection() == EntryDirection.DEBIT).findFirst().orElseThrow();
        assertThat(debitEntry.getWalletId()).isEqualTo(senderWalletId);
        assertThat(debitEntry.getAmountInMinorUnits()).isEqualTo(1000L);
        assertThat(debitEntry.getAllocationRole()).isEqualTo(AllocationRole.DEBIT);
        assertThat(debitEntry.getBalanceAfterPostingUnits()).isEqualTo(4000L);

        LedgerEntry creditEntry = entries.stream().filter(e -> e.getEntryDirection() == EntryDirection.CREDIT).findFirst().orElseThrow();
        assertThat(creditEntry.getWalletId()).isEqualTo(recipientWalletId);
        assertThat(creditEntry.getAmountInMinorUnits()).isEqualTo(1000L);
        assertThat(creditEntry.getAllocationRole()).isEqualTo(AllocationRole.CREDIT);
        assertThat(creditEntry.getBalanceAfterPostingUnits()).isEqualTo(6000L);

        verify(eventPublisher).publishEvent(any(LedgerEntryPostedEvent.class));
    }
}
