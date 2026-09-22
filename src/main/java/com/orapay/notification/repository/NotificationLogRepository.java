package com.orapay.notification.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.orapay.notification.model.NotificationChannel;
import com.orapay.notification.model.NotificationLog;
import com.orapay.notification.model.NotificationStatus;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID>, NotificationLogRepositoryCustom {

    Page<NotificationLog> findByUserId(UUID userId, Pageable pageable);

    Page<NotificationLog> findByChannelAndStatus(NotificationChannel channel, NotificationStatus status, Pageable pageable);
}
