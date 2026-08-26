package com.orapay.ledger.listener;

import com.orapay.common.event.EventPublisher;
import com.orapay.ledger.event.LedgerEntryPostedEvent;
import com.orapay.ledger.model.AllocationRole;
import com.orapay.ledger.model.EntryDirection;
import com.orapay.ledger.model.LedgerEntry;
import com.orapay.ledger.repository.LedgerRepository;
import com.orapay.ledger.util.LedgerIntegrityVerifier;
import com.orapay.split.event.SplitPaymentCompletedEvent;
import com.orapay.split.model.SplitAllocation;
import com.orapay.split.model.SplitOrder;
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
class SplitPaymentLedgerEventListenerTest {

    @Mock
    private LedgerRepository ledgerRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private LedgerIntegrityVerifier ledgerIntegrityVerifier;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private SplitPaymentLedgerEventListener listener;

    private UUID payerWalletId;
    private UUID recipient1WalletId;
    private UUID recipient2WalletId;
    private UUID splitOrderId;
    private SplitOrder splitOrder;

    @BeforeEach
    void setUp() {
        payerWalletId = UUID.randomUUID();
        recipient1WalletId = UUID.randomUUID();
        recipient2WalletId = UUID.randomUUID();
        splitOrderId = UUID.randomUUID();

        Wallet payerWallet = new Wallet();
        payerWallet.setWalletId(payerWalletId);
        payerWallet.setAvailableBalanceInMinorUnits(5000L);

        Wallet recipient1 = new Wallet();
        recipient1.setWalletId(recipient1WalletId);
        recipient1.setAvailableBalanceInMinorUnits(3000L);

        Wallet recipient2 = new Wallet();
        recipient2.setWalletId(recipient2WalletId);
        recipient2.setAvailableBalanceInMinorUnits(2000L);

        splitOrder = new SplitOrder();
        splitOrder.setSplitOrderId(splitOrderId);
        splitOrder.setPayerWallet(payerWallet);
        splitOrder.setTotalAmountInMinorUnits(5000L);
        splitOrder.setCurrencyCode("NGN");

        SplitAllocation alloc1 = new SplitAllocation();
        alloc1.setAllocationId(UUID.randomUUID());
        alloc1.setSplitOrder(splitOrder);
        alloc1.setRecipientWallet(recipient1);
        alloc1.setAllocatedAmountInMinorUnits(3000L);

        SplitAllocation alloc2 = new SplitAllocation();
        alloc2.setAllocationId(UUID.randomUUID());
        alloc2.setSplitOrder(splitOrder);
        alloc2.setRecipientWallet(recipient2);
        alloc2.setAllocatedAmountInMinorUnits(2000L);

        splitOrder.setAllocations(List.of(alloc1, alloc2));

        when(walletRepository.findById(payerWalletId)).thenReturn(Optional.of(payerWallet));
        when(walletRepository.findById(recipient1WalletId)).thenReturn(Optional.of(recipient1));
        when(walletRepository.findById(recipient2WalletId)).thenReturn(Optional.of(recipient2));
        when(ledgerRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Should create balanced DEBIT and multi-CREDIT ledger entries for SplitPaymentCompletedEvent")
    void shouldCreateBalancedLedgerEntriesOnSplitPaymentCompleted() {
        SplitPaymentCompletedEvent event = new SplitPaymentCompletedEvent(this, splitOrder);

        listener.onSplitPaymentCompleted(event);

        verify(ledgerIntegrityVerifier).validateAndVerifyBalanced(eq(listener), eq(splitOrderId), any());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LedgerEntry>> entriesCaptor = ArgumentCaptor.forClass(List.class);
        verify(ledgerRepository).saveAll(entriesCaptor.capture());

        List<LedgerEntry> entries = entriesCaptor.getValue();
        assertThat(entries).hasSize(3); // 1 DEBIT + 2 CREDITs

        LedgerEntry payerEntry = entries.stream().filter(e -> e.getEntryDirection() == EntryDirection.DEBIT).findFirst().orElseThrow();
        assertThat(payerEntry.getWalletId()).isEqualTo(payerWalletId);
        assertThat(payerEntry.getAmountInMinorUnits()).isEqualTo(5000L);
        assertThat(payerEntry.getBalanceAfterPostingUnits()).isEqualTo(5000L);

        List<LedgerEntry> creditEntries = entries.stream().filter(e -> e.getEntryDirection() == EntryDirection.CREDIT).toList();
        assertThat(creditEntries).hasSize(2);
        long totalCreditAmount = creditEntries.stream().mapToLong(LedgerEntry::getAmountInMinorUnits).sum();
        assertThat(totalCreditAmount).isEqualTo(5000L);

        verify(eventPublisher).publishEvent(any(LedgerEntryPostedEvent.class));
    }
}
