package com.hive.notification.repository;

import com.hive.notification.domain.entity.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for NotificationPreference entities
 */
@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {

    /**
     * Find preference by tenant, user and category
     */
    Optional<NotificationPreference> findByTenantIdAndUserIdAndCategory(String tenantId, String userId, String category);

    /**
     * Find all preferences for a user
     */
    List<NotificationPreference> findByTenantIdAndUserId(String tenantId, String userId);

    /**
     * Find preferences by tenant
     */
    List<NotificationPreference> findByTenantId(String tenantId);

    /**
     * Find preferences by category
     */
    List<NotificationPreference> findByTenantIdAndCategory(String tenantId, String category);

    /**
     * Find enabled preferences
     */
    List<NotificationPreference> findByTenantIdAndUserIdAndEnabled(String tenantId, String userId, boolean enabled);

    /**
     * Find preferences with marketing consent
     */
    List<NotificationPreference> findByTenantIdAndMarketingConsent(String tenantId, boolean marketingConsent);

    /**
     * Find preferences updated after specific date
     */
    List<NotificationPreference> findByTenantIdAndConsentUpdatedAtAfter(String tenantId, LocalDateTime after);

    /**
     * Check if preference exists
     */
    boolean existsByTenantIdAndUserIdAndCategory(String tenantId, String userId, String category);

    /**
     * Delete preferences by user
     */
    void deleteByTenantIdAndUserId(String tenantId, String userId);

    /**
     * Delete preferences by tenant
     */
    void deleteByTenantId(String tenantId);

    /**
     * Count preferences by tenant
     */
    long countByTenantId(String tenantId);

    /**
     * Count enabled preferences
     */
    long countByTenantIdAndEnabled(String tenantId, boolean enabled);

    /**
     * Find users with specific channel enabled
     */
    @Query("SELECT DISTINCT p.userId FROM NotificationPreference p WHERE p.tenantId = :tenantId " +
           "AND JSON_EXTRACT(p.channelPreferences, CONCAT('$.', :channel)) = true")
    List<String> findUsersWithChannelEnabled(@Param("tenantId") String tenantId, @Param("channel") String channel);

    /**
     * Find users in quiet hours
     */
    @Query("SELECT p FROM NotificationPreference p WHERE p.tenantId = :tenantId " +
           "AND p.quietHoursStart IS NOT NULL AND p.quietHoursEnd IS NOT NULL " +
           "AND ((p.quietHoursStart < p.quietHoursEnd AND :currentTime BETWEEN p.quietHoursStart AND p.quietHoursEnd) " +
           "OR (p.quietHoursStart > p.quietHoursEnd AND (:currentTime >= p.quietHoursStart OR :currentTime <= p.quietHoursEnd)))")
    List<NotificationPreference> findUsersInQuietHours(@Param("tenantId") String tenantId, 
                                                       @Param("currentTime") java.time.LocalTime currentTime);

    /**
     * Find users with daily notification limits
     */
    List<NotificationPreference> findByTenantIdAndMaxDailyNotificationsIsNotNull(String tenantId);

    /**
     * Find preferences with specific advanced setting
     */
    @Query("SELECT p FROM NotificationPreference p WHERE p.tenantId = :tenantId " +
           "AND JSON_EXTRACT(p.advancedSettings, :jsonPath) = :value")
    List<NotificationPreference> findByAdvancedSetting(@Param("tenantId") String tenantId, 
                                                      @Param("jsonPath") String jsonPath, 
                                                      @Param("value") String value);

    /**
     * Get consent statistics
     */
    @Query("SELECT p.marketingConsent, COUNT(p) FROM NotificationPreference p " +
           "WHERE p.tenantId = :tenantId GROUP BY p.marketingConsent")
    List<Object[]> getConsentStatistics(@Param("tenantId") String tenantId);

    /**
     * Find preferences needing consent update
     */
    @Query("SELECT p FROM NotificationPreference p WHERE p.tenantId = :tenantId " +
           "AND (p.consentUpdatedAt IS NULL OR p.consentUpdatedAt < :cutoff)")
    List<NotificationPreference> findPreferencesNeedingConsentUpdate(@Param("tenantId") String tenantId, 
                                                                    @Param("cutoff") LocalDateTime cutoff);
}