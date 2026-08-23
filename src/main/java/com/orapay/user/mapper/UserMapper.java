package com.orapay.user.mapper;

import com.orapay.user.dto.UserResponseDto;
import com.orapay.user.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponseDto mapUserEntityToUserResponseDto(User userEntityInstance) {
        if (userEntityInstance == null) {
            return null;
        }

        return UserResponseDto.builder()
                .userUniqueId(userEntityInstance.getUserUniqueId())
                .userFirstName(userEntityInstance.getUserFirstName())
                .userLastName(userEntityInstance.getUserLastName())
                .userEmailAddress(userEntityInstance.getUserEmailAddress())
                .PhoneNumber(userEntityInstance.getPhoneNumber())
                .userAccountStatus(userEntityInstance.getUserAccountStatus())
                .kycVerificationTier(userEntityInstance.getKycVerificationTier())
                .accountCreatedAtTimestamp(userEntityInstance.getAccountCreatedAtTimestamp())
                .accountLastUpdatedAtTimestamp(userEntityInstance.getAccountLastUpdatedAtTimestamp())
                .build();
    }
}
