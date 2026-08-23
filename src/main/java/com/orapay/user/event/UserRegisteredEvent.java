package com.orapay.user.event;

import com.orapay.common.event.BaseDomainEvent;
import com.orapay.user.model.KycVerificationTier;
import lombok.Getter;

import java.util.UUID;

@Getter
public class UserRegisteredEvent extends BaseDomainEvent {

    private final UUID newlyRegisteredUserUniqueId;
    private final String registeredUserEmailAddress;
    private final String registeredUserPhoneNumber;
    private final KycVerificationTier assignedKycVerificationTier;

    public UserRegisteredEvent(
            Object eventSourceComponentInstance,
            UUID newlyRegisteredUserUniqueId,
            String registeredUserEmailAddress,
            String registeredUserPhoneNumber,
            KycVerificationTier assignedKycVerificationTier
    ) {
        super(eventSourceComponentInstance);
        this.newlyRegisteredUserUniqueId = newlyRegisteredUserUniqueId;
        this.registeredUserEmailAddress = registeredUserEmailAddress;
        this.registeredUserPhoneNumber = registeredUserPhoneNumber;
        this.assignedKycVerificationTier = assignedKycVerificationTier;
    }
}