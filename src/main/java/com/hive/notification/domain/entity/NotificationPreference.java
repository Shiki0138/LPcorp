package com.hive.notification.domain.entity;

import com.hive.notification.domain.enums.NotificationChannel;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * User notification preferences entity
 */
@Entity
@Table(name = "notification_preferences", 
    uniqueConstraints = @UniqueConstraint(columnNames = {"tenantId", "userId", "category"}),
    indexes = {
        @Index(name = "idx_preference_user", columnList = "userId"),
        @Index(name = "idx_preference_tenant", columnList = "tenantId"),
        @Index(name = "idx_preference_category", columnList = "category")
    }
)
@EntityListeners(AuditingEntityListener.class)
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String category; // e.g., "security", "marketing", "system", "alerts"

    @Column(nullable = false)
    private boolean enabled = true;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<NotificationChannel, Boolean> channelPreferences = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<NotificationChannel, Integer> channelPriority = new HashMap<>();

    // Do not disturb settings
    private LocalTime quietHoursStart;
    private LocalTime quietHoursEnd;
    private String quietHoursTimezone = "UTC";

    @JdbcTypeCode(SqlTypes.JSON)
    private Set<String> quietDays = new HashSet<>(); // "MONDAY", "TUESDAY", etc.

    // Frequency settings
    private Integer maxDailyNotifications;
    private Integer maxWeeklyNotifications;
    private Integer maxHourlyNotifications;

    // Throttling settings
    private Integer minIntervalMinutes = 5; // Minimum minutes between notifications

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> advancedSettings = new HashMap<>();

    // Consent and compliance
    private boolean marketingConsent = false;
    private boolean analyticsConsent = true;
    private LocalDateTime consentUpdatedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, LocalDateTime> channelOptOuts = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> metadata = new HashMap<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    // Constructors
    public NotificationPreference() {
        initializeDefaults();
    }

    public NotificationPreference(String tenantId, String userId, String category) {
        this.tenantId = tenantId;
        this.userId = userId;
        this.category = category;
        initializeDefaults();
    }

    private void initializeDefaults() {
        // Set default channel preferences
        Arrays.stream(NotificationChannel.values()).forEach(channel -> {
            channelPreferences.put(channel, true);
            channelPriority.put(channel, getDefaultPriority(channel));
        });
    }

    private Integer getDefaultPriority(NotificationChannel channel) {
        return switch (channel) {
            case PUSH -> 1;
            case IN_APP -> 2;
            case EMAIL -> 3;
            case SMS -> 4;
            case WHATSAPP -> 5;
            case SLACK -> 6;
            case TEAMS -> 7;
            case WEBHOOK -> 8;
        };
    }

    // Business methods
    public boolean isChannelEnabled(NotificationChannel channel) {
        return enabled && channelPreferences.getOrDefault(channel, false);
    }

    public List<NotificationChannel> getPreferredChannels() {
        return channelPreferences.entrySet().stream()
            .filter(Map.Entry::getValue)
            .map(Map.Entry::getKey)
            .sorted(Comparator.comparing(channel -> channelPriority.getOrDefault(channel, 999)))
            .toList();
    }

    public void setChannelPreference(NotificationChannel channel, boolean enabled, Integer priority) {
        channelPreferences.put(channel, enabled);
        if (priority != null) {
            channelPriority.put(channel, priority);
        }
    }

    public void optOutFromChannel(NotificationChannel channel) {
        channelOptOuts.put(channel.getCode(), LocalDateTime.now());
        channelPreferences.put(channel, false);
    }

    public void optInToChannel(NotificationChannel channel) {
        channelOptOuts.remove(channel.getCode());
        channelPreferences.put(channel, true);
    }

    public boolean isInQuietHours(LocalDateTime timestamp) {
        if (quietHoursStart == null || quietHoursEnd == null) {
            return false;
        }

        LocalTime currentTime = timestamp.toLocalTime();
        
        if (quietHoursStart.isBefore(quietHoursEnd)) {
            return !currentTime.isBefore(quietHoursStart) && !currentTime.isAfter(quietHoursEnd);
        } else {
            // Quiet hours cross midnight
            return !currentTime.isBefore(quietHoursStart) || !currentTime.isAfter(quietHoursEnd);
        }
    }

    public boolean isQuietDay(LocalDateTime timestamp) {
        return quietDays.contains(timestamp.getDayOfWeek().name());
    }

    public boolean shouldThrottle(LocalDateTime lastNotificationTime) {
        if (lastNotificationTime == null || minIntervalMinutes == null) {
            return false;
        }

        return lastNotificationTime.plusMinutes(minIntervalMinutes).isAfter(LocalDateTime.now());
    }

    public void updateConsent(boolean marketing, boolean analytics) {
        this.marketingConsent = marketing;
        this.analyticsConsent = analytics;
        this.consentUpdatedAt = LocalDateTime.now();
    }

    public void addAdvancedSetting(String key, Object value) {
        this.advancedSettings.put(key, value);
    }

    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Map<NotificationChannel, Boolean> getChannelPreferences() { return channelPreferences; }
    public void setChannelPreferences(Map<NotificationChannel, Boolean> channelPreferences) { this.channelPreferences = channelPreferences; }

    public Map<NotificationChannel, Integer> getChannelPriority() { return channelPriority; }
    public void setChannelPriority(Map<NotificationChannel, Integer> channelPriority) { this.channelPriority = channelPriority; }

    public LocalTime getQuietHoursStart() { return quietHoursStart; }
    public void setQuietHoursStart(LocalTime quietHoursStart) { this.quietHoursStart = quietHoursStart; }

    public LocalTime getQuietHoursEnd() { return quietHoursEnd; }
    public void setQuietHoursEnd(LocalTime quietHoursEnd) { this.quietHoursEnd = quietHoursEnd; }

    public String getQuietHoursTimezone() { return quietHoursTimezone; }
    public void setQuietHoursTimezone(String quietHoursTimezone) { this.quietHoursTimezone = quietHoursTimezone; }

    public Set<String> getQuietDays() { return quietDays; }
    public void setQuietDays(Set<String> quietDays) { this.quietDays = quietDays; }

    public Integer getMaxDailyNotifications() { return maxDailyNotifications; }
    public void setMaxDailyNotifications(Integer maxDailyNotifications) { this.maxDailyNotifications = maxDailyNotifications; }

    public Integer getMaxWeeklyNotifications() { return maxWeeklyNotifications; }
    public void setMaxWeeklyNotifications(Integer maxWeeklyNotifications) { this.maxWeeklyNotifications = maxWeeklyNotifications; }

    public Integer getMaxHourlyNotifications() { return maxHourlyNotifications; }
    public void setMaxHourlyNotifications(Integer maxHourlyNotifications) { this.maxHourlyNotifications = maxHourlyNotifications; }

    public Integer getMinIntervalMinutes() { return minIntervalMinutes; }
    public void setMinIntervalMinutes(Integer minIntervalMinutes) { this.minIntervalMinutes = minIntervalMinutes; }

    public Map<String, Object> getAdvancedSettings() { return advancedSettings; }
    public void setAdvancedSettings(Map<String, Object> advancedSettings) { this.advancedSettings = advancedSettings; }

    public boolean isMarketingConsent() { return marketingConsent; }
    public void setMarketingConsent(boolean marketingConsent) { this.marketingConsent = marketingConsent; }

    public boolean isAnalyticsConsent() { return analyticsConsent; }
    public void setAnalyticsConsent(boolean analyticsConsent) { this.analyticsConsent = analyticsConsent; }

    public LocalDateTime getConsentUpdatedAt() { return consentUpdatedAt; }
    public void setConsentUpdatedAt(LocalDateTime consentUpdatedAt) { this.consentUpdatedAt = consentUpdatedAt; }

    public Map<String, LocalDateTime> getChannelOptOuts() { return channelOptOuts; }
    public void setChannelOptOuts(Map<String, LocalDateTime> channelOptOuts) { this.channelOptOuts = channelOptOuts; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}