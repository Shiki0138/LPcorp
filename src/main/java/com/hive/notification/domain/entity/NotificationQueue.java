package com.hive.notification.domain.entity;

import com.hive.notification.domain.enums.NotificationPriority;
import com.hive.notification.domain.enums.NotificationChannel;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Notification queue entity for managing notification processing
 */
@Entity
@Table(name = "notification_queue", indexes = {
    @Index(name = "idx_queue_priority", columnList = "priority"),
    @Index(name = "idx_queue_scheduled", columnList = "scheduledFor"),
    @Index(name = "idx_queue_status", columnList = "status"),
    @Index(name = "idx_queue_tenant", columnList = "tenantId")
})
@EntityListeners(AuditingEntityListener.class)
public class NotificationQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID notificationId;

    @Column(nullable = false)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationPriority priority = NotificationPriority.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QueueStatus status = QueueStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime scheduledFor = LocalDateTime.now();

    private LocalDateTime processedAt;
    private LocalDateTime nextRetryAt;

    private Integer retryCount = 0;
    private Integer maxRetries = 3;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> processingContext = new HashMap<>();

    private String processingNodeId; // For distributed processing

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    // Queue status enumeration
    public enum QueueStatus {
        PENDING("pending", "Waiting to be processed"),
        PROCESSING("processing", "Currently being processed"),
        COMPLETED("completed", "Successfully processed"),
        FAILED("failed", "Processing failed"),
        RETRY("retry", "Scheduled for retry"),
        CANCELLED("cancelled", "Processing cancelled");

        private final String code;
        private final String description;

        QueueStatus(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() { return code; }
        public String getDescription() { return description; }

        public boolean isTerminal() {
            return this == COMPLETED || this == FAILED || this == CANCELLED;
        }
    }

    // Constructors
    public NotificationQueue() {}

    public NotificationQueue(UUID notificationId, String tenantId, NotificationChannel channel, 
                           NotificationPriority priority, LocalDateTime scheduledFor) {
        this.notificationId = notificationId;
        this.tenantId = tenantId;
        this.channel = channel;
        this.priority = priority;
        this.scheduledFor = scheduledFor;
    }

    // Business methods
    public void markAsProcessing(String nodeId) {
        this.status = QueueStatus.PROCESSING;
        this.processedAt = LocalDateTime.now();
        this.processingNodeId = nodeId;
    }

    public void markAsCompleted() {
        this.status = QueueStatus.COMPLETED;
        this.processedAt = LocalDateTime.now();
    }

    public void markAsFailed(String errorMessage, boolean scheduleRetry) {
        this.errorMessage = errorMessage;
        
        if (scheduleRetry && canRetry()) {
            this.status = QueueStatus.RETRY;
            this.retryCount++;
            this.nextRetryAt = calculateNextRetry();
        } else {
            this.status = QueueStatus.FAILED;
        }
    }

    public void markAsCancelled() {
        this.status = QueueStatus.CANCELLED;
    }

    public boolean canRetry() {
        return retryCount < maxRetries;
    }

    public boolean isReadyForProcessing() {
        return status == QueueStatus.PENDING || 
               (status == QueueStatus.RETRY && nextRetryAt != null && nextRetryAt.isBefore(LocalDateTime.now()));
    }

    public boolean isHighPriority() {
        return priority == NotificationPriority.HIGH || priority == NotificationPriority.URGENT;
    }

    private LocalDateTime calculateNextRetry() {
        // Exponential backoff: 1min, 5min, 15min
        int delayMinutes = switch (retryCount) {
            case 1 -> 1;
            case 2 -> 5;
            default -> 15;
        };
        return LocalDateTime.now().plusMinutes(delayMinutes);
    }

    public void addProcessingContext(String key, Object value) {
        this.processingContext.put(key, value);
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getNotificationId() { return notificationId; }
    public void setNotificationId(UUID notificationId) { this.notificationId = notificationId; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public NotificationPriority getPriority() { return priority; }
    public void setPriority(NotificationPriority priority) { this.priority = priority; }

    public NotificationChannel getChannel() { return channel; }
    public void setChannel(NotificationChannel channel) { this.channel = channel; }

    public QueueStatus getStatus() { return status; }
    public void setStatus(QueueStatus status) { this.status = status; }

    public LocalDateTime getScheduledFor() { return scheduledFor; }
    public void setScheduledFor(LocalDateTime scheduledFor) { this.scheduledFor = scheduledFor; }

    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }

    public LocalDateTime getNextRetryAt() { return nextRetryAt; }
    public void setNextRetryAt(LocalDateTime nextRetryAt) { this.nextRetryAt = nextRetryAt; }

    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }

    public Integer getMaxRetries() { return maxRetries; }
    public void setMaxRetries(Integer maxRetries) { this.maxRetries = maxRetries; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Map<String, Object> getProcessingContext() { return processingContext; }
    public void setProcessingContext(Map<String, Object> processingContext) { this.processingContext = processingContext; }

    public String getProcessingNodeId() { return processingNodeId; }
    public void setProcessingNodeId(String processingNodeId) { this.processingNodeId = processingNodeId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}