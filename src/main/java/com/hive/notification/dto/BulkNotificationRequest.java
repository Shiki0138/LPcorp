package com.hive.notification.dto;

import com.hive.notification.domain.enums.DeliveryStrategy;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Request DTO for bulk notification operations
 */
public class BulkNotificationRequest {

    @NotEmpty(message = "Recipients list cannot be empty")
    private List<@Valid NotificationRecipient> recipients;

    @NotNull(message = "Template request is required")
    private NotificationRequest template;

    private DeliveryStrategy deliveryStrategy = DeliveryStrategy.SINGLE_CHANNEL;

    private String campaignId;

    private Integer batchSize = 100;

    private LocalDateTime scheduledAt;

    private Map<String, Object> globalMetadata;

    private boolean respectRateLimits = true;

    private boolean allowDuplicates = false;

    // Nested class for recipient information
    public static class NotificationRecipient {
        private String recipientId;
        private String recipientContact;
        private Map<String, Object> templateVariables;
        private Map<String, Object> metadata;

        // Constructors
        public NotificationRecipient() {}

        public NotificationRecipient(String recipientId, String recipientContact) {
            this.recipientId = recipientId;
            this.recipientContact = recipientContact;
        }

        // Getters and setters
        public String getRecipientId() { return recipientId; }
        public void setRecipientId(String recipientId) { this.recipientId = recipientId; }

        public String getRecipientContact() { return recipientContact; }
        public void setRecipientContact(String recipientContact) { this.recipientContact = recipientContact; }

        public Map<String, Object> getTemplateVariables() { return templateVariables; }
        public void setTemplateVariables(Map<String, Object> templateVariables) { this.templateVariables = templateVariables; }

        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }

    // Constructors
    public BulkNotificationRequest() {}

    public BulkNotificationRequest(List<NotificationRecipient> recipients, NotificationRequest template) {
        this.recipients = recipients;
        this.template = template;
    }

    // Getters and setters
    public List<NotificationRecipient> getRecipients() { return recipients; }
    public void setRecipients(List<NotificationRecipient> recipients) { this.recipients = recipients; }

    public NotificationRequest getTemplate() { return template; }
    public void setTemplate(NotificationRequest template) { this.template = template; }

    public DeliveryStrategy getDeliveryStrategy() { return deliveryStrategy; }
    public void setDeliveryStrategy(DeliveryStrategy deliveryStrategy) { this.deliveryStrategy = deliveryStrategy; }

    public String getCampaignId() { return campaignId; }
    public void setCampaignId(String campaignId) { this.campaignId = campaignId; }

    public Integer getBatchSize() { return batchSize; }
    public void setBatchSize(Integer batchSize) { this.batchSize = batchSize; }

    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }

    public Map<String, Object> getGlobalMetadata() { return globalMetadata; }
    public void setGlobalMetadata(Map<String, Object> globalMetadata) { this.globalMetadata = globalMetadata; }

    public boolean isRespectRateLimits() { return respectRateLimits; }
    public void setRespectRateLimits(boolean respectRateLimits) { this.respectRateLimits = respectRateLimits; }

    public boolean isAllowDuplicates() { return allowDuplicates; }
    public void setAllowDuplicates(boolean allowDuplicates) { this.allowDuplicates = allowDuplicates; }
}