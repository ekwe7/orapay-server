package com.orapay.split.model;

import com.orapay.wallet.model.Wallet;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "split_templates")
@Getter
@Setter
@NoArgsConstructor
public class SplitTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "template_id", nullable = false, updatable = false)
    private UUID templateId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_wallet_id", nullable = false)
    private Wallet merchantWallet;

    @Column(name = "template_name", nullable = false, length = 100)
    private String templateName;

    @Column(name = "fee_category", nullable = false, length = 50)
    private String feeCategory;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "splitTemplate", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SplitTemplateRule> rules = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
