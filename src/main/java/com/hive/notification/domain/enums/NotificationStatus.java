package com.hive.notification.domain.enums;

/**
 * Status enumeration for notification delivery lifecycle
 */
public enum NotificationStatus {
    QUEUED("queued", "Notification queued for processing"),
    PROCESSING("processing", "Notification is being processed"),
    SENT("sent", "Notification sent successfully"),
    DELIVERED("delivered", "Notification delivered to recipient"),
    READ("read", "Notification read by recipient"),
    FAILED("failed", "Notification failed to deliver"),
    CANCELLED("cancelled", "Notification cancelled"),
    EXPIRED("expired", "Notification expired"),
    BOUNCED("bounced", "Notification bounced back"),
    SUPPRESSED("suppressed", "Notification suppressed due to user preferences");

    private final String code;
    private final String description;

    NotificationStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public boolean isTerminal() {
        return this == DELIVERED || this == READ || this == FAILED || 
               this == CANCELLED || this == EXPIRED || this == BOUNCED || this == SUPPRESSED;
    }

    public boolean isSuccess() {
        return this == SENT || this == DELIVERED || this == READ;
    }

    public boolean isFailure() {
        return this == FAILED || this == BOUNCED || this == EXPIRED;
    }
}