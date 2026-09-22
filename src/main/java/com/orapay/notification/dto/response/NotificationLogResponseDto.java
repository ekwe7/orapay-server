package com.orapay.notification.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLogResponseDto {
    private UUID notificationId;
    private UUID userId;
    private String channel;
    private String recipientAddress;
    private String status;
    private String errorMessage;
    private OffsetDateTime createdAt;
}
