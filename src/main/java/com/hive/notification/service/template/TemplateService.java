package com.hive.notification.service.template;

import com.hive.notification.domain.entity.NotificationTemplate;
import com.hive.notification.domain.enums.NotificationChannel;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service interface for managing notification templates
 */
public interface TemplateService {

    /**
     * Create a new notification template
     */
    NotificationTemplate createTemplate(NotificationTemplate template);

    /**
     * Update an existing template
     */
    NotificationTemplate updateTemplate(UUID templateId, NotificationTemplate template);

    /**
     * Get template by ID
     */
    NotificationTemplate getTemplate(UUID templateId);

    /**
     * Get template by name and version
     */
    NotificationTemplate getTemplate(String tenantId, String name, String version);

    /**
     * Get latest version of template by name
     */
    NotificationTemplate getLatestTemplate(String tenantId, String name);

    /**
     * Get all templates for a tenant
     */
    List<NotificationTemplate> getTemplates(String tenantId, NotificationChannel channel, int page, int size);

    /**
     * Delete a template
     */
    void deleteTemplate(UUID templateId);

    /**
     * Archive a template
     */
    void archiveTemplate(UUID templateId);

    /**
     * Approve a template
     */
    void approveTemplate(UUID templateId, String approvedBy, String notes);

    /**
     * Render template with variables
     */
    RenderedTemplate renderTemplate(UUID templateId, Map<String, Object> variables);

    /**
     * Render template content with variables
     */
    RenderedTemplate renderTemplate(String templateContent, String htmlContent, 
                                   String subjectTemplate, Map<String, Object> variables);

    /**
     * Validate template syntax
     */
    ValidationResult validateTemplate(String templateContent, String htmlContent, String subjectTemplate);

    /**
     * Get template performance analytics
     */
    Map<String, Object> getTemplateAnalytics(UUID templateId);

    /**
     * A/B test templates
     */
    NotificationTemplate selectTemplateForABTest(String tenantId, String templateName, String userId);

    /**
     * Get template variables from content
     */
    List<String> extractTemplateVariables(String templateContent);

    /**
     * Clone template with new version
     */
    NotificationTemplate cloneTemplate(UUID templateId, String newVersion);

    /**
     * Get template history/versions
     */
    List<NotificationTemplate> getTemplateVersions(String tenantId, String templateName);

    /**
     * Result classes
     */
    class RenderedTemplate {
        private final String subject;
        private final String content;
        private final String htmlContent;

        public RenderedTemplate(String subject, String content, String htmlContent) {
            this.subject = subject;
            this.content = content;
            this.htmlContent = htmlContent;
        }

        public String getSubject() { return subject; }
        public String getContent() { return content; }
        public String getHtmlContent() { return htmlContent; }
    }

    class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private final List<String> warnings;

        public ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
            this.valid = valid;
            this.errors = errors;
            this.warnings = warnings;
        }

        public boolean isValid() { return valid; }
        public List<String> getErrors() { return errors; }
        public List<String> getWarnings() { return warnings; }
    }
}