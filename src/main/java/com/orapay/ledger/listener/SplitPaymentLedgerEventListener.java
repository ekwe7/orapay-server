package com.orapay.ledger.listener;

import com.orapay.ledger.model.AllocationRole;
import com.orapay.ledger.model.EntryDirection;
import com.orapay.ledger.model.LedgerEntry;
import com.orapay.ledger.repository.LedgerRepository;
import com.orapay.split.event.SplitPaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SplitPaymentLedgerEventListener {

    private final LedgerRepository ledgerRepository;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onSplitPaymentCompleted(SplitPaymentCompletedEvent event) {
        List<LedgerEntry> entries = new ArrayList<>();

        // 1. Debit entry for payer wallet
        LedgerEntry payerEntry = new LedgerEntry();
        payerEntry.setTransactionId(event.getSplitOrderId());
        payerEntry.setWalletId(event.getPayerWalletId());
        payerEntry.setDirection(EntryDirection.DEBIT);
        payerEntry.setAmountInMinorUnits(event.getTotalAmountInMinorUnits());
        payerEntry.setCurrencyCode(event.getCurrencyCode());
        payerEntry.setAllocationRole(AllocationRole.DEBIT);
        payerEntry.setReference("SPLIT-" + event.getSplitOrderId());
        entries.add(payerEntry);

        // 2. Credit entries for each split allocation leg
        for (SplitPaymentCompletedEvent.SplitAllocationEventData allocation : event.getAllocations()) {
            LedgerEntry creditEntry = new LedgerEntry();
            creditEntry.setTransactionId(event.getSplitOrderId());
            creditEntry.setWalletId(allocation.getRecipientWalletId());
            creditEntry.setDirection(EntryDirection.CREDIT);
            creditEntry.setAmountInMinorUnits(allocation.getAllocatedAmountInMinorUnits());
            creditEntry.setCurrencyCode(event.getCurrencyCode());
            creditEntry.setAllocationRole(AllocationRole.CREDIT);
            creditEntry.setReference("SPLIT-LEG-" + allocation.getAllocationId());
            entries.add(creditEntry);
        }

        ledgerRepository.saveAll(entries);
    }
}
