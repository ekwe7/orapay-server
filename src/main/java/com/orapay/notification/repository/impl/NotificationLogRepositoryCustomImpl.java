package com.orapay.notification.repository.impl;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.orapay.notification.model.NotificationLog;
import com.orapay.notification.repository.NotificationLogRepositoryCustom;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class NotificationLogRepositoryCustomImpl implements NotificationLogRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Returns notifications that failed and have not exceeded the retry ceiling.
     * Ordered oldest-first so that the most overdue entries are retried first.
     */
    @Override
    public List<NotificationLog> findRetryableFailuresCustom(int maxRetries, Pageable pageable) {
        String jpql = """
                SELECT n FROM NotificationLog n
                WHERE n.status = 'FAILED'
                  AND n.retryCount < :maxRetries
                ORDER BY n.createdAt ASC
                """;
        return entityManager.createQuery(jpql, NotificationLog.class)
                .setParameter("maxRetries", maxRetries)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();
    }

    /**
     * Returns all notifications sent to a specific user on a specific channel
     * within a sliding time window (used for rate-limiting / deduplication).
     */
    @Override
    public List<NotificationLog> findRecentNotificationsByUserAndChannelCustom(
            UUID userId, String channel, OffsetDateTime windowStart) {
        String jpql = """
                SELECT n FROM NotificationLog n
                WHERE n.userId = :userId
                  AND n.channel = :channel
                  AND n.createdAt >= :windowStart
                ORDER BY n.createdAt DESC
                """;
        return entityManager.createQuery(jpql, NotificationLog.class)
                .setParameter("userId", userId)
                .setParameter("channel", channel)
                .setParameter("windowStart", windowStart)
                .getResultList();
    }

    /**
     * Atomically updates the status and error message of a notification log entry
     * while incrementing its retry counter. Returns the number of rows affected.
     */
    @Override
    public int bulkUpdateStatusAndIncrementRetryCustom(
            UUID notificationId, String newStatus, String errorMessage) {
        String jpql = """
                UPDATE NotificationLog n
                SET n.status = :newStatus,
                    n.errorMessage = :errorMessage,
                    n.retryCount = n.retryCount + 1
                WHERE n.notificationId = :notificationId
                """;
        return entityManager.createQuery(jpql)
                .setParameter("newStatus", newStatus)
                .setParameter("errorMessage", errorMessage)
                .setParameter("notificationId", notificationId)
                .executeUpdate();
    }
}
