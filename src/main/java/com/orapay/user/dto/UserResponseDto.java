package com.orapay.user.dto;

import com.orapay.user.model.KycVerificationTier;
import com.orapay.user.model.UserAccountStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {
    private UUID userUniqueId;
    private String userFirstName;
    private String userLastName;
    private String userEmailAddress;
    private String PhoneNumber;
    private UserAccountStatus userAccountStatus;
    private KycVerificationTier kycVerificationTier;
    private Instant accountCreatedAtTimestamp;
    private Instant accountLastUpdatedAtTimestamp;
}
