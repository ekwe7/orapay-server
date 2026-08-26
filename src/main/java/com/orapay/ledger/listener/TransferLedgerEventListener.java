package com.orapay.ledger.listener;

import com.orapay.common.event.EventPublisher;
import com.orapay.ledger.event.LedgerEntryPostedEvent;
import com.orapay.ledger.model.AllocationRole;
import com.orapay.ledger.model.EntryDirection;
import com.orapay.ledger.model.LedgerEntry;
import com.orapay.ledger.repository.LedgerRepository;
import com.orapay.ledger.util.LedgerIntegrityVerifier;
import com.orapay.transfer.event.TransferCompletedEvent;
import com.orapay.wallet.model.Wallet;
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
public class TransferLedgerEventListener {

    private final LedgerRepository ledgerRepository;
    private final WalletRepository walletRepository;
    private final LedgerIntegrityVerifier ledgerIntegrityVerifier;
    private final EventPublisher eventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onTransferCompleted(TransferCompletedEvent event) {
        List<LedgerEntry> entries = new ArrayList<>();

        // 1. Debit entry for sender wallet
        if (event.getSenderWalletId() != null) {
            LedgerEntry debitEntry = new LedgerEntry();
            debitEntry.setTransactionId(event.getTransactionId());
            debitEntry.setWalletId(event.getSenderWalletId());
            debitEntry.setEntryDirection(EntryDirection.DEBIT);
            debitEntry.setAmountInMinorUnits(event.getAmountInMinorUnits());
            debitEntry.setCurrencyCode(event.getCurrencyCode());
            debitEntry.setAllocationRole(AllocationRole.DEBIT);
            debitEntry.setReference(event.getReference());

            walletRepository.findById(event.getSenderWalletId()).ifPresent(wallet ->
                    debitEntry.setBalanceAfterPostingUnits(wallet.getAvailableBalanceInMinorUnits())
            );

            entries.add(debitEntry);
        }

        // 2. Credit entry for recipient wallet
        if (event.getRecipientWalletId() != null) {
            LedgerEntry creditEntry = new LedgerEntry();
            creditEntry.setTransactionId(event.getTransactionId());
            creditEntry.setWalletId(event.getRecipientWalletId());
            creditEntry.setEntryDirection(EntryDirection.CREDIT);
            creditEntry.setAmountInMinorUnits(event.getAmountInMinorUnits());
            creditEntry.setCurrencyCode(event.getCurrencyCode());
            creditEntry.setAllocationRole(AllocationRole.CREDIT);
            creditEntry.setReference(event.getReference());

            walletRepository.findById(event.getRecipientWalletId()).ifPresent(wallet ->
                    creditEntry.setBalanceAfterPostingUnits(wallet.getAvailableBalanceInMinorUnits())
            );

            entries.add(creditEntry);
        }

        // 3. Verify double-entry integrity (sum(Debits) - sum(Credits) == 0)
        ledgerIntegrityVerifier.validateAndVerifyBalanced(this, event.getTransactionId(), entries);

        // 4. Save balanced ledger postings
        List<LedgerEntry> savedEntries = ledgerRepository.saveAll(entries);

        // 5. Emit LedgerEntryPostedEvent
        eventPublisher.publishEvent(new LedgerEntryPostedEvent(this, event.getTransactionId(), savedEntries));
    }
}
