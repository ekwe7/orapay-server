package com.orapay.ledger.event;

import com.orapay.common.event.BaseDomainEvent;
import com.orapay.ledger.model.LedgerEntry;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
public class LedgerEntryPostedEvent extends BaseDomainEvent {

    private final UUID transactionId;
    private final List<LedgerEntry> entries;

    public LedgerEntryPostedEvent(Object source, UUID transactionId, List<LedgerEntry> entries) {
        super(source);
        this.transactionId = transactionId;
        this.entries = entries;
    }
}
