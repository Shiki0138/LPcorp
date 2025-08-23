package com.hive.notification.repository;

import com.hive.notification.domain.entity.NotificationTemplate;
import com.hive.notification.domain.enums.NotificationChannel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for NotificationTemplate entities
 */
@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {

    /**
     * Find template by tenant, name and version
     */
    Optional<NotificationTemplate> findByTenantIdAndNameAndVersion(String tenantId, String name, String version);

    /**
     * Find latest version of template by tenant and name
     */
    @Query("SELECT t FROM NotificationTemplate t WHERE t.tenantId = :tenantId AND t.name = :name " +
           "ORDER BY t.version DESC LIMIT 1")
    Optional<NotificationTemplate> findLatestByTenantIdAndName(@Param("tenantId") String tenantId, @Param("name") String name);

    /**
     * Find templates by tenant and status
     */
    Page<NotificationTemplate> findByTenantIdAndStatus(String tenantId, 
                                                      NotificationTemplate.TemplateStatus status, 
                                                      Pageable pageable);

    /**
     * Find templates by tenant, channel and status
     */
    Page<NotificationTemplate> findByTenantIdAndChannelAndStatus(String tenantId, 
                                                               NotificationChannel channel,
                                                               NotificationTemplate.TemplateStatus status, 
                                                               Pageable pageable);

    /**
     * Find templates by tenant and channel
     */
    List<NotificationTemplate> findByTenantIdAndChannel(String tenantId, NotificationChannel channel);

    /**
     * Find all versions of a template
     */
    List<NotificationTemplate> findByTenantIdAndNameOrderByVersionDesc(String tenantId, String name);

    /**
     * Find templates by tenant and name with status
     */
    List<NotificationTemplate> findByTenantIdAndNameAndStatus(String tenantId, String name, 
                                                             NotificationTemplate.TemplateStatus status);

    /**
     * Find templates by category
     */
    List<NotificationTemplate> findByTenantIdAndCategory(String tenantId, String category);

    /**
     * Find templates by tags
     */
    @Query("SELECT t FROM NotificationTemplate t WHERE t.tenantId = :tenantId AND t.tags LIKE %:tag%")
    List<NotificationTemplate> findByTenantIdAndTag(@Param("tenantId") String tenantId, @Param("tag") String tag);

    /**
     * Find approved templates only
     */
    List<NotificationTemplate> findByTenantIdAndStatusOrderByNameAsc(String tenantId, 
                                                                    NotificationTemplate.TemplateStatus status);

    /**
     * Find templates needing approval
     */
    List<NotificationTemplate> findByTenantIdAndStatus(String tenantId, NotificationTemplate.TemplateStatus status);

    /**
     * Find templates by approver
     */
    List<NotificationTemplate> findByTenantIdAndApprovedBy(String tenantId, String approvedBy);

    /**
     * Search templates by name or description
     */
    @Query("SELECT t FROM NotificationTemplate t WHERE t.tenantId = :tenantId AND " +
           "(LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<NotificationTemplate> searchTemplates(@Param("tenantId") String tenantId, @Param("search") String search);

    /**
     * Find templates by locale
     */
    List<NotificationTemplate> findByTenantIdAndLocale(String tenantId, String locale);

    /**
     * Find templates with specific metadata
     */
    @Query("SELECT t FROM NotificationTemplate t WHERE t.tenantId = :tenantId AND " +
           "JSON_EXTRACT(t.metadata, :jsonPath) = :value")
    List<NotificationTemplate> findByMetadata(@Param("tenantId") String tenantId, 
                                             @Param("jsonPath") String jsonPath, 
                                             @Param("value") String value);

    /**
     * Count templates by tenant
     */
    long countByTenantId(String tenantId);

    /**
     * Count templates by tenant and status
     */
    long countByTenantIdAndStatus(String tenantId, NotificationTemplate.TemplateStatus status);

    /**
     * Count templates by tenant and channel
     */
    long countByTenantIdAndChannel(String tenantId, NotificationChannel channel);

    /**
     * Find duplicate template names
     */
    @Query("SELECT t.name FROM NotificationTemplate t WHERE t.tenantId = :tenantId " +
           "GROUP BY t.name HAVING COUNT(t.name) > 1")
    List<String> findDuplicateTemplateNames(@Param("tenantId") String tenantId);

    /**
     * Find templates created by specific user
     */
    @Query("SELECT t FROM NotificationTemplate t WHERE t.tenantId = :tenantId AND " +
           "JSON_EXTRACT(t.metadata, '$.createdBy') = :createdBy")
    List<NotificationTemplate> findByCreatedBy(@Param("tenantId") String tenantId, @Param("createdBy") String createdBy);

    /**
     * Find templates updated after specific date
     */
    @Query("SELECT t FROM NotificationTemplate t WHERE t.tenantId = :tenantId AND t.updatedAt > :since")
    List<NotificationTemplate> findUpdatedSince(@Param("tenantId") String tenantId, 
                                               @Param("since") java.time.LocalDateTime since);

    /**
     * Find templates by required variable
     */
    @Query("SELECT t FROM NotificationTemplate t WHERE t.tenantId = :tenantId AND " +
           "JSON_EXTRACT(t.requiredVariables, CONCAT('$.', :variableName)) IS NOT NULL")
    List<NotificationTemplate> findByRequiredVariable(@Param("tenantId") String tenantId, 
                                                     @Param("variableName") String variableName);

    /**
     * Find templates without required variables
     */
    @Query("SELECT t FROM NotificationTemplate t WHERE t.tenantId = :tenantId AND " +
           "(t.requiredVariables IS NULL OR JSON_LENGTH(t.requiredVariables) = 0)")
    List<NotificationTemplate> findTemplatesWithoutRequiredVariables(@Param("tenantId") String tenantId);

    /**
     * Find templates by channel config
     */
    @Query("SELECT t FROM NotificationTemplate t WHERE t.tenantId = :tenantId AND " +
           "JSON_EXTRACT(t.channelSpecificConfig, :jsonPath) IS NOT NULL")
    List<NotificationTemplate> findByChannelConfig(@Param("tenantId") String tenantId, 
                                                  @Param("jsonPath") String jsonPath);

    /**
     * Find archived templates
     */
    List<NotificationTemplate> findByTenantIdAndStatusAndUpdatedAtBefore(
        String tenantId, 
        NotificationTemplate.TemplateStatus status, 
        java.time.LocalDateTime before);

    /**
     * Find templates for cleanup
     */
    @Query("SELECT t FROM NotificationTemplate t WHERE t.tenantId = :tenantId AND " +
           "t.status = 'ARCHIVED' AND t.updatedAt < :cutoff")
    List<NotificationTemplate> findTemplatesForCleanup(@Param("tenantId") String tenantId, 
                                                      @Param("cutoff") java.time.LocalDateTime cutoff);

    /**
     * Get template usage statistics
     */
    @Query("SELECT t.id, t.name, COUNT(n.id) as usageCount " +
           "FROM NotificationTemplate t LEFT JOIN Notification n ON t.name = n.templateId " +
           "WHERE t.tenantId = :tenantId AND n.createdAt >= :since " +
           "GROUP BY t.id, t.name ORDER BY usageCount DESC")
    List<Object[]> getTemplateUsageStatistics(@Param("tenantId") String tenantId, 
                                             @Param("since") java.time.LocalDateTime since);

    /**
     * Find popular templates
     */
    @Query("SELECT t.* FROM NotificationTemplate t " +
           "LEFT JOIN (SELECT n.templateId, COUNT(*) as usage_count " +
           "           FROM Notification n WHERE n.createdAt >= :since " +
           "           GROUP BY n.templateId) usage ON t.name = usage.templateId " +
           "WHERE t.tenantId = :tenantId AND t.status = 'APPROVED' " +
           "ORDER BY COALESCE(usage.usage_count, 0) DESC")
    List<NotificationTemplate> findPopularTemplates(@Param("tenantId") String tenantId, 
                                                   @Param("since") java.time.LocalDateTime since, 
                                                   Pageable pageable);

    /**
     * Check if template name exists
     */
    boolean existsByTenantIdAndName(String tenantId, String name);

    /**
     * Check if specific version exists
     */
    boolean existsByTenantIdAndNameAndVersion(String tenantId, String name, String version);

    /**
     * Delete templates by tenant
     */
    void deleteByTenantId(String tenantId);
}