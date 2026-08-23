package com.orapay.user.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userUniqueId;

    @Column(name = "first_name", nullable = false, length = 100)
    private String userFirstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String userLastName;

    @Column(name = "email_address", nullable = false, unique = true)
    private String userEmailAddress;

    @Column(name = "phone_number", nullable = false, unique = true, length = 30)
    private String PhoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_account_status", nullable = false, length = 20)
    private UserAccountStatus userAccountStatus = UserAccountStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_verification_tier", nullable = false, length = 20)
    private KycVerificationTier kycVerificationTier = KycVerificationTier.TIER_1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant accountCreatedAtTimestamp;

    @Column(name = "updated_at", nullable = false)
    private Instant accountLastUpdatedAtTimestamp;

    @PrePersist
    protected void AccountCreation() {
        Instant currentSystemTimestamp = Instant.now();
        this.accountCreatedAtTimestamp = currentSystemTimestamp;
        this.accountLastUpdatedAtTimestamp = currentSystemTimestamp;
    }

    @PreUpdate
    protected void onPreUpdateAccountModification() {
        this.accountLastUpdatedAtTimestamp = Instant.now();
    }
}
