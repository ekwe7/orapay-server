package com.orapay.auth.controller;

import com.orapay.auth.dto.request.LoginRequestDto;
import com.orapay.auth.dto.request.RefreshTokenRequestDto;
import com.orapay.auth.dto.response.TokenResponseDto;
import com.orapay.auth.service.AuthService;
import com.orapay.common.dto.response.ApiResponse;
import com.orapay.user.dto.request.UserRegistrationRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user registration, login, and JWT token management")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user and issue access and refresh tokens")
    public ResponseEntity<ApiResponse<TokenResponseDto>> register(
            @Valid @RequestBody UserRegistrationRequestDto registrationRequestDto
    ) {
        TokenResponseDto tokenResponse = authService.register(registrationRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", tokenResponse));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user with email/phone and password")
    public ResponseEntity<ApiResponse<TokenResponseDto>> login(
            @Valid @RequestBody LoginRequestDto loginRequestDto
    ) {
        TokenResponseDto tokenResponse = authService.login(loginRequestDto);
        return ResponseEntity.ok(ApiResponse.success("Authentication successful", tokenResponse));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renew access token using refresh token")
    public ResponseEntity<ApiResponse<TokenResponseDto>> refreshToken(
            @Valid @RequestBody RefreshTokenRequestDto refreshTokenRequestDto
    ) {
        TokenResponseDto tokenResponse = authService.refreshToken(refreshTokenRequestDto);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", tokenResponse));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user and invalidate refresh token")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody RefreshTokenRequestDto refreshTokenRequestDto
    ) {
        authService.logout(refreshTokenRequestDto);
        return ResponseEntity.ok(ApiResponse.success("User logged out successfully", null));
    }
}
