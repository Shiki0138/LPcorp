package com.hive.notification.dto;

import com.hive.notification.domain.enums.NotificationChannel;
import com.hive.notification.domain.enums.NotificationStatus;
import com.hive.notification.domain.enums.NotificationPriority;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for notification operations
 */
public class NotificationResponse {

    private UUID notificationId;
    private String tenantId;
    private String recipientId;
    private NotificationChannel channel;
    private NotificationStatus status;
    private NotificationPriority priority;
    private String subject;
    private LocalDateTime createdAt;
    private LocalDateTime scheduledAt;
    private LocalDateTime sentAt;
    private String errorMessage;
    private Map<String, Object> deliveryTracking;
    private Map<String, Object> metadata;
    private boolean success;
    private String message;

    // Constructors
    public NotificationResponse() {}

    public NotificationResponse(UUID notificationId, NotificationStatus status, boolean success, String message) {
        this.notificationId = notificationId;
        this.status = status;
        this.success = success;
        this.message = message;
    }

    // Static factory methods
    public static NotificationResponse success(UUID notificationId, String message) {
        return new NotificationResponse(notificationId, NotificationStatus.QUEUED, true, message);
    }

    public static NotificationResponse failure(String message) {
        return new NotificationResponse(null, NotificationStatus.FAILED, false, message);
    }

    public static NotificationResponse sent(UUID notificationId, String message) {
        return new NotificationResponse(notificationId, NotificationStatus.SENT, true, message);
    }

    // Getters and setters
    public UUID getNotificationId() { return notificationId; }
    public void setNotificationId(UUID notificationId) { this.notificationId = notificationId; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getRecipientId() { return recipientId; }
    public void setRecipientId(String recipientId) { this.recipientId = recipientId; }

    public NotificationChannel getChannel() { return channel; }
    public void setChannel(NotificationChannel channel) { this.channel = channel; }

    public NotificationStatus getStatus() { return status; }
    public void setStatus(NotificationStatus status) { this.status = status; }

    public NotificationPriority getPriority() { return priority; }
    public void setPriority(NotificationPriority priority) { this.priority = priority; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Map<String, Object> getDeliveryTracking() { return deliveryTracking; }
    public void setDeliveryTracking(Map<String, Object> deliveryTracking) { this.deliveryTracking = deliveryTracking; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}