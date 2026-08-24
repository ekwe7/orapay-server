package com.orapay.auth.service;

import com.orapay.auth.dto.request.LoginRequestDto;
import com.orapay.auth.dto.request.RefreshTokenRequestDto;
import com.orapay.auth.dto.response.TokenResponseDto;
import com.orapay.user.dto.request.UserRegistrationRequestDto;

public interface AuthService {

    TokenResponseDto register(UserRegistrationRequestDto registrationRequestDto);

    TokenResponseDto login(LoginRequestDto loginRequestDto);

    TokenResponseDto refreshToken(RefreshTokenRequestDto refreshTokenRequestDto);

    void logout(RefreshTokenRequestDto refreshTokenRequestDto);
}
