package com.orapay.ledger.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries")
@Getter
@Setter
@NoArgsConstructor
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "entry_id", nullable = false, updatable = false)
    private UUID entryId;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 10)
    private EntryDirection direction;

    @Column(name = "amount_minor_units", nullable = false)
    private long amountInMinorUnits;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "allocation_role", nullable = false, length = 20)
    private AllocationRole allocationRole;

    @Column(name = "reference", length = 255)
    private String reference;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
