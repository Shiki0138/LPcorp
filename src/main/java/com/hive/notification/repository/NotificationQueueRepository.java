package com.hive.notification.repository;

import com.hive.notification.domain.entity.NotificationQueue;
import com.hive.notification.domain.enums.NotificationChannel;
import com.hive.notification.domain.enums.NotificationPriority;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for NotificationQueue entities
 */
@Repository
public interface NotificationQueueRepository extends JpaRepository<NotificationQueue, UUID> {

    /**
     * Find queue items by notification ID
     */
    List<NotificationQueue> findByNotificationId(UUID notificationId);

    /**
     * Find queue items ready for processing
     */
    @Query("SELECT q FROM NotificationQueue q WHERE " +
           "(q.status = 'PENDING' OR (q.status = 'RETRY' AND q.nextRetryAt <= :now)) " +
           "AND q.scheduledFor <= :now " +
           "ORDER BY q.priority DESC, q.scheduledFor ASC")
    List<NotificationQueue> findReadyForProcessing(@Param("now") LocalDateTime now, Pageable pageable);

    /**
     * Find high priority queue items
     */
    @Query("SELECT q FROM NotificationQueue q WHERE " +
           "(q.status = 'PENDING' OR (q.status = 'RETRY' AND q.nextRetryAt <= :now)) " +
           "AND q.scheduledFor <= :now AND q.priority IN ('HIGH', 'URGENT') " +
           "ORDER BY q.priority DESC, q.scheduledFor ASC")
    List<NotificationQueue> findHighPriorityItems(@Param("now") LocalDateTime now, Pageable pageable);

    /**
     * Find queue items by status
     */
    List<NotificationQueue> findByStatus(NotificationQueue.QueueStatus status);

    /**
     * Find queue items by tenant and status
     */
    List<NotificationQueue> findByTenantIdAndStatus(String tenantId, NotificationQueue.QueueStatus status);

    /**
     * Find queue items by channel
     */
    List<NotificationQueue> findByChannel(NotificationChannel channel);

    /**
     * Find queue items by priority
     */
    List<NotificationQueue> findByPriority(NotificationPriority priority);

    /**
     * Find scheduled queue items
     */
    @Query("SELECT q FROM NotificationQueue q WHERE q.status = 'PENDING' AND q.scheduledFor > :now")
    List<NotificationQueue> findScheduledItems(@Param("now") LocalDateTime now);

    /**
     * Find queue items scheduled for specific time range
     */
    @Query("SELECT q FROM NotificationQueue q WHERE q.scheduledFor BETWEEN :from AND :to")
    List<NotificationQueue> findScheduledBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /**
     * Find failed queue items that can be retried
     */
    @Query("SELECT q FROM NotificationQueue q WHERE q.status = 'RETRY' AND q.nextRetryAt <= :now")
    List<NotificationQueue> findRetryableItems(@Param("now") LocalDateTime now);

    /**
     * Find queue items by processing node
     */
    List<NotificationQueue> findByProcessingNodeId(String processingNodeId);

    /**
     * Find stuck/orphaned queue items
     */
    @Query("SELECT q FROM NotificationQueue q WHERE q.status = 'PROCESSING' AND q.processedAt < :cutoff")
    List<NotificationQueue> findStuckItems(@Param("cutoff") LocalDateTime cutoff);

    /**
     * Count queue items by status
     */
    long countByStatus(NotificationQueue.QueueStatus status);

    /**
     * Count queue items by tenant and status
     */
    long countByTenantIdAndStatus(String tenantId, NotificationQueue.QueueStatus status);

    /**
     * Count queue items by channel and status
     */
    long countByChannelAndStatus(NotificationChannel channel, NotificationQueue.QueueStatus status);

    /**
     * Count pending items
     */
    @Query("SELECT COUNT(q) FROM NotificationQueue q WHERE " +
           "(q.status = 'PENDING' OR (q.status = 'RETRY' AND q.nextRetryAt <= :now)) " +
           "AND q.scheduledFor <= :now")
    long countPendingItems(@Param("now") LocalDateTime now);

    /**
     * Count items by priority
     */
    long countByPriorityAndStatus(NotificationPriority priority, NotificationQueue.QueueStatus status);

    /**
     * Find items processed in date range
     */
    @Query("SELECT q FROM NotificationQueue q WHERE q.processedAt BETWEEN :from AND :to")
    List<NotificationQueue> findProcessedBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /**
     * Find items by retry count
     */
    List<NotificationQueue> findByRetryCountGreaterThan(int retryCount);

    /**
     * Find items that exceeded max retries
     */
    @Query("SELECT q FROM NotificationQueue q WHERE q.retryCount >= q.maxRetries AND q.status != 'FAILED'")
    List<NotificationQueue> findItemsExceedingMaxRetries();

    /**
     * Get queue statistics
     */
    @Query("SELECT q.status, COUNT(q) FROM NotificationQueue q GROUP BY q.status")
    List<Object[]> getQueueStatistics();

    /**
     * Get queue statistics by tenant
     */
    @Query("SELECT q.status, COUNT(q) FROM NotificationQueue q WHERE q.tenantId = :tenantId GROUP BY q.status")
    List<Object[]> getQueueStatisticsByTenant(@Param("tenantId") String tenantId);

    /**
     * Get channel statistics
     */
    @Query("SELECT q.channel, q.status, COUNT(q) FROM NotificationQueue q GROUP BY q.channel, q.status")
    List<Object[]> getChannelStatistics();

    /**
     * Get processing performance metrics
     */
    @Query("SELECT AVG(TIMESTAMPDIFF(SECOND, q.scheduledFor, q.processedAt)) as avgProcessingTime, " +
           "MIN(TIMESTAMPDIFF(SECOND, q.scheduledFor, q.processedAt)) as minProcessingTime, " +
           "MAX(TIMESTAMPDIFF(SECOND, q.scheduledFor, q.processedAt)) as maxProcessingTime " +
           "FROM NotificationQueue q WHERE q.processedAt IS NOT NULL AND q.processedAt >= :since")
    Object[] getProcessingMetrics(@Param("since") LocalDateTime since);

    /**
     * Find items for health check
     */
    @Query("SELECT q FROM NotificationQueue q WHERE " +
           "(q.status = 'PROCESSING' AND q.processedAt < :stuckCutoff) OR " +
           "(q.status = 'RETRY' AND q.retryCount >= q.maxRetries)")
    List<NotificationQueue> findItemsNeedingAttention(@Param("stuckCutoff") LocalDateTime stuckCutoff);

    /**
     * Find overdue scheduled items
     */
    @Query("SELECT q FROM NotificationQueue q WHERE q.status = 'PENDING' AND q.scheduledFor < :cutoff")
    List<NotificationQueue> findOverdueItems(@Param("cutoff") LocalDateTime cutoff);

    /**
     * Find items by error pattern
     */
    @Query("SELECT q FROM NotificationQueue q WHERE q.errorMessage LIKE %:pattern%")
    List<NotificationQueue> findByErrorPattern(@Param("pattern") String pattern);

    /**
     * Get retry statistics
     */
    @Query("SELECT q.retryCount, COUNT(q) FROM NotificationQueue q " +
           "WHERE q.status IN ('RETRY', 'FAILED') GROUP BY q.retryCount")
    List<Object[]> getRetryStatistics();

    /**
     * Find oldest pending items
     */
    @Query("SELECT q FROM NotificationQueue q WHERE q.status = 'PENDING' " +
           "ORDER BY q.createdAt ASC")
    List<NotificationQueue> findOldestPendingItems(Pageable pageable);

    /**
     * Find items for tenant cleanup
     */
    @Query("SELECT q FROM NotificationQueue q WHERE q.tenantId = :tenantId AND " +
           "q.status IN ('COMPLETED', 'FAILED', 'CANCELLED') AND q.updatedAt < :cutoff")
    List<NotificationQueue> findCompletedItemsForCleanup(@Param("tenantId") String tenantId, 
                                                        @Param("cutoff") LocalDateTime cutoff);

    /**
     * Delete completed items older than cutoff
     */
    @Query("DELETE FROM NotificationQueue q WHERE " +
           "q.status IN ('COMPLETED', 'FAILED', 'CANCELLED') AND q.updatedAt < :cutoff")
    void deleteCompletedItemsOlderThan(@Param("cutoff") LocalDateTime cutoff);

    /**
     * Reset stuck processing items
     */
    @Query("UPDATE NotificationQueue q SET q.status = 'PENDING', q.processedAt = NULL, q.processingNodeId = NULL " +
           "WHERE q.status = 'PROCESSING' AND q.processedAt < :cutoff")
    int resetStuckItems(@Param("cutoff") LocalDateTime cutoff);

    /**
     * Find items by correlation ID
     */
    @Query("SELECT q FROM NotificationQueue q " +
           "JOIN Notification n ON q.notificationId = n.id " +
           "WHERE n.correlationId = :correlationId")
    List<NotificationQueue> findByCorrelationId(@Param("correlationId") String correlationId);

    /**
     * Find items by campaign ID
     */
    @Query("SELECT q FROM NotificationQueue q " +
           "JOIN Notification n ON q.notificationId = n.id " +
           "WHERE n.campaignId = :campaignId")
    List<NotificationQueue> findByCampaignId(@Param("campaignId") String campaignId);

    /**
     * Get average processing time by channel
     */
    @Query("SELECT q.channel, AVG(TIMESTAMPDIFF(SECOND, q.scheduledFor, q.processedAt)) as avgTime " +
           "FROM NotificationQueue q WHERE q.processedAt IS NOT NULL AND q.processedAt >= :since " +
           "GROUP BY q.channel")
    List<Object[]> getAverageProcessingTimeByChannel(@Param("since") LocalDateTime since);

    /**
     * Find items needing immediate processing
     */
    @Query("SELECT q FROM NotificationQueue q WHERE " +
           "(q.status = 'PENDING' OR (q.status = 'RETRY' AND q.nextRetryAt <= :now)) " +
           "AND q.scheduledFor <= :now AND q.priority = 'URGENT' " +
           "ORDER BY q.scheduledFor ASC")
    List<NotificationQueue> findUrgentItems(@Param("now") LocalDateTime now);
}