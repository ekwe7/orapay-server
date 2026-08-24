package com.orapay.wallet.model;
    
    import com.orapay.user.model.User;
    import jakarta.persistence.*;
    import lombok.Getter;
    import lombok.NoArgsConstructor;
    import lombok.Setter;
    
    import java.time.Instant;
    import java.util.UUID;
    
    @Entity
    @Table(name = "wallets")
    @Getter
    @Setter
    @NoArgsConstructor
    public class Wallet {
    
        @Id
        @GeneratedValue(strategy = GenerationType.UUID)
        @Column(name = "wallet_id", nullable = false, updatable =
  false)
        private UUID walletId;
    
        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "user_id", nullable = false)
        private User user;
    
        @Column(name = "account_number", nullable = false, unique
  = true, length = 10)
        private String accountNumber;
    
        @Column(name = "currency_code", nullable = false, length
  = 3)
        private String currencyCode = "NGN";
    
        @Column(name = "available_balance_minor_units", nullable
  = false)
        private long availableBalanceInMinorUnits = 0L;
    
        @Column(name = "locked_balance_minor_units", nullable =
  false)
        private long lockedBalanceInMinorUnits = 0L;
    
        @Version
        @Column(name = "version_lock", nullable = false)
        private long versionLock;
    
        @Column(name = "is_active", nullable = false)
        private boolean isActive = true;
    
        @Column(name = "created_at", nullable = false, updatable
  = false)
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
