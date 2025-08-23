package com.hive.notification.exception;

/**
 * Base exception for notification-related errors
 */
public class NotificationException extends RuntimeException {
    
    public NotificationException(String message) {
        super(message);
    }
    
    public NotificationException(String message, Throwable cause) {
        super(message, cause);
    }
}

/**
 * Exception thrown when notification is not found
 */
class NotificationNotFoundException extends NotificationException {
    public NotificationNotFoundException(String message) {
        super(message);
    }
}

/**
 * Exception thrown when notification validation fails
 */
class NotificationValidationException extends NotificationException {
    public NotificationValidationException(String message) {
        super(message);
    }
}

/**
 * Exception thrown when notification delivery fails
 */
class NotificationDeliveryException extends NotificationException {
    public NotificationDeliveryException(String message) {
        super(message);
    }
    
    public NotificationDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}

/**
 * Exception thrown when template operations fail
 */
class TemplateNotFoundException extends NotificationException {
    public TemplateNotFoundException(String message) {
        super(message);
    }
}

/**
 * Exception thrown when template rendering fails
 */
class TemplateRenderException extends NotificationException {
    public TemplateRenderException(String message) {
        super(message);
    }
    
    public TemplateRenderException(String message, Throwable cause) {
        super(message, cause);
    }
}

/**
 * Exception thrown when template validation fails
 */
class TemplateValidationException extends NotificationException {
    public TemplateValidationException(String message) {
        super(message);
    }
}