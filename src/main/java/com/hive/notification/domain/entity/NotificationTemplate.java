package com.hive.notification.domain.entity;

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
import java.util.Set;
import java.util.UUID;

/**
 * Notification template entity for managing reusable notification templates
 */
@Entity
@Table(name = "notification_templates", indexes = {
    @Index(name = "idx_template_name", columnList = "name"),
    @Index(name = "idx_template_channel", columnList = "channel"),
    @Index(name = "idx_template_status", columnList = "status"),
    @Index(name = "idx_template_tenant", columnList = "tenantId")
})
@EntityListeners(AuditingEntityListener.class)
public class NotificationTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String version = "1.0";

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    @Column(nullable = false)
    private String subjectTemplate;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contentTemplate;

    @Column(columnDefinition = "TEXT")
    private String htmlTemplate;

    @Column(nullable = false)
    private String locale = "en";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TemplateStatus status = TemplateStatus.DRAFT;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> defaultVariables = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, String> requiredVariables = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> channelSpecificConfig = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> metadata = new HashMap<>();

    private String category;
    private String tags;

    private String approvedBy;
    private LocalDateTime approvedAt;
    private String approvalNotes;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version_;

    // Template status enumeration
    public enum TemplateStatus {
        DRAFT, PENDING_APPROVAL, APPROVED, ARCHIVED
    }

    // Constructors
    public NotificationTemplate() {}

    public NotificationTemplate(String tenantId, String name, String description, 
                               NotificationChannel channel, String subjectTemplate, String contentTemplate) {
        this.tenantId = tenantId;
        this.name = name;
        this.description = description;
        this.channel = channel;
        this.subjectTemplate = subjectTemplate;
        this.contentTemplate = contentTemplate;
    }

    // Business methods
    public boolean isActive() {
        return status == TemplateStatus.APPROVED;
    }

    public void approve(String approvedBy, String notes) {
        this.status = TemplateStatus.APPROVED;
        this.approvedBy = approvedBy;
        this.approvedAt = LocalDateTime.now();
        this.approvalNotes = notes;
    }

    public void archive() {
        this.status = TemplateStatus.ARCHIVED;
    }

    public void addRequiredVariable(String variableName, String description) {
        this.requiredVariables.put(variableName, description);
    }

    public void addDefaultVariable(String variableName, Object defaultValue) {
        this.defaultVariables.put(variableName, defaultValue);
    }

    public void addChannelConfig(String key, Object value) {
        this.channelSpecificConfig.put(key, value);
    }

    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }

    // Validation methods
    public boolean hasRequiredVariables(Map<String, Object> variables) {
        if (requiredVariables == null || requiredVariables.isEmpty()) {
            return true;
        }
        
        return requiredVariables.keySet().stream()
            .allMatch(variables::containsKey);
    }

    public Map<String, Object> getMergedVariables(Map<String, Object> providedVariables) {
        Map<String, Object> merged = new HashMap<>(defaultVariables);
        if (providedVariables != null) {
            merged.putAll(providedVariables);
        }
        return merged;
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public NotificationChannel getChannel() { return channel; }
    public void setChannel(NotificationChannel channel) { this.channel = channel; }

    public String getSubjectTemplate() { return subjectTemplate; }
    public void setSubjectTemplate(String subjectTemplate) { this.subjectTemplate = subjectTemplate; }

    public String getContentTemplate() { return contentTemplate; }
    public void setContentTemplate(String contentTemplate) { this.contentTemplate = contentTemplate; }

    public String getHtmlTemplate() { return htmlTemplate; }
    public void setHtmlTemplate(String htmlTemplate) { this.htmlTemplate = htmlTemplate; }

    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }

    public TemplateStatus getStatus() { return status; }
    public void setStatus(TemplateStatus status) { this.status = status; }

    public Map<String, Object> getDefaultVariables() { return defaultVariables; }
    public void setDefaultVariables(Map<String, Object> defaultVariables) { this.defaultVariables = defaultVariables; }

    public Map<String, String> getRequiredVariables() { return requiredVariables; }
    public void setRequiredVariables(Map<String, String> requiredVariables) { this.requiredVariables = requiredVariables; }

    public Map<String, Object> getChannelSpecificConfig() { return channelSpecificConfig; }
    public void setChannelSpecificConfig(Map<String, Object> channelSpecificConfig) { this.channelSpecificConfig = channelSpecificConfig; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public String getApprovalNotes() { return approvalNotes; }
    public void setApprovalNotes(String approvalNotes) { this.approvalNotes = approvalNotes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Long getVersion_() { return version_; }
    public void setVersion_(Long version_) { this.version_ = version_; }
}