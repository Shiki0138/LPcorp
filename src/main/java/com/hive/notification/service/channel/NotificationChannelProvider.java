package com.hive.notification.service.channel;

import com.hive.notification.domain.entity.Notification;
import com.hive.notification.domain.enums.NotificationChannel;

/**
 * Interface for channel-specific notification providers
 */
public interface NotificationChannelProvider {

    /**
     * Get the supported channel for this provider
     */
    NotificationChannel getSupportedChannel();

    /**
     * Send notification through this channel
     */
    ChannelDeliveryResult sendNotification(Notification notification);

    /**
     * Check if the provider can handle this notification
     */
    boolean canHandle(Notification notification);

    /**
     * Validate notification for this channel
     */
    void validateNotification(Notification notification);

    /**
     * Get channel-specific configuration requirements
     */
    ChannelConfigRequirements getConfigRequirements();

    /**
     * Check provider health/availability
     */
    boolean isHealthy();

    /**
     * Get provider metrics
     */
    ChannelMetrics getMetrics();

    /**
     * Result class for delivery operations
     */
    class ChannelDeliveryResult {
        private final boolean success;
        private final String externalId;
        private final String message;
        private final Exception error;

        public ChannelDeliveryResult(boolean success, String externalId, String message) {
            this(success, externalId, message, null);
        }

        public ChannelDeliveryResult(boolean success, String externalId, String message, Exception error) {
            this.success = success;
            this.externalId = externalId;
            this.message = message;
            this.error = error;
        }

        public boolean isSuccess() { return success; }
        public String getExternalId() { return externalId; }
        public String getMessage() { return message; }
        public Exception getError() { return error; }
    }

    /**
     * Channel configuration requirements
     */
    class ChannelConfigRequirements {
        private final java.util.List<String> requiredFields;
        private final java.util.List<String> optionalFields;
        private final java.util.Map<String, String> fieldDescriptions;

        public ChannelConfigRequirements(java.util.List<String> requiredFields, 
                                       java.util.List<String> optionalFields,
                                       java.util.Map<String, String> fieldDescriptions) {
            this.requiredFields = requiredFields;
            this.optionalFields = optionalFields;
            this.fieldDescriptions = fieldDescriptions;
        }

        public java.util.List<String> getRequiredFields() { return requiredFields; }
        public java.util.List<String> getOptionalFields() { return optionalFields; }
        public java.util.Map<String, String> getFieldDescriptions() { return fieldDescriptions; }
    }

    /**
     * Channel provider metrics
     */
    class ChannelMetrics {
        private final long totalSent;
        private final long totalDelivered;
        private final long totalFailed;
        private final double deliveryRate;
        private final double averageResponseTime;

        public ChannelMetrics(long totalSent, long totalDelivered, long totalFailed, 
                            double deliveryRate, double averageResponseTime) {
            this.totalSent = totalSent;
            this.totalDelivered = totalDelivered;
            this.totalFailed = totalFailed;
            this.deliveryRate = deliveryRate;
            this.averageResponseTime = averageResponseTime;
        }

        public long getTotalSent() { return totalSent; }
        public long getTotalDelivered() { return totalDelivered; }
        public long getTotalFailed() { return totalFailed; }
        public double getDeliveryRate() { return deliveryRate; }
        public double getAverageResponseTime() { return averageResponseTime; }
    }
}