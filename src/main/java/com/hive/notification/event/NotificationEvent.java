package com.hive.notification.event;

import com.hive.notification.domain.entity.Notification;
import com.hive.notification.domain.enums.NotificationStatus;

import java.time.LocalDateTime;

/**
 * Base class for notification events
 */
public abstract class NotificationEvent {
    
    protected final Notification notification;
    protected final LocalDateTime timestamp;

    protected NotificationEvent(Notification notification) {
        this.notification = notification;
        this.timestamp = LocalDateTime.now();
    }

    public Notification getNotification() {
        return notification;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Event fired when a notification is created
     */
    public static class Created extends NotificationEvent {
        public Created(Notification notification) {
            super(notification);
        }
    }

    /**
     * Event fired when a notification is sent
     */
    public static class Sent extends NotificationEvent {
        private final String externalId;

        public Sent(Notification notification, String externalId) {
            super(notification);
            this.externalId = externalId;
        }

        public String getExternalId() {
            return externalId;
        }
    }

    /**
     * Event fired when a notification is delivered
     */
    public static class Delivered extends NotificationEvent {
        public Delivered(Notification notification) {
            super(notification);
        }
    }

    /**
     * Event fired when a notification is read
     */
    public static class Read extends NotificationEvent {
        public Read(Notification notification) {
            super(notification);
        }
    }

    /**
     * Event fired when a notification fails
     */
    public static class Failed extends NotificationEvent {
        private final String errorMessage;
        private final Exception cause;

        public Failed(Notification notification, String errorMessage) {
            this(notification, errorMessage, null);
        }

        public Failed(Notification notification, String errorMessage, Exception cause) {
            super(notification);
            this.errorMessage = errorMessage;
            this.cause = cause;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public Exception getCause() {
            return cause;
        }
    }

    /**
     * Event fired when a notification is cancelled
     */
    public static class Cancelled extends NotificationEvent {
        private final String reason;

        public Cancelled(Notification notification, String reason) {
            super(notification);
            this.reason = reason;
        }

        public String getReason() {
            return reason;
        }
    }

    /**
     * Event fired when a notification status is updated
     */
    public static class StatusUpdated extends NotificationEvent {
        private final NotificationStatus previousStatus;
        private final NotificationStatus newStatus;

        public StatusUpdated(Notification notification, NotificationStatus previousStatus) {
            super(notification);
            this.previousStatus = previousStatus;
            this.newStatus = notification.getStatus();
        }

        public NotificationStatus getPreviousStatus() {
            return previousStatus;
        }

        public NotificationStatus getNewStatus() {
            return newStatus;
        }
    }

    /**
     * Event fired when a notification is retried
     */
    public static class Retried extends NotificationEvent {
        private final int attemptNumber;

        public Retried(Notification notification, int attemptNumber) {
            super(notification);
            this.attemptNumber = attemptNumber;
        }

        public int getAttemptNumber() {
            return attemptNumber;
        }
    }

    /**
     * Event fired when a notification expires
     */
    public static class Expired extends NotificationEvent {
        public Expired(Notification notification) {
            super(notification);
        }
    }
}