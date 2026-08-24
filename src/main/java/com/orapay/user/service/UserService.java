package com.orapay.user.service;

import com.orapay.user.dto.request.UserRegistrationRequestDto;
import com.orapay.user.dto.request.UserStatusUpdateRequestDto;
import com.orapay.user.dto.response.UserResponseDto;

import java.util.UUID;

public interface UserService {

    UserResponseDto registerNewUserAccount(UserRegistrationRequestDto registrationRequestDto);

    UserResponseDto getUserAccountProfileByUniqueId(UUID targetUserUniqueId);

    UserResponseDto updateUserAccountStatus(UUID targetUserUniqueId, UserStatusUpdateRequestDto statusUpdateRequestDto);
}
