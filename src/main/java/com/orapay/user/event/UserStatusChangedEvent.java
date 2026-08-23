package com.orapay.user.event;

import com.orapay.common.event.BaseDomainEvent;
import com.orapay.user.model.UserAccountStatus;
import lombok.Getter;

import java.util.UUID;

@Getter
public class UserStatusChangedEvent extends BaseDomainEvent {

    private final UUID targetUserUniqueId;
    private final UserAccountStatus previousUserAccountStatus;
    private final UserAccountStatus updatedUserAccountStatus;
    private final String statusChangeReasonDescription;

    public UserStatusChangedEvent(
            Object eventSourceComponentInstance,
            UUID targetUserUniqueId,
            UserAccountStatus previousUserAccountStatus,
            UserAccountStatus updatedUserAccountStatus,
            String statusChangeReasonDescription
    ) {
        super(eventSourceComponentInstance);
        this.targetUserUniqueId = targetUserUniqueId;
        this.previousUserAccountStatus = previousUserAccountStatus;
        this.updatedUserAccountStatus = updatedUserAccountStatus;
        this.statusChangeReasonDescription = statusChangeReasonDescription;
    }
}
