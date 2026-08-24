package com.orapay.split.model;

import com.orapay.wallet.model.Wallet;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "split_allocations")
@Getter
@Setter
@NoArgsConstructor
public class SplitAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "allocation_id", nullable = false, updatable = false)
    private UUID allocationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "split_order_id")
    private SplitOrder splitOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_wallet_id")
    private Wallet recipientWallet;

    @Column(name = "allocated_amount_minor_units", nullable = false)
    private long allocatedAmountInMinorUnits;
}
