package com.orapay.split.model;

import com.orapay.wallet.model.Wallet;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "split_orders")
@Getter
@Setter
@NoArgsConstructor
public class SplitOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "split_order_id", nullable = false, updatable = false)
    private UUID splitOrderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_wallet_id")
    private Wallet payerWallet;

    @Column(name = "total_amount_minor_units", nullable = false)
    private long totalAmountInMinorUnits;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SplitOrderStatus status;

    @OneToMany(mappedBy = "splitOrder", cascade = CascadeType.ALL)
    private List<SplitAllocation> allocations;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public enum SplitOrderStatus {
        PENDING,
        SETTLED,
        FAILED
    }
}
