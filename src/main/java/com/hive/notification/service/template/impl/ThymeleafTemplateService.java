package com.hive.notification.service.template.impl;

import com.hive.notification.domain.entity.NotificationTemplate;
import com.hive.notification.domain.enums.NotificationChannel;
import com.hive.notification.repository.NotificationTemplateRepository;
import com.hive.notification.service.template.TemplateService;
import com.hive.notification.exception.TemplateNotFoundException;
import com.hive.notification.exception.TemplateRenderException;
import com.hive.notification.exception.TemplateValidationException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.exceptions.TemplateProcessingException;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Thymeleaf-based template service implementation
 */
@Service
@Transactional
public class ThymeleafTemplateService implements TemplateService {

    private final NotificationTemplateRepository templateRepository;
    private final TemplateEngine templateEngine;
    private final TemplateEngine htmlTemplateEngine;
    
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    @Autowired
    public ThymeleafTemplateService(NotificationTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
        this.templateEngine = createTemplateEngine(TemplateMode.TEXT);
        this.htmlTemplateEngine = createTemplateEngine(TemplateMode.HTML);
    }

    private TemplateEngine createTemplateEngine(TemplateMode mode) {
        TemplateEngine engine = new TemplateEngine();
        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode(mode);
        resolver.setCacheable(true);
        engine.setTemplateResolver(resolver);
        return engine;
    }

    @Override
    public NotificationTemplate createTemplate(NotificationTemplate template) {
        validateTemplate(template.getContentTemplate(), template.getHtmlTemplate(), template.getSubjectTemplate());
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());
        return templateRepository.save(template);
    }

    @Override
    public NotificationTemplate updateTemplate(UUID templateId, NotificationTemplate template) {
        NotificationTemplate existing = getTemplate(templateId);
        
        validateTemplate(template.getContentTemplate(), template.getHtmlTemplate(), template.getSubjectTemplate());
        
        existing.setName(template.getName());
        existing.setDescription(template.getDescription());
        existing.setSubjectTemplate(template.getSubjectTemplate());
        existing.setContentTemplate(template.getContentTemplate());
        existing.setHtmlTemplate(template.getHtmlTemplate());
        existing.setDefaultVariables(template.getDefaultVariables());
        existing.setRequiredVariables(template.getRequiredVariables());
        existing.setChannelSpecificConfig(template.getChannelSpecificConfig());
        existing.setMetadata(template.getMetadata());
        existing.setUpdatedAt(LocalDateTime.now());
        
        return templateRepository.save(existing);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationTemplate getTemplate(UUID templateId) {
        return templateRepository.findById(templateId)
            .orElseThrow(() -> new TemplateNotFoundException("Template not found: " + templateId));
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationTemplate getTemplate(String tenantId, String name, String version) {
        return templateRepository.findByTenantIdAndNameAndVersion(tenantId, name, version)
            .orElseThrow(() -> new TemplateNotFoundException(
                String.format("Template not found: %s/%s/%s", tenantId, name, version)));
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationTemplate getLatestTemplate(String tenantId, String name) {
        return templateRepository.findLatestByTenantIdAndName(tenantId, name)
            .orElseThrow(() -> new TemplateNotFoundException(
                String.format("Template not found: %s/%s", tenantId, name)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationTemplate> getTemplates(String tenantId, NotificationChannel channel, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationTemplate> templates;
        
        if (channel != null) {
            templates = templateRepository.findByTenantIdAndChannelAndStatus(
                tenantId, channel, NotificationTemplate.TemplateStatus.APPROVED, pageable);
        } else {
            templates = templateRepository.findByTenantIdAndStatus(
                tenantId, NotificationTemplate.TemplateStatus.APPROVED, pageable);
        }
        
        return templates.getContent();
    }

    @Override
    public void deleteTemplate(UUID templateId) {
        NotificationTemplate template = getTemplate(templateId);
        templateRepository.delete(template);
    }

    @Override
    public void archiveTemplate(UUID templateId) {
        NotificationTemplate template = getTemplate(templateId);
        template.archive();
        templateRepository.save(template);
    }

    @Override
    public void approveTemplate(UUID templateId, String approvedBy, String notes) {
        NotificationTemplate template = getTemplate(templateId);
        template.approve(approvedBy, notes);
        templateRepository.save(template);
    }

    @Override
    @Transactional(readOnly = true)
    public RenderedTemplate renderTemplate(UUID templateId, Map<String, Object> variables) {
        NotificationTemplate template = getTemplate(templateId);
        
        if (!template.isActive()) {
            throw new TemplateRenderException("Template is not active: " + templateId);
        }
        
        // Validate required variables
        if (!template.hasRequiredVariables(variables)) {
            List<String> missing = template.getRequiredVariables().keySet().stream()
                .filter(var -> !variables.containsKey(var))
                .collect(Collectors.toList());
            throw new TemplateRenderException("Missing required variables: " + missing);
        }
        
        // Merge with default variables
        Map<String, Object> mergedVariables = template.getMergedVariables(variables);
        
        return renderTemplate(template.getContentTemplate(), template.getHtmlTemplate(), 
                            template.getSubjectTemplate(), mergedVariables);
    }

    @Override
    public RenderedTemplate renderTemplate(String templateContent, String htmlContent, 
                                         String subjectTemplate, Map<String, Object> variables) {
        try {
            Context context = new Context();
            context.setVariables(variables);
            
            String renderedSubject = subjectTemplate != null ? 
                templateEngine.process(subjectTemplate, context) : null;
            
            String renderedContent = templateContent != null ? 
                templateEngine.process(templateContent, context) : null;
            
            String renderedHtml = htmlContent != null ? 
                htmlTemplateEngine.process(htmlContent, context) : null;
            
            return new RenderedTemplate(renderedSubject, renderedContent, renderedHtml);
            
        } catch (TemplateProcessingException e) {
            throw new TemplateRenderException("Failed to render template: " + e.getMessage(), e);
        }
    }

    @Override
    public ValidationResult validateTemplate(String templateContent, String htmlContent, String subjectTemplate) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        try {
            // Test render with empty variables
            Map<String, Object> emptyVars = new HashMap<>();
            Context context = new Context();
            context.setVariables(emptyVars);
            
            if (subjectTemplate != null && !subjectTemplate.isEmpty()) {
                try {
                    templateEngine.process(subjectTemplate, context);
                } catch (TemplateProcessingException e) {
                    errors.add("Subject template error: " + e.getMessage());
                }
            }
            
            if (templateContent != null && !templateContent.isEmpty()) {
                try {
                    templateEngine.process(templateContent, context);
                } catch (TemplateProcessingException e) {
                    errors.add("Content template error: " + e.getMessage());
                }
            }
            
            if (htmlContent != null && !htmlContent.isEmpty()) {
                try {
                    htmlTemplateEngine.process(htmlContent, context);
                } catch (TemplateProcessingException e) {
                    errors.add("HTML template error: " + e.getMessage());
                }
            }
            
            // Check for undefined variables
            List<String> subjectVars = extractTemplateVariables(subjectTemplate);
            List<String> contentVars = extractTemplateVariables(templateContent);
            List<String> htmlVars = extractTemplateVariables(htmlContent);
            
            Set<String> allVars = new HashSet<>();
            allVars.addAll(subjectVars);
            allVars.addAll(contentVars);
            allVars.addAll(htmlVars);
            
            if (!allVars.isEmpty()) {
                warnings.add("Template uses variables: " + String.join(", ", allVars));
            }
            
        } catch (Exception e) {
            errors.add("Template validation failed: " + e.getMessage());
        }
        
        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getTemplateAnalytics(UUID templateId) {
        NotificationTemplate template = getTemplate(templateId);
        
        Map<String, Object> analytics = new HashMap<>();
        analytics.put("templateId", templateId);
        analytics.put("templateName", template.getName());
        analytics.put("version", template.getVersion());
        analytics.put("channel", template.getChannel());
        analytics.put("status", template.getStatus());
        analytics.put("createdAt", template.getCreatedAt());
        
        // TODO: Add actual usage analytics from notification statistics
        analytics.put("totalSent", 0);
        analytics.put("totalDelivered", 0);
        analytics.put("totalFailed", 0);
        analytics.put("deliveryRate", 0.0);
        analytics.put("engagementRate", 0.0);
        
        return analytics;
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationTemplate selectTemplateForABTest(String tenantId, String templateName, String userId) {
        List<NotificationTemplate> templates = templateRepository
            .findByTenantIdAndNameAndStatus(tenantId, templateName, NotificationTemplate.TemplateStatus.APPROVED);
        
        if (templates.isEmpty()) {
            throw new TemplateNotFoundException("No approved templates found for A/B test: " + templateName);
        }
        
        if (templates.size() == 1) {
            return templates.get(0);
        }
        
        // Simple hash-based A/B test selection
        int hash = (userId + templateName).hashCode();
        int index = Math.abs(hash) % templates.size();
        return templates.get(index);
    }

    @Override
    public List<String> extractTemplateVariables(String templateContent) {
        if (templateContent == null || templateContent.isEmpty()) {
            return new ArrayList<>();
        }
        
        Set<String> variables = new HashSet<>();
        Matcher matcher = VARIABLE_PATTERN.matcher(templateContent);
        
        while (matcher.find()) {
            String variable = matcher.group(1);
            // Clean up variable name (remove any additional processing syntax)
            variable = variable.split("\\?")[0].split("\\:")[0].trim();
            variables.add(variable);
        }
        
        return new ArrayList<>(variables);
    }

    @Override
    public NotificationTemplate cloneTemplate(UUID templateId, String newVersion) {
        NotificationTemplate original = getTemplate(templateId);
        
        NotificationTemplate clone = new NotificationTemplate();
        clone.setTenantId(original.getTenantId());
        clone.setName(original.getName());
        clone.setVersion(newVersion);
        clone.setDescription(original.getDescription() + " (Cloned from " + original.getVersion() + ")");
        clone.setChannel(original.getChannel());
        clone.setSubjectTemplate(original.getSubjectTemplate());
        clone.setContentTemplate(original.getContentTemplate());
        clone.setHtmlTemplate(original.getHtmlTemplate());
        clone.setLocale(original.getLocale());
        clone.setStatus(NotificationTemplate.TemplateStatus.DRAFT);
        clone.setDefaultVariables(new HashMap<>(original.getDefaultVariables()));
        clone.setRequiredVariables(new HashMap<>(original.getRequiredVariables()));
        clone.setChannelSpecificConfig(new HashMap<>(original.getChannelSpecificConfig()));
        clone.setCategory(original.getCategory());
        clone.setTags(original.getTags());
        
        return templateRepository.save(clone);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationTemplate> getTemplateVersions(String tenantId, String templateName) {
        return templateRepository.findByTenantIdAndNameOrderByVersionDesc(tenantId, templateName);
    }
}