package com.hive.notification.service.impl;

import com.hive.notification.domain.entity.Notification;
import com.hive.notification.domain.entity.NotificationQueue;
import com.hive.notification.domain.entity.NotificationTemplate;
import com.hive.notification.domain.entity.NotificationPreference;
import com.hive.notification.domain.enums.NotificationChannel;
import com.hive.notification.domain.enums.NotificationPriority;
import com.hive.notification.domain.enums.NotificationStatus;
import com.hive.notification.domain.enums.DeliveryStrategy;
import com.hive.notification.dto.*;
import com.hive.notification.repository.*;
import com.hive.notification.service.NotificationService;
import com.hive.notification.service.channel.NotificationChannelProvider;
import com.hive.notification.service.template.TemplateService;
import com.hive.notification.service.preference.PreferenceService;
import com.hive.notification.service.delivery.DeliveryOptimizationService;
import com.hive.notification.event.NotificationEvent;
import com.hive.notification.exception.NotificationNotFoundException;
import com.hive.notification.exception.NotificationValidationException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Main notification service implementation
 */
@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;
    private final NotificationQueueRepository queueRepository;
    private final NotificationTemplateRepository templateRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    
    private final List<NotificationChannelProvider> channelProviders;
    private final TemplateService templateService;
    private final PreferenceService preferenceService;
    private final DeliveryOptimizationService deliveryOptimizationService;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            NotificationQueueRepository queueRepository,
            NotificationTemplateRepository templateRepository,
            NotificationPreferenceRepository preferenceRepository,
            List<NotificationChannelProvider> channelProviders,
            TemplateService templateService,
            PreferenceService preferenceService,
            DeliveryOptimizationService deliveryOptimizationService,
            ApplicationEventPublisher eventPublisher) {
        
        this.notificationRepository = notificationRepository;
        this.queueRepository = queueRepository;
        this.templateRepository = templateRepository;
        this.preferenceRepository = preferenceRepository;
        this.channelProviders = channelProviders;
        this.templateService = templateService;
        this.preferenceService = preferenceService;
        this.deliveryOptimizationService = deliveryOptimizationService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public NotificationResponse sendNotification(NotificationRequest request) {
        try {
            validateNotificationRequest(request);
            
            // Check user preferences if required
            if (request.isRespectUserPreferences()) {
                if (!isNotificationAllowed(request.getTenantId(), request.getRecipientId(), 
                                         request.getCategory(), request.getChannel())) {
                    return NotificationResponse.failure("Notification blocked by user preferences");
                }
            }
            
            // Create notification entity
            Notification notification = createNotificationFromRequest(request);
            
            // Save notification
            notification = notificationRepository.save(notification);
            
            // Handle immediate vs scheduled delivery
            if (notification.isScheduled()) {
                scheduleNotificationDelivery(notification);
            } else {
                queueNotificationForDelivery(notification);
            }
            
            // Publish event
            eventPublisher.publishEvent(new NotificationEvent.Created(notification));
            
            logger.info("Notification created successfully: {}", notification.getId());
            
            return createResponseFromNotification(notification, "Notification created successfully");
            
        } catch (Exception e) {
            logger.error("Failed to send notification: {}", e.getMessage(), e);
            return NotificationResponse.failure("Failed to send notification: " + e.getMessage());
        }
    }

    @Override
    public List<NotificationResponse> sendBulkNotifications(BulkNotificationRequest request) {
        List<NotificationResponse> responses = new ArrayList<>();
        
        try {
            validateNotificationRequest(request.getTemplate());
            
            // Process recipients in batches
            List<BulkNotificationRequest.NotificationRecipient> recipients = request.getRecipients();
            int batchSize = request.getBatchSize();
            
            for (int i = 0; i < recipients.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, recipients.size());
                List<BulkNotificationRequest.NotificationRecipient> batch = recipients.subList(i, endIndex);
                
                List<NotificationResponse> batchResponses = processBatch(batch, request);
                responses.addAll(batchResponses);
                
                // Add delay between batches if rate limiting is enabled
                if (request.isRespectRateLimits() && endIndex < recipients.size()) {
                    try {
                        Thread.sleep(100); // 100ms delay between batches
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            
            logger.info("Bulk notification processing completed: {} notifications", responses.size());
            
        } catch (Exception e) {
            logger.error("Failed to send bulk notifications: {}", e.getMessage(), e);
            responses.add(NotificationResponse.failure("Failed to send bulk notifications: " + e.getMessage()));
        }
        
        return responses;
    }

    private List<NotificationResponse> processBatch(
            List<BulkNotificationRequest.NotificationRecipient> batch, 
            BulkNotificationRequest request) {
        
        return batch.stream().map(recipient -> {
            try {
                NotificationRequest individualRequest = createIndividualRequest(recipient, request);
                return sendNotification(individualRequest);
            } catch (Exception e) {
                logger.error("Failed to process bulk notification for recipient {}: {}", 
                           recipient.getRecipientId(), e.getMessage());
                return NotificationResponse.failure("Failed to process notification: " + e.getMessage());
            }
        }).collect(Collectors.toList());
    }

    private NotificationRequest createIndividualRequest(
            BulkNotificationRequest.NotificationRecipient recipient, 
            BulkNotificationRequest request) {
        
        NotificationRequest individualRequest = new NotificationRequest();
        NotificationRequest template = request.getTemplate();
        
        // Copy template properties
        individualRequest.setTenantId(template.getTenantId());
        individualRequest.setChannel(template.getChannel());
        individualRequest.setPriority(template.getPriority());
        individualRequest.setDeliveryStrategy(request.getDeliveryStrategy());
        individualRequest.setSubject(template.getSubject());
        individualRequest.setContent(template.getContent());
        individualRequest.setHtmlContent(template.getHtmlContent());
        individualRequest.setTemplateId(template.getTemplateId());
        individualRequest.setTemplateVersion(template.getTemplateVersion());
        individualRequest.setCategory(template.getCategory());
        individualRequest.setCampaignId(request.getCampaignId());
        individualRequest.setScheduledAt(request.getScheduledAt());
        individualRequest.setChannelConfig(template.getChannelConfig());
        
        // Set recipient-specific properties
        individualRequest.setRecipientId(recipient.getRecipientId());
        individualRequest.setRecipientContact(recipient.getRecipientContact());
        
        // Merge template variables
        Map<String, Object> templateVariables = new HashMap<>();
        if (template.getTemplateVariables() != null) {
            templateVariables.putAll(template.getTemplateVariables());
        }
        if (recipient.getTemplateVariables() != null) {
            templateVariables.putAll(recipient.getTemplateVariables());
        }
        individualRequest.setTemplateVariables(templateVariables);
        
        // Merge metadata
        Map<String, Object> metadata = new HashMap<>();
        if (template.getMetadata() != null) {
            metadata.putAll(template.getMetadata());
        }
        if (request.getGlobalMetadata() != null) {
            metadata.putAll(request.getGlobalMetadata());
        }
        if (recipient.getMetadata() != null) {
            metadata.putAll(recipient.getMetadata());
        }
        individualRequest.setMetadata(metadata);
        
        return individualRequest;
    }

    @Override
    public NotificationResponse scheduleNotification(NotificationRequest request, LocalDateTime scheduledTime) {
        request.setScheduledAt(scheduledTime);
        return sendNotification(request);
    }

    @Override
    public NotificationResponse sendTemplateNotification(String tenantId, String templateId, 
                                                       String recipientId, String recipientContact,
                                                       NotificationChannel channel, 
                                                       Map<String, Object> variables) {
        try {
            NotificationTemplate template = templateService.getLatestTemplate(tenantId, templateId);
            
            if (!template.isActive()) {
                return NotificationResponse.failure("Template is not active: " + templateId);
            }
            
            // Render template
            TemplateService.RenderedTemplate rendered = templateService.renderTemplate(template.getId(), variables);
            
            // Create notification request
            NotificationRequest request = NotificationRequest.builder()
                .tenantId(tenantId)
                .recipientId(recipientId)
                .recipientContact(recipientContact)
                .channel(channel)
                .subject(rendered.getSubject())
                .content(rendered.getContent())
                .htmlContent(rendered.getHtmlContent())
                .template(templateId, template.getVersion())
                .templateVariable("variables", variables)
                .channelConfig("template_config", template.getChannelSpecificConfig());
            
            return sendNotification(request);
            
        } catch (Exception e) {
            logger.error("Failed to send template notification: {}", e.getMessage(), e);
            return NotificationResponse.failure("Failed to send template notification: " + e.getMessage());
        }
    }

    @Override
    public List<NotificationResponse> sendMultiChannelNotification(String tenantId, String recipientId, 
                                                                 List<NotificationChannel> channels,
                                                                 DeliveryStrategy strategy, 
                                                                 NotificationRequest request) {
        
        List<NotificationResponse> responses = new ArrayList<>();
        
        try {
            // Get user preferences
            List<NotificationChannel> preferredChannels = preferenceService
                .getPreferredChannels(tenantId, recipientId, request.getCategory());
            
            // Filter channels based on preferences
            List<NotificationChannel> effectiveChannels = channels.stream()
                .filter(preferredChannels::contains)
                .collect(Collectors.toList());
            
            if (effectiveChannels.isEmpty()) {
                return List.of(NotificationResponse.failure("No channels available after preference filtering"));
            }
            
            switch (strategy) {
                case SINGLE_CHANNEL:
                    NotificationChannel optimal = getOptimalChannel(tenantId, recipientId, request.getCategory());
                    request.setChannel(optimal);
                    responses.add(sendNotification(request));
                    break;
                    
                case FAILOVER:
                    responses.addAll(sendWithFailover(request, effectiveChannels));
                    break;
                    
                case BROADCAST:
                    responses.addAll(sendToBroadcast(request, effectiveChannels));
                    break;
                    
                case SMART:
                    responses.addAll(sendWithSmartStrategy(request, effectiveChannels, tenantId, recipientId));
                    break;
                    
                default:
                    request.setChannel(effectiveChannels.get(0));
                    responses.add(sendNotification(request));
                    break;
            }
            
        } catch (Exception e) {
            logger.error("Failed to send multi-channel notification: {}", e.getMessage(), e);
            responses.add(NotificationResponse.failure("Failed to send multi-channel notification: " + e.getMessage()));
        }
        
        return responses;
    }

    private List<NotificationResponse> sendWithFailover(NotificationRequest request, List<NotificationChannel> channels) {
        List<NotificationResponse> responses = new ArrayList<>();
        
        for (NotificationChannel channel : channels) {
            request.setChannel(channel);
            NotificationResponse response = sendNotification(request);
            responses.add(response);
            
            if (response.isSuccess()) {
                break; // Success, no need to try other channels
            }
        }
        
        return responses;
    }

    private List<NotificationResponse> sendToBroadcast(NotificationRequest request, List<NotificationChannel> channels) {
        return channels.stream().map(channel -> {
            NotificationRequest channelRequest = cloneRequest(request);
            channelRequest.setChannel(channel);
            return sendNotification(channelRequest);
        }).collect(Collectors.toList());
    }

    private List<NotificationResponse> sendWithSmartStrategy(NotificationRequest request, 
                                                           List<NotificationChannel> channels, 
                                                           String tenantId, String recipientId) {
        // Use delivery optimization service to determine best strategy
        NotificationChannel optimalChannel = deliveryOptimizationService
            .getOptimalChannel(tenantId, recipientId, channels, request.getCategory());
        
        request.setChannel(optimalChannel);
        return List.of(sendNotification(request));
    }

    @Override
    public void cancelNotification(UUID notificationId, String reason) {
        Notification notification = getNotification(notificationId);
        
        if (notification.getStatus().isTerminal()) {
            throw new IllegalStateException("Cannot cancel notification in terminal state: " + notification.getStatus());
        }
        
        notification.setStatus(NotificationStatus.CANCELLED);
        notification.setErrorMessage(reason);
        notificationRepository.save(notification);
        
        // Remove from queue if present
        queueRepository.findByNotificationId(notificationId)
            .forEach(queueItem -> {
                queueItem.markAsCancelled();
                queueRepository.save(queueItem);
            });
        
        eventPublisher.publishEvent(new NotificationEvent.Cancelled(notification, reason));
        
        logger.info("Notification cancelled: {} - {}", notificationId, reason);
    }

    @Override
    public void updateNotificationStatus(UUID notificationId, NotificationStatusUpdate statusUpdate) {
        Notification notification = getNotification(notificationId);
        
        NotificationStatus previousStatus = notification.getStatus();
        notification.setStatus(statusUpdate.getStatus());
        
        if (statusUpdate.getTimestamp() != null) {
            switch (statusUpdate.getStatus()) {
                case SENT -> notification.setSentAt(statusUpdate.getTimestamp());
                case DELIVERED -> notification.setDeliveredAt(statusUpdate.getTimestamp());
                case READ -> notification.setReadAt(statusUpdate.getTimestamp());
            }
        }
        
        if (statusUpdate.getErrorMessage() != null) {
            notification.setErrorMessage(statusUpdate.getErrorMessage());
        }
        
        if (statusUpdate.getProviderData() != null) {
            notification.getDeliveryTracking().putAll(statusUpdate.getProviderData());
        }
        
        notificationRepository.save(notification);
        
        eventPublisher.publishEvent(new NotificationEvent.StatusUpdated(notification, previousStatus));
        
        logger.info("Notification status updated: {} {} -> {}", 
                   notificationId, previousStatus, statusUpdate.getStatus());
    }

    @Override
    @Transactional(readOnly = true)
    public Notification getNotification(UUID notificationId) {
        return notificationRepository.findById(notificationId)
            .orElseThrow(() -> new NotificationNotFoundException("Notification not found: " + notificationId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getUserNotifications(String tenantId, String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notification> notifications = notificationRepository
            .findByTenantIdAndRecipientId(tenantId, userId, pageable);
        return notifications.getContent();
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadNotificationsCount(String tenantId, String userId) {
        return notificationRepository.countByTenantIdAndRecipientIdAndReadAtIsNull(tenantId, userId);
    }

    @Override
    public void markAsRead(UUID notificationId, String userId) {
        Notification notification = getNotification(notificationId);
        
        if (!notification.getRecipientId().equals(userId)) {
            throw new IllegalArgumentException("User not authorized to read this notification");
        }
        
        if (notification.getReadAt() == null) {
            notification.markAsRead();
            notificationRepository.save(notification);
            
            eventPublisher.publishEvent(new NotificationEvent.Read(notification));
            
            logger.info("Notification marked as read: {} by user {}", notificationId, userId);
        }
    }

    @Override
    public void markAllAsRead(String tenantId, String userId) {
        List<Notification> unreadNotifications = notificationRepository
            .findByTenantIdAndRecipientIdAndReadAtIsNull(tenantId, userId);
        
        LocalDateTime now = LocalDateTime.now();
        unreadNotifications.forEach(notification -> {
            notification.setReadAt(now);
            notification.setStatus(NotificationStatus.READ);
        });
        
        notificationRepository.saveAll(unreadNotifications);
        
        logger.info("All notifications marked as read for user {} in tenant {}", userId, tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDeliveryStatus(UUID notificationId) {
        Notification notification = getNotification(notificationId);
        
        Map<String, Object> status = new HashMap<>();
        status.put("notificationId", notificationId);
        status.put("status", notification.getStatus());
        status.put("channel", notification.getChannel());
        status.put("createdAt", notification.getCreatedAt());
        status.put("scheduledAt", notification.getScheduledAt());
        status.put("sentAt", notification.getSentAt());
        status.put("deliveredAt", notification.getDeliveredAt());
        status.put("readAt", notification.getReadAt());
        status.put("retryCount", notification.getRetryCount());
        status.put("errorMessage", notification.getErrorMessage());
        status.put("deliveryTracking", notification.getDeliveryTracking());
        
        return status;
    }

    @Override
    public void retryNotification(UUID notificationId) {
        Notification notification = getNotification(notificationId);
        
        if (!notification.canRetry()) {
            throw new IllegalStateException("Cannot retry notification: max retries exceeded");
        }
        
        if (notification.isExpired()) {
            throw new IllegalStateException("Cannot retry notification: expired");
        }
        
        notification.setStatus(NotificationStatus.QUEUED);
        notification.incrementRetryCount();
        notificationRepository.save(notification);
        
        queueNotificationForDelivery(notification);
        
        logger.info("Notification queued for retry: {}", notificationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getCampaignNotifications(String campaignId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notification> notifications = notificationRepository
            .findByCampaignId(campaignId, pageable);
        return notifications.getContent();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getNotificationAnalytics(String tenantId, LocalDateTime from, LocalDateTime to) {
        Map<String, Object> analytics = new HashMap<>();
        
        // Get basic counts
        long totalSent = notificationRepository.countByTenantIdAndCreatedAtBetween(tenantId, from, to);
        long totalDelivered = notificationRepository.countByTenantIdAndStatusAndCreatedAtBetween(
            tenantId, NotificationStatus.DELIVERED, from, to);
        long totalFailed = notificationRepository.countByTenantIdAndStatusAndCreatedAtBetween(
            tenantId, NotificationStatus.FAILED, from, to);
        long totalRead = notificationRepository.countByTenantIdAndStatusAndCreatedAtBetween(
            tenantId, NotificationStatus.READ, from, to);
        
        analytics.put("totalSent", totalSent);
        analytics.put("totalDelivered", totalDelivered);
        analytics.put("totalFailed", totalFailed);
        analytics.put("totalRead", totalRead);
        analytics.put("deliveryRate", totalSent > 0 ? (double) totalDelivered / totalSent * 100 : 0);
        analytics.put("readRate", totalDelivered > 0 ? (double) totalRead / totalDelivered * 100 : 0);
        analytics.put("failureRate", totalSent > 0 ? (double) totalFailed / totalSent * 100 : 0);
        
        // Get channel breakdown
        Map<NotificationChannel, Long> channelBreakdown = Arrays.stream(NotificationChannel.values())
            .collect(Collectors.toMap(
                channel -> channel,
                channel -> notificationRepository.countByTenantIdAndChannelAndCreatedAtBetween(
                    tenantId, channel, from, to)
            ));
        analytics.put("channelBreakdown", channelBreakdown);
        
        // Get daily trends
        // This would require more complex queries in a real implementation
        analytics.put("dailyTrends", new HashMap<>());
        
        return analytics;
    }

    @Override
    public void validateNotificationRequest(NotificationRequest request) {
        if (request.getTenantId() == null || request.getTenantId().isEmpty()) {
            throw new NotificationValidationException("Tenant ID is required");
        }
        
        if (request.getRecipientId() == null || request.getRecipientId().isEmpty()) {
            throw new NotificationValidationException("Recipient ID is required");
        }
        
        if (request.getRecipientContact() == null || request.getRecipientContact().isEmpty()) {
            throw new NotificationValidationException("Recipient contact is required");
        }
        
        if (request.getChannel() == null) {
            throw new NotificationValidationException("Notification channel is required");
        }
        
        if (request.getSubject() == null || request.getSubject().isEmpty()) {
            throw new NotificationValidationException("Subject is required");
        }
        
        if (request.getContent() == null || request.getContent().isEmpty()) {
            throw new NotificationValidationException("Content is required");
        }
        
        // Validate channel-specific requirements
        NotificationChannelProvider provider = getChannelProvider(request.getChannel());
        if (provider != null) {
            // Create temporary notification for validation
            Notification tempNotification = createNotificationFromRequest(request);
            provider.validateNotification(tempNotification);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationChannel getOptimalChannel(String tenantId, String userId, String category) {
        return deliveryOptimizationService.getOptimalChannel(tenantId, userId, category);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isNotificationAllowed(String tenantId, String userId, String category, 
                                       NotificationChannel channel) {
        return preferenceService.isNotificationAllowed(tenantId, userId, category, channel);
    }

    @Override
    public void processQueuedNotifications() {
        // This would be called by a scheduled job
        List<NotificationQueue> queuedItems = queueRepository
            .findReadyForProcessing(LocalDateTime.now(), PageRequest.of(0, 100));
        
        for (NotificationQueue queueItem : queuedItems) {
            try {
                processQueuedNotification(queueItem);
            } catch (Exception e) {
                logger.error("Failed to process queued notification {}: {}", 
                           queueItem.getNotificationId(), e.getMessage());
                queueItem.markAsFailed(e.getMessage(), true);
                queueRepository.save(queueItem);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getNotificationTemplates(String tenantId, NotificationChannel channel) {
        List<NotificationTemplate> templates = templateService.getTemplates(tenantId, channel, 0, 100);
        
        return templates.stream().map(template -> {
            Map<String, Object> templateInfo = new HashMap<>();
            templateInfo.put("id", template.getId());
            templateInfo.put("name", template.getName());
            templateInfo.put("version", template.getVersion());
            templateInfo.put("description", template.getDescription());
            templateInfo.put("channel", template.getChannel());
            templateInfo.put("category", template.getCategory());
            templateInfo.put("requiredVariables", template.getRequiredVariables());
            return templateInfo;
        }).collect(Collectors.toList());
    }

    // Private helper methods
    
    private Notification createNotificationFromRequest(NotificationRequest request) {
        Notification notification = new Notification(
            request.getTenantId(),
            request.getRecipientId(),
            request.getRecipientContact(),
            request.getChannel(),
            request.getSubject(),
            request.getContent(),
            request.getTemplateId() != null ? request.getTemplateId() : "default"
        );
        
        notification.setHtmlContent(request.getHtmlContent());
        notification.setTemplateVersion(request.getTemplateVersion() != null ? request.getTemplateVersion() : "1.0");
        notification.setPriority(request.getPriority());
        notification.setDeliveryStrategy(request.getDeliveryStrategy());
        notification.setScheduledAt(request.getScheduledAt());
        notification.setExpiresAt(request.getExpiresAt());
        notification.setMaxRetries(request.getMaxRetries());
        notification.setCampaignId(request.getCampaignId());
        notification.setCorrelationId(request.getCorrelationId());
        notification.setTemplateVariables(request.getTemplateVariables());
        notification.setMetadata(request.getMetadata());
        notification.setChannelConfig(request.getChannelConfig());
        
        return notification;
    }

    private NotificationResponse createResponseFromNotification(Notification notification, String message) {
        NotificationResponse response = new NotificationResponse();
        response.setNotificationId(notification.getId());
        response.setTenantId(notification.getTenantId());
        response.setRecipientId(notification.getRecipientId());
        response.setChannel(notification.getChannel());
        response.setStatus(notification.getStatus());
        response.setPriority(notification.getPriority());
        response.setSubject(notification.getSubject());
        response.setCreatedAt(notification.getCreatedAt());
        response.setScheduledAt(notification.getScheduledAt());
        response.setSentAt(notification.getSentAt());
        response.setSuccess(true);
        response.setMessage(message);
        return response;
    }

    private void scheduleNotificationDelivery(Notification notification) {
        NotificationQueue queueItem = new NotificationQueue(
            notification.getId(),
            notification.getTenantId(),
            notification.getChannel(),
            notification.getPriority(),
            notification.getScheduledAt()
        );
        
        queueRepository.save(queueItem);
    }

    private void queueNotificationForDelivery(Notification notification) {
        NotificationQueue queueItem = new NotificationQueue(
            notification.getId(),
            notification.getTenantId(),
            notification.getChannel(),
            notification.getPriority(),
            LocalDateTime.now()
        );
        
        queueRepository.save(queueItem);
    }

    private void processQueuedNotification(NotificationQueue queueItem) {
        queueItem.markAsProcessing("node-" + UUID.randomUUID().toString());
        queueRepository.save(queueItem);
        
        Notification notification = getNotification(queueItem.getNotificationId());
        NotificationChannelProvider provider = getChannelProvider(notification.getChannel());
        
        if (provider == null) {
            throw new IllegalStateException("No provider found for channel: " + notification.getChannel());
        }
        
        NotificationChannelProvider.ChannelDeliveryResult result = provider.sendNotification(notification);
        
        if (result.isSuccess()) {
            notification.markAsSent();
            notification.setExternalId(result.getExternalId());
            queueItem.markAsCompleted();
        } else {
            notification.markAsFailed(result.getMessage());
            queueItem.markAsFailed(result.getMessage(), notification.canRetry());
        }
        
        notificationRepository.save(notification);
        queueRepository.save(queueItem);
    }

    private NotificationChannelProvider getChannelProvider(NotificationChannel channel) {
        return channelProviders.stream()
            .filter(provider -> provider.getSupportedChannel() == channel)
            .findFirst()
            .orElse(null);
    }

    private NotificationRequest cloneRequest(NotificationRequest original) {
        // Simple cloning - in production, you might want to use a proper cloning library
        NotificationRequest clone = new NotificationRequest();
        clone.setTenantId(original.getTenantId());
        clone.setRecipientId(original.getRecipientId());
        clone.setRecipientContact(original.getRecipientContact());
        clone.setChannel(original.getChannel());
        clone.setPriority(original.getPriority());
        clone.setDeliveryStrategy(original.getDeliveryStrategy());
        clone.setSubject(original.getSubject());
        clone.setContent(original.getContent());
        clone.setHtmlContent(original.getHtmlContent());
        clone.setTemplateId(original.getTemplateId());
        clone.setTemplateVersion(original.getTemplateVersion());
        clone.setTemplateVariables(new HashMap<>(original.getTemplateVariables()));
        clone.setMetadata(new HashMap<>(original.getMetadata()));
        clone.setChannelConfig(new HashMap<>(original.getChannelConfig()));
        clone.setCategory(original.getCategory());
        clone.setCampaignId(original.getCampaignId());
        clone.setCorrelationId(original.getCorrelationId());
        clone.setScheduledAt(original.getScheduledAt());
        clone.setExpiresAt(original.getExpiresAt());
        clone.setMaxRetries(original.getMaxRetries());
        return clone;
    }
}