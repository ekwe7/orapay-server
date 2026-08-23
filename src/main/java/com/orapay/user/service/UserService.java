package com.orapay.user.service;

import com.orapay.user.dto.UserRegistrationRequestDto;
import com.orapay.user.dto.UserResponseDto;
import com.orapay.user.dto.UserStatusUpdateRequestDto;

import java.util.UUID;

public interface UserService {

    UserResponseDto registerNewUserAccount(UserRegistrationRequestDto registrationRequestDto);

    UserResponseDto getUserAccountProfileByUniqueId(UUID targetUserUniqueId);

    UserResponseDto updateUserAccountStatus(UUID targetUserUniqueId, UserStatusUpdateRequestDto statusUpdateRequestDto);
}
