package com.orapay.split.model;

import com.orapay.wallet.model.Wallet;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "split_template_rules")
@Getter
@Setter
@NoArgsConstructor
public class SplitTemplateRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "rule_id", nullable = false, updatable = false)
    private UUID ruleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private SplitTemplate splitTemplate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_wallet_id", nullable = false)
    private Wallet recipientWallet;

    @Column(name = "recipient_name", length = 100)
    private String recipientName;

    @Column(name = "percentage", precision = 5, scale = 2)
    private BigDecimal percentage;

    @Column(name = "fixed_amount_minor_units")
    private Long fixedAmountInMinorUnits;
}
