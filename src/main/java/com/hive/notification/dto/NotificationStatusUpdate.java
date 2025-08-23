package com.hive.notification.dto;

import com.hive.notification.domain.enums.NotificationStatus;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for notification status updates from external providers
 */
public class NotificationStatusUpdate {

    private NotificationStatus status;
    private String externalId;
    private LocalDateTime timestamp;
    private String reason;
    private String errorMessage;
    private Map<String, Object> providerData;
    private String providerName;

    // Constructors
    public NotificationStatusUpdate() {}

    public NotificationStatusUpdate(NotificationStatus status, String externalId, LocalDateTime timestamp) {
        this.status = status;
        this.externalId = externalId;
        this.timestamp = timestamp;
    }

    // Getters and setters
    public NotificationStatus getStatus() { return status; }
    public void setStatus(NotificationStatus status) { this.status = status; }

    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Map<String, Object> getProviderData() { return providerData; }
    public void setProviderData(Map<String, Object> providerData) { this.providerData = providerData; }

    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }
}