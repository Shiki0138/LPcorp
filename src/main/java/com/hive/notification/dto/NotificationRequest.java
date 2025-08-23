package com.hive.notification.dto;

import com.hive.notification.domain.enums.NotificationChannel;
import com.hive.notification.domain.enums.NotificationPriority;
import com.hive.notification.domain.enums.DeliveryStrategy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Request DTO for sending notifications
 */
public class NotificationRequest {

    @NotBlank(message = "Tenant ID is required")
    private String tenantId;

    @NotBlank(message = "Recipient ID is required")
    private String recipientId;

    @NotBlank(message = "Recipient contact is required")
    private String recipientContact;

    @NotNull(message = "Channel is required")
    private NotificationChannel channel;

    private NotificationPriority priority = NotificationPriority.NORMAL;

    private DeliveryStrategy deliveryStrategy = DeliveryStrategy.SINGLE_CHANNEL;

    @NotBlank(message = "Subject is required")
    private String subject;

    @NotBlank(message = "Content is required")
    private String content;

    private String htmlContent;

    private String templateId;

    private String templateVersion;

    private Map<String, Object> templateVariables = new HashMap<>();

    private Map<String, Object> metadata = new HashMap<>();

    private Map<String, Object> channelConfig = new HashMap<>();

    private String category = "general";

    private String campaignId;

    private String correlationId;

    private LocalDateTime scheduledAt;

    private LocalDateTime expiresAt;

    private Integer maxRetries = 3;

    private boolean trackDelivery = true;

    private boolean trackReads = true;

    private boolean respectUserPreferences = true;

    // Constructors
    public NotificationRequest() {}

    public NotificationRequest(String tenantId, String recipientId, String recipientContact,
                              NotificationChannel channel, String subject, String content) {
        this.tenantId = tenantId;
        this.recipientId = recipientId;
        this.recipientContact = recipientContact;
        this.channel = channel;
        this.subject = subject;
        this.content = content;
    }

    // Builder pattern
    public static NotificationRequest builder() {
        return new NotificationRequest();
    }

    public NotificationRequest tenantId(String tenantId) {
        this.tenantId = tenantId;
        return this;
    }

    public NotificationRequest recipientId(String recipientId) {
        this.recipientId = recipientId;
        return this;
    }

    public NotificationRequest recipientContact(String recipientContact) {
        this.recipientContact = recipientContact;
        return this;
    }

    public NotificationRequest channel(NotificationChannel channel) {
        this.channel = channel;
        return this;
    }

    public NotificationRequest priority(NotificationPriority priority) {
        this.priority = priority;
        return this;
    }

    public NotificationRequest deliveryStrategy(DeliveryStrategy deliveryStrategy) {
        this.deliveryStrategy = deliveryStrategy;
        return this;
    }

    public NotificationRequest subject(String subject) {
        this.subject = subject;
        return this;
    }

    public NotificationRequest content(String content) {
        this.content = content;
        return this;
    }

    public NotificationRequest htmlContent(String htmlContent) {
        this.htmlContent = htmlContent;
        return this;
    }

    public NotificationRequest template(String templateId, String templateVersion) {
        this.templateId = templateId;
        this.templateVersion = templateVersion;
        return this;
    }

    public NotificationRequest templateVariable(String key, Object value) {
        this.templateVariables.put(key, value);
        return this;
    }

    public NotificationRequest metadata(String key, Object value) {
        this.metadata.put(key, value);
        return this;
    }

    public NotificationRequest channelConfig(String key, Object value) {
        this.channelConfig.put(key, value);
        return this;
    }

    public NotificationRequest category(String category) {
        this.category = category;
        return this;
    }

    public NotificationRequest campaignId(String campaignId) {
        this.campaignId = campaignId;
        return this;
    }

    public NotificationRequest scheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
        return this;
    }

    public NotificationRequest expiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
        return this;
    }

    // Getters and setters
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getRecipientId() { return recipientId; }
    public void setRecipientId(String recipientId) { this.recipientId = recipientId; }

    public String getRecipientContact() { return recipientContact; }
    public void setRecipientContact(String recipientContact) { this.recipientContact = recipientContact; }

    public NotificationChannel getChannel() { return channel; }
    public void setChannel(NotificationChannel channel) { this.channel = channel; }

    public NotificationPriority getPriority() { return priority; }
    public void setPriority(NotificationPriority priority) { this.priority = priority; }

    public DeliveryStrategy getDeliveryStrategy() { return deliveryStrategy; }
    public void setDeliveryStrategy(DeliveryStrategy deliveryStrategy) { this.deliveryStrategy = deliveryStrategy; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getHtmlContent() { return htmlContent; }
    public void setHtmlContent(String htmlContent) { this.htmlContent = htmlContent; }

    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }

    public String getTemplateVersion() { return templateVersion; }
    public void setTemplateVersion(String templateVersion) { this.templateVersion = templateVersion; }

    public Map<String, Object> getTemplateVariables() { return templateVariables; }
    public void setTemplateVariables(Map<String, Object> templateVariables) { this.templateVariables = templateVariables; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public Map<String, Object> getChannelConfig() { return channelConfig; }
    public void setChannelConfig(Map<String, Object> channelConfig) { this.channelConfig = channelConfig; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getCampaignId() { return campaignId; }
    public void setCampaignId(String campaignId) { this.campaignId = campaignId; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public Integer getMaxRetries() { return maxRetries; }
    public void setMaxRetries(Integer maxRetries) { this.maxRetries = maxRetries; }

    public boolean isTrackDelivery() { return trackDelivery; }
    public void setTrackDelivery(boolean trackDelivery) { this.trackDelivery = trackDelivery; }

    public boolean isTrackReads() { return trackReads; }
    public void setTrackReads(boolean trackReads) { this.trackReads = trackReads; }

    public boolean isRespectUserPreferences() { return respectUserPreferences; }
    public void setRespectUserPreferences(boolean respectUserPreferences) { this.respectUserPreferences = respectUserPreferences; }
}