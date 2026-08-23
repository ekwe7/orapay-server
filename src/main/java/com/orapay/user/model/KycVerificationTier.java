package com.orapay.user.model;

import lombok.Getter;

@Getter
public enum KycVerificationTier {
    TIER_1("Tier 1 Basic Verification", 500000L),   // 5,000.00 limit in minor units
    TIER_2("Tier 2 Standard Verification", 5000000L), // 50,000.00 limit in minor units
    TIER_3("Tier 3 Advanced Verification", 50000000L); // 500,000.00 limit in minor units

    private final String tierDescriptionText;
    private final long maximumSingleTransactionLimitInMinorUnits;

    KycVerificationTier(String tierDescriptionText, long maximumSingleTransactionLimitInMinorUnits) {
        this.tierDescriptionText = tierDescriptionText;
        this.maximumSingleTransactionLimitInMinorUnits = maximumSingleTransactionLimitInMinorUnits;
    }
}