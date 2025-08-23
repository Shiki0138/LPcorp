package com.hive.notification.service;

import com.hive.notification.domain.entity.Notification;
import com.hive.notification.domain.enums.NotificationChannel;
import com.hive.notification.domain.enums.NotificationPriority;
import com.hive.notification.domain.enums.DeliveryStrategy;
import com.hive.notification.dto.NotificationRequest;
import com.hive.notification.dto.NotificationResponse;
import com.hive.notification.dto.BulkNotificationRequest;
import com.hive.notification.dto.NotificationStatusUpdate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Core notification service interface
 */
public interface NotificationService {

    /**
     * Send a single notification
     */
    NotificationResponse sendNotification(NotificationRequest request);

    /**
     * Send bulk notifications
     */
    List<NotificationResponse> sendBulkNotifications(BulkNotificationRequest request);

    /**
     * Schedule a notification for later delivery
     */
    NotificationResponse scheduleNotification(NotificationRequest request, LocalDateTime scheduledTime);

    /**
     * Send a notification using a template
     */
    NotificationResponse sendTemplateNotification(String tenantId, String templateId, 
                                                String recipientId, String recipientContact,
                                                NotificationChannel channel, 
                                                Map<String, Object> variables);

    /**
     * Send multi-channel notification with delivery strategy
     */
    List<NotificationResponse> sendMultiChannelNotification(String tenantId, String recipientId, 
                                                          List<NotificationChannel> channels,
                                                          DeliveryStrategy strategy, 
                                                          NotificationRequest request);

    /**
     * Cancel a scheduled notification
     */
    void cancelNotification(UUID notificationId, String reason);

    /**
     * Update notification status (for external delivery confirmations)
     */
    void updateNotificationStatus(UUID notificationId, NotificationStatusUpdate statusUpdate);

    /**
     * Get notification by ID
     */
    Notification getNotification(UUID notificationId);

    /**
     * Get notifications for a user
     */
    List<Notification> getUserNotifications(String tenantId, String userId, int page, int size);

    /**
     * Get unread notifications count
     */
    long getUnreadNotificationsCount(String tenantId, String userId);

    /**
     * Mark notification as read
     */
    void markAsRead(UUID notificationId, String userId);

    /**
     * Mark all notifications as read for a user
     */
    void markAllAsRead(String tenantId, String userId);

    /**
     * Get notification delivery status
     */
    Map<String, Object> getDeliveryStatus(UUID notificationId);

    /**
     * Retry failed notification
     */
    void retryNotification(UUID notificationId);

    /**
     * Get notifications by campaign
     */
    List<Notification> getCampaignNotifications(String campaignId, int page, int size);

    /**
     * Get notification analytics
     */
    Map<String, Object> getNotificationAnalytics(String tenantId, LocalDateTime from, LocalDateTime to);

    /**
     * Validate notification request
     */
    void validateNotificationRequest(NotificationRequest request);

    /**
     * Get optimal delivery channel for user
     */
    NotificationChannel getOptimalChannel(String tenantId, String userId, String category);

    /**
     * Check if notification is allowed by user preferences
     */
    boolean isNotificationAllowed(String tenantId, String userId, String category, 
                                NotificationChannel channel);

    /**
     * Process queued notifications
     */
    void processQueuedNotifications();

    /**
     * Get notification templates for tenant
     */
    List<Map<String, Object>> getNotificationTemplates(String tenantId, NotificationChannel channel);
}