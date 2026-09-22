package com.orapay.notification.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;

import com.orapay.notification.model.NotificationLog;

public interface NotificationLogRepositoryCustom {

    List<NotificationLog> findRetryableFailuresCustom(int maxRetries, Pageable pageable);

    List<NotificationLog> findRecentNotificationsByUserAndChannelCustom(UUID userId, String channel, OffsetDateTime windowStart);

    int bulkUpdateStatusAndIncrementRetryCustom(UUID notificationId, String newStatus, String errorMessage);
}
