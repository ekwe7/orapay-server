package com.orapay.user.controller;

import com.orapay.common.dto.ApiResponse;
import com.orapay.user.dto.UserRegistrationRequestDto;
import com.orapay.user.dto.UserResponseDto;
import com.orapay.user.dto.UserStatusUpdateRequestDto;
import com.orapay.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Identity Management", description = "Endpoints for user registration, profiles, and account status management")
public class UserController {

    private final UserService userServiceInstance;

    @PostMapping
    @Operation(summary = "Register a new user account with canonical E.164 phone number")
    public ResponseEntity<ApiResponse<UserResponseDto>> registerNewUserAccount(
            @Valid @RequestBody UserRegistrationRequestDto registrationRequestDto
    ) {
        UserResponseDto createdUserResponse = userServiceInstance.registerNewUserAccount(registrationRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User account registered successfully", createdUserResponse));
    }

    @GetMapping("/{targetUserUniqueId}")
    @Operation(summary = "Fetch user profile by unique user ID")
    public ResponseEntity<ApiResponse<UserResponseDto>> getUserAccountProfileByUniqueId(
            @PathVariable UUID targetUserUniqueId
    ) {
        UserResponseDto userResponse = userServiceInstance.getUserAccountProfileByUniqueId(targetUserUniqueId);
        return ResponseEntity.ok(ApiResponse.success("User profile fetched successfully", userResponse));
    }

    @PatchMapping("/{targetUserUniqueId}/status")
    @Operation(summary = "Update user account status (ACTIVE, SUSPENDED, FROZEN)")
    public ResponseEntity<ApiResponse<UserResponseDto>> updateUserAccountStatus(
            @PathVariable UUID targetUserUniqueId,
            @Valid @RequestBody UserStatusUpdateRequestDto statusUpdateRequestDto
    ) {
        UserResponseDto updatedUserResponse = userServiceInstance.updateUserAccountStatus(targetUserUniqueId, statusUpdateRequestDto);
        return ResponseEntity.ok(ApiResponse.success("User account status updated successfully", updatedUserResponse));
    }
}
