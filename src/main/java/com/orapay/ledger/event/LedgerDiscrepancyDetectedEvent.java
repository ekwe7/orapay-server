package com.orapay.ledger.event;

import com.orapay.common.event.BaseDomainEvent;
import lombok.Getter;

import java.util.UUID;

@Getter
public class LedgerDiscrepancyDetectedEvent extends BaseDomainEvent {

    private final UUID transactionId;
    private final long totalDebits;
    private final long totalCredits;
    private final String reason;

    public LedgerDiscrepancyDetectedEvent(Object source, UUID transactionId, long totalDebits, long totalCredits, String reason) {
        super(source);
        this.transactionId = transactionId;
        this.totalDebits = totalDebits;
        this.totalCredits = totalCredits;
        this.reason = reason;
    }
}
