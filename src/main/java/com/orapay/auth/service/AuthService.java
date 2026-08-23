package com.orapay.auth.service;

import com.orapay.auth.dto.LoginRequestDto;
import com.orapay.auth.dto.RefreshTokenRequestDto;
import com.orapay.auth.dto.TokenResponseDto;
import com.orapay.user.dto.UserRegistrationRequestDto;

public interface AuthService {

    TokenResponseDto register(UserRegistrationRequestDto registrationRequestDto);

    TokenResponseDto login(LoginRequestDto loginRequestDto);

    TokenResponseDto refreshToken(RefreshTokenRequestDto refreshTokenRequestDto);

    void logout(RefreshTokenRequestDto refreshTokenRequestDto);
}
