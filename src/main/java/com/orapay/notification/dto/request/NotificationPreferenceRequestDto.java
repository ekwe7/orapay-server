package com.orapay.notification.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferenceRequestDto {

    @NotNull(message = "User ID is required")
    private UUID userId;

    private boolean emailEnabled;

    private boolean smsEnabled;
}
