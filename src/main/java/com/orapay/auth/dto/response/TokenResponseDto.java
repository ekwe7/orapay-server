package com.orapay.auth.dto.response;

import com.orapay.user.dto.response.UserResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponseDto {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresInSeconds;
    private UserResponseDto user;
}
