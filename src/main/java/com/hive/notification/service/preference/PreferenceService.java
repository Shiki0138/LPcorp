package com.hive.notification.service.preference;

import com.hive.notification.domain.entity.NotificationPreference;
import com.hive.notification.domain.enums.NotificationChannel;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing user notification preferences
 */
public interface PreferenceService {

    /**
     * Get user preferences for a category
     */
    NotificationPreference getUserPreferences(String tenantId, String userId, String category);

    /**
     * Update user preferences
     */
    NotificationPreference updatePreferences(NotificationPreference preferences);

    /**
     * Get preferred channels for user and category
     */
    List<NotificationChannel> getPreferredChannels(String tenantId, String userId, String category);

    /**
     * Check if notification is allowed
     */
    boolean isNotificationAllowed(String tenantId, String userId, String category, NotificationChannel channel);

    /**
     * Check if user is in quiet hours
     */
    boolean isInQuietHours(String tenantId, String userId, String category);

    /**
     * Check if user has exceeded rate limits
     */
    boolean hasExceededRateLimit(String tenantId, String userId, String category);

    /**
     * Opt user out of a channel
     */
    void optOutFromChannel(String tenantId, String userId, String category, NotificationChannel channel);

    /**
     * Opt user into a channel
     */
    void optInToChannel(String tenantId, String userId, String category, NotificationChannel channel);

    /**
     * Update consent settings
     */
    void updateConsent(String tenantId, String userId, boolean marketingConsent, boolean analyticsConsent);

    /**
     * Get all preferences for a user
     */
    List<NotificationPreference> getAllUserPreferences(String tenantId, String userId);

    /**
     * Create default preferences for new user
     */
    void createDefaultPreferences(String tenantId, String userId);

    /**
     * Delete user preferences
     */
    void deleteUserPreferences(String tenantId, String userId);

    /**
     * Check if user should receive notification based on throttling
     */
    boolean shouldThrottle(String tenantId, String userId, String category);
}