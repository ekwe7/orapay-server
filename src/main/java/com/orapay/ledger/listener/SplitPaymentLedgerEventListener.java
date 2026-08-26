package com.orapay.ledger.listener;

import com.orapay.common.event.EventPublisher;
import com.orapay.ledger.event.LedgerEntryPostedEvent;
import com.orapay.ledger.model.AllocationRole;
import com.orapay.ledger.model.EntryDirection;
import com.orapay.ledger.model.LedgerEntry;
import com.orapay.ledger.repository.LedgerRepository;
import com.orapay.ledger.util.LedgerIntegrityVerifier;
import com.orapay.split.event.SplitPaymentCompletedEvent;
import com.orapay.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SplitPaymentLedgerEventListener {

    private final LedgerRepository ledgerRepository;
    private final WalletRepository walletRepository;
    private final LedgerIntegrityVerifier ledgerIntegrityVerifier;
    private final EventPublisher eventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onSplitPaymentCompleted(SplitPaymentCompletedEvent event) {
        List<LedgerEntry> entries = new ArrayList<>();

        // 1. Debit entry for payer wallet
        LedgerEntry payerEntry = new LedgerEntry();
        payerEntry.setTransactionId(event.getSplitOrderId());
        payerEntry.setWalletId(event.getPayerWalletId());
        payerEntry.setEntryDirection(EntryDirection.DEBIT);
        payerEntry.setAmountInMinorUnits(event.getTotalAmountInMinorUnits());
        payerEntry.setCurrencyCode(event.getCurrencyCode());
        payerEntry.setAllocationRole(AllocationRole.DEBIT);
        payerEntry.setReference("SPLIT-" + event.getSplitOrderId());

        walletRepository.findById(event.getPayerWalletId()).ifPresent(wallet ->
                payerEntry.setBalanceAfterPostingUnits(wallet.getAvailableBalanceInMinorUnits())
        );

        entries.add(payerEntry);

        // 2. Credit entries for each split allocation leg
        for (SplitPaymentCompletedEvent.SplitAllocationEventData allocation : event.getAllocations()) {
            LedgerEntry creditEntry = new LedgerEntry();
            creditEntry.setTransactionId(event.getSplitOrderId());
            creditEntry.setWalletId(allocation.getRecipientWalletId());
            creditEntry.setEntryDirection(EntryDirection.CREDIT);
            creditEntry.setAmountInMinorUnits(allocation.getAllocatedAmountInMinorUnits());
            creditEntry.setCurrencyCode(event.getCurrencyCode());
            creditEntry.setAllocationRole(AllocationRole.CREDIT);
            creditEntry.setReference("SPLIT-LEG-" + allocation.getAllocationId());

            walletRepository.findById(allocation.getRecipientWalletId()).ifPresent(wallet ->
                    creditEntry.setBalanceAfterPostingUnits(wallet.getAvailableBalanceInMinorUnits())
            );

            entries.add(creditEntry);
        }

        // 3. Verify double-entry integrity (sum(Debits) - sum(Credits) == 0)
        ledgerIntegrityVerifier.validateAndVerifyBalanced(this, event.getSplitOrderId(), entries);

        // 4. Save balanced ledger postings
        List<LedgerEntry> savedEntries = ledgerRepository.saveAll(entries);

        // 5. Emit LedgerEntryPostedEvent
        eventPublisher.publishEvent(new LedgerEntryPostedEvent(this, event.getSplitOrderId(), savedEntries));
    }
}
