package com.hive.notification.repository;

import com.hive.notification.domain.entity.Notification;
import com.hive.notification.domain.enums.NotificationChannel;
import com.hive.notification.domain.enums.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for Notification entities
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * Find notifications by tenant and recipient
     */
    Page<Notification> findByTenantIdAndRecipientId(String tenantId, String recipientId, Pageable pageable);

    /**
     * Find unread notifications by tenant and recipient
     */
    List<Notification> findByTenantIdAndRecipientIdAndReadAtIsNull(String tenantId, String recipientId);

    /**
     * Count unread notifications by tenant and recipient
     */
    long countByTenantIdAndRecipientIdAndReadAtIsNull(String tenantId, String recipientId);

    /**
     * Find notifications by campaign
     */
    Page<Notification> findByCampaignId(String campaignId, Pageable pageable);

    /**
     * Find notifications by status
     */
    List<Notification> findByStatus(NotificationStatus status);

    /**
     * Find notifications by tenant and status
     */
    List<Notification> findByTenantIdAndStatus(String tenantId, NotificationStatus status);

    /**
     * Find notifications by channel
     */
    List<Notification> findByChannel(NotificationChannel channel);

    /**
     * Find notifications by tenant and channel
     */
    List<Notification> findByTenantIdAndChannel(String tenantId, NotificationChannel channel);

    /**
     * Find notifications scheduled for delivery
     */
    @Query("SELECT n FROM Notification n WHERE n.scheduledAt IS NOT NULL AND n.scheduledAt <= :now AND n.status = 'QUEUED'")
    List<Notification> findScheduledForDelivery(@Param("now") LocalDateTime now);

    /**
     * Find expired notifications
     */
    @Query("SELECT n FROM Notification n WHERE n.expiresAt IS NOT NULL AND n.expiresAt < :now AND n.status NOT IN ('DELIVERED', 'READ', 'EXPIRED', 'CANCELLED')")
    List<Notification> findExpiredNotifications(@Param("now") LocalDateTime now);

    /**
     * Find notifications by correlation ID
     */
    List<Notification> findByCorrelationId(String correlationId);

    /**
     * Find notifications by external ID
     */
    List<Notification> findByExternalId(String externalId);

    /**
     * Count notifications by tenant and date range
     */
    long countByTenantIdAndCreatedAtBetween(String tenantId, LocalDateTime from, LocalDateTime to);

    /**
     * Count notifications by tenant, status and date range
     */
    long countByTenantIdAndStatusAndCreatedAtBetween(String tenantId, NotificationStatus status, 
                                                    LocalDateTime from, LocalDateTime to);

    /**
     * Count notifications by tenant, channel and date range
     */
    long countByTenantIdAndChannelAndCreatedAtBetween(String tenantId, NotificationChannel channel, 
                                                     LocalDateTime from, LocalDateTime to);

    /**
     * Find notifications that can be retried
     */
    @Query("SELECT n FROM Notification n WHERE n.status = 'FAILED' AND n.retryCount < n.maxRetries AND (n.expiresAt IS NULL OR n.expiresAt > :now)")
    List<Notification> findRetryableNotifications(@Param("now") LocalDateTime now);

    /**
     * Find notifications by recipient and date range
     */
    @Query("SELECT n FROM Notification n WHERE n.tenantId = :tenantId AND n.recipientId = :recipientId AND n.createdAt BETWEEN :from AND :to")
    List<Notification> findByRecipientAndDateRange(@Param("tenantId") String tenantId, 
                                                  @Param("recipientId") String recipientId,
                                                  @Param("from") LocalDateTime from, 
                                                  @Param("to") LocalDateTime to);

    /**
     * Find notifications by template
     */
    List<Notification> findByTemplateId(String templateId);

    /**
     * Get notification delivery statistics
     */
    @Query("SELECT " +
           "COUNT(*) as total, " +
           "SUM(CASE WHEN n.status = 'SENT' OR n.status = 'DELIVERED' OR n.status = 'READ' THEN 1 ELSE 0 END) as successful, " +
           "SUM(CASE WHEN n.status = 'FAILED' OR n.status = 'BOUNCED' THEN 1 ELSE 0 END) as failed, " +
           "SUM(CASE WHEN n.status = 'READ' THEN 1 ELSE 0 END) as read " +
           "FROM Notification n WHERE n.tenantId = :tenantId AND n.createdAt BETWEEN :from AND :to")
    Object[] getDeliveryStatistics(@Param("tenantId") String tenantId, 
                                  @Param("from") LocalDateTime from, 
                                  @Param("to") LocalDateTime to);

    /**
     * Get channel performance statistics
     */
    @Query("SELECT n.channel, " +
           "COUNT(*) as total, " +
           "SUM(CASE WHEN n.status = 'DELIVERED' OR n.status = 'READ' THEN 1 ELSE 0 END) as delivered, " +
           "SUM(CASE WHEN n.status = 'FAILED' THEN 1 ELSE 0 END) as failed " +
           "FROM Notification n WHERE n.tenantId = :tenantId AND n.createdAt BETWEEN :from AND :to " +
           "GROUP BY n.channel")
    List<Object[]> getChannelStatistics(@Param("tenantId") String tenantId, 
                                       @Param("from") LocalDateTime from, 
                                       @Param("to") LocalDateTime to);

    /**
     * Find notifications with delivery tracking data
     */
    @Query("SELECT n FROM Notification n WHERE n.deliveryTracking IS NOT NULL AND SIZE(n.deliveryTracking) > 0")
    List<Notification> findNotificationsWithTracking();

    /**
     * Find notifications by priority
     */
    List<Notification> findByTenantIdAndPriority(String tenantId, 
                                                com.hive.notification.domain.enums.NotificationPriority priority);

    /**
     * Find notifications sent in the last N minutes
     */
    @Query("SELECT n FROM Notification n WHERE n.sentAt > :cutoff")
    List<Notification> findRecentlySent(@Param("cutoff") LocalDateTime cutoff);

    /**
     * Find notifications by recipient contact for a specific channel
     */
    List<Notification> findByTenantIdAndRecipientContactAndChannel(String tenantId, String recipientContact, 
                                                                  NotificationChannel channel);

    /**
     * Count daily notifications for rate limiting
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.tenantId = :tenantId AND n.recipientId = :recipientId " +
           "AND DATE(n.createdAt) = DATE(:date)")
    long countDailyNotifications(@Param("tenantId") String tenantId, 
                                @Param("recipientId") String recipientId, 
                                @Param("date") LocalDateTime date);

    /**
     * Count hourly notifications for rate limiting
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.tenantId = :tenantId AND n.recipientId = :recipientId " +
           "AND n.createdAt >= :hourAgo")
    long countHourlyNotifications(@Param("tenantId") String tenantId, 
                                 @Param("recipientId") String recipientId, 
                                 @Param("hourAgo") LocalDateTime hourAgo);

    /**
     * Find last notification sent to user
     */
    @Query("SELECT n FROM Notification n WHERE n.tenantId = :tenantId AND n.recipientId = :recipientId " +
           "AND n.sentAt IS NOT NULL ORDER BY n.sentAt DESC LIMIT 1")
    Notification findLastSentNotification(@Param("tenantId") String tenantId, @Param("recipientId") String recipientId);

    /**
     * Find notifications for bulk operations
     */
    @Query("SELECT n FROM Notification n WHERE n.id IN :notificationIds")
    List<Notification> findAllByIds(@Param("notificationIds") List<UUID> notificationIds);

    /**
     * Delete old notifications
     */
    @Query("DELETE FROM Notification n WHERE n.createdAt < :cutoff")
    void deleteOldNotifications(@Param("cutoff") LocalDateTime cutoff);
}