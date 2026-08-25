package com.orapay.ledger.listener;

import com.orapay.ledger.model.AllocationRole;
import com.orapay.ledger.model.EntryDirection;
import com.orapay.ledger.model.LedgerEntry;
import com.orapay.ledger.repository.LedgerRepository;
import com.orapay.transfer.event.TransferCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TransferLedgerEventListener {

    private final LedgerRepository ledgerRepository;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onTransferCompleted(TransferCompletedEvent event) {
        List<LedgerEntry> entries = new ArrayList<>();

        // 1. Debit entry for sender wallet
        if (event.getSenderWalletId() != null) {
            LedgerEntry debitEntry = new LedgerEntry();
            debitEntry.setTransactionId(event.getTransactionId());
            debitEntry.setWalletId(event.getSenderWalletId());
            debitEntry.setDirection(EntryDirection.DEBIT);
            debitEntry.setAmountInMinorUnits(event.getAmountInMinorUnits());
            debitEntry.setCurrencyCode(event.getCurrencyCode());
            debitEntry.setAllocationRole(AllocationRole.DEBIT);
            debitEntry.setReference(event.getReference());
            entries.add(debitEntry);
        }

        // 2. Credit entry for recipient wallet
        if (event.getRecipientWalletId() != null) {
            LedgerEntry creditEntry = new LedgerEntry();
            creditEntry.setTransactionId(event.getTransactionId());
            creditEntry.setWalletId(event.getRecipientWalletId());
            creditEntry.setDirection(EntryDirection.CREDIT);
            creditEntry.setAmountInMinorUnits(event.getAmountInMinorUnits());
            creditEntry.setCurrencyCode(event.getCurrencyCode());
            creditEntry.setAllocationRole(AllocationRole.CREDIT);
            creditEntry.setReference(event.getReference());
            entries.add(creditEntry);
        }

        ledgerRepository.saveAll(entries);
    }
}
