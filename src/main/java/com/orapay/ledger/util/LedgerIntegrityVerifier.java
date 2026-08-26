package com.orapay.ledger.util;

import com.orapay.common.event.EventPublisher;
import com.orapay.common.exception.BusinessRuleException;
import com.orapay.ledger.event.LedgerDiscrepancyDetectedEvent;
import com.orapay.ledger.model.EntryDirection;
import com.orapay.ledger.model.LedgerEntry;
import com.orapay.ledger.repository.LedgerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class LedgerIntegrityVerifier {

    private final EventPublisher eventPublisher;
    private final LedgerRepository ledgerRepository;

    /**
     * Verifies that the provided list of ledger entries is balanced:
     * sum(Debits) - sum(Credits) == 0.
     *
     * If imbalanced, emits a LedgerDiscrepancyDetectedEvent and throws a BusinessRuleException.
     */
    public boolean validateAndVerifyBalanced(Object source, UUID transactionId, List<LedgerEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            throw new BusinessRuleException("Ledger entries list cannot be null or empty for transaction: " + transactionId);
        }

        long totalDebits = entries.stream()
                .filter(e -> e.getEntryDirection() == EntryDirection.DEBIT)
                .mapToLong(LedgerEntry::getAmountInMinorUnits)
                .sum();

        long totalCredits = entries.stream()
                .filter(e -> e.getEntryDirection() == EntryDirection.CREDIT)
                .mapToLong(LedgerEntry::getAmountInMinorUnits)
                .sum();

        long discrepancy = totalDebits - totalCredits;

        if (discrepancy != 0) {
            String reason = String.format("Ledger integrity violation for transaction %s: total debits = %d, total credits = %d, discrepancy = %d",
                    transactionId, totalDebits, totalCredits, discrepancy);
            log.error(reason);

            eventPublisher.publishEvent(new LedgerDiscrepancyDetectedEvent(source, transactionId, totalDebits, totalCredits, reason));
            throw new BusinessRuleException(reason);
        }

        return true;
    }

    /**
     * Verifies stored transaction ledger integrity by transaction ID:
     * sum(Debits) - sum(Credits) == 0.
     */
    public boolean verifyTransactionIntegrity(Object source, UUID transactionId) {
        List<LedgerEntry> entries = ledgerRepository.findByTransactionId(transactionId);
        if (entries.isEmpty()) {
            return true;
        }

        long totalDebits = entries.stream()
                .filter(e -> e.getEntryDirection() == EntryDirection.DEBIT)
                .mapToLong(LedgerEntry::getAmountInMinorUnits)
                .sum();

        long totalCredits = entries.stream()
                .filter(e -> e.getEntryDirection() == EntryDirection.CREDIT)
                .mapToLong(LedgerEntry::getAmountInMinorUnits)
                .sum();

        long discrepancy = totalDebits - totalCredits;

        if (discrepancy != 0) {
            String reason = String.format("Stored ledger entries imbalanced for transaction %s: total debits = %d, total credits = %d",
                    transactionId, totalDebits, totalCredits);
            log.error(reason);
            eventPublisher.publishEvent(new LedgerDiscrepancyDetectedEvent(source, transactionId, totalDebits, totalCredits, reason));
            return false;
        }

        return true;
    }
}
