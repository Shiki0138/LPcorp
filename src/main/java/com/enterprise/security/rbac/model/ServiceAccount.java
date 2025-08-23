package com.enterprise.security.rbac.model;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Service account entity for service-to-service authentication
 */
@Entity
@Table(name = "rbac_service_accounts", indexes = {
    @Index(name = "idx_sa_service_name", columnList = "serviceName"),
    @Index(name = "idx_sa_tenant", columnList = "tenantId"),
    @Index(name = "idx_sa_api_key_hash", columnList = "apiKeyHash"),
    @Index(name = "idx_sa_active", columnList = "active")
})
public class ServiceAccount {
    
    @Id
    private String id;
    
    @Column(nullable = false, length = 100)
    @NotBlank(message = "Service name is required")
    private String serviceName;
    
    @Column(length = 500)
    private String description;
    
    @Column(nullable = false)
    private String tenantId;
    
    @Column(nullable = false)
    private String userId; // Associated user account
    
    @Column(nullable = false, unique = true)
    private String apiKeyHash;
    
    @Column(nullable = false)
    private Boolean active = true;
    
    @Column
    private Instant expiresAt;
    
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "rbac_service_allowed_services",
        joinColumns = @JoinColumn(name = "service_account_id")
    )
    @Column(name = "service_name")
    private Set<String> allowedServices = new HashSet<>();
    
    // Rate limiting
    @Column
    private Integer maxRequestsPerMinute = 1000;
    
    @Column
    private Integer maxRequestsPerHour = 10000;
    
    // IP restrictions
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "rbac_service_allowed_ips",
        joinColumns = @JoinColumn(name = "service_account_id")
    )
    @Column(name = "ip_range")
    private Set<String> allowedIpRanges = new HashSet<>();
    
    // Audit fields
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    
    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
    
    @Column
    private String createdBy;
    
    @Column
    private String updatedBy;
    
    @Column
    private Instant lastUsedAt;
    
    @Column
    private Instant lastRotatedAt;
    
    @Column
    private String lastRotatedBy;
    
    @Column
    private Instant disabledAt;
    
    @Column
    private String disabledBy;
    
    // Transient field for returning the plain API key (only once)
    @Transient
    private String plainApiKey;
    
    // Constructors
    public ServiceAccount() {}
    
    public ServiceAccount(String serviceName, String tenantId, String userId) {
        this.serviceName = serviceName;
        this.tenantId = tenantId;
        this.userId = userId;
    }
    
    // Business methods
    public boolean isActive() {
        return Boolean.TRUE.equals(active) && !isExpired();
    }
    
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }
    
    public boolean isIpAllowed(String ipAddress) {
        if (allowedIpRanges.isEmpty()) {
            return true; // No restrictions
        }
        
        for (String ipRange : allowedIpRanges) {
            if (isIpInRange(ipAddress, ipRange)) {
                return true;
            }
        }
        
        return false;
    }
    
    public boolean isServiceAllowed(String serviceName) {
        if (allowedServices.isEmpty()) {
            return true; // No restrictions
        }
        
        return allowedServices.contains(serviceName) || allowedServices.contains("*");
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
    
    private boolean isIpInRange(String ip, String range) {
        // Simple implementation - would need proper CIDR matching in production
        if (range.contains("/")) {
            // CIDR notation
            return matchesCIDR(ip, range);
        } else {
            // Single IP
            return ip.equals(range);
        }
    }
    
    private boolean matchesCIDR(String ip, String cidr) {
        // Implement CIDR matching logic
        // This is a placeholder - use proper library like Apache Commons Net
        return false;
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getApiKeyHash() { return apiKeyHash; }
    public void setApiKeyHash(String apiKeyHash) { this.apiKeyHash = apiKeyHash; }
    
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    
    public Set<String> getAllowedServices() { return allowedServices; }
    public void setAllowedServices(Set<String> allowedServices) { this.allowedServices = allowedServices; }
    
    public Integer getMaxRequestsPerMinute() { return maxRequestsPerMinute; }
    public void setMaxRequestsPerMinute(Integer maxRequestsPerMinute) { this.maxRequestsPerMinute = maxRequestsPerMinute; }
    
    public Integer getMaxRequestsPerHour() { return maxRequestsPerHour; }
    public void setMaxRequestsPerHour(Integer maxRequestsPerHour) { this.maxRequestsPerHour = maxRequestsPerHour; }
    
    public Set<String> getAllowedIpRanges() { return allowedIpRanges; }
    public void setAllowedIpRanges(Set<String> allowedIpRanges) { this.allowedIpRanges = allowedIpRanges; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    
    public Instant getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(Instant lastUsedAt) { this.lastUsedAt = lastUsedAt; }
    
    public Instant getLastRotatedAt() { return lastRotatedAt; }
    public void setLastRotatedAt(Instant lastRotatedAt) { this.lastRotatedAt = lastRotatedAt; }
    
    public String getLastRotatedBy() { return lastRotatedBy; }
    public void setLastRotatedBy(String lastRotatedBy) { this.lastRotatedBy = lastRotatedBy; }
    
    public Instant getDisabledAt() { return disabledAt; }
    public void setDisabledAt(Instant disabledAt) { this.disabledAt = disabledAt; }
    
    public String getDisabledBy() { return disabledBy; }
    public void setDisabledBy(String disabledBy) { this.disabledBy = disabledBy; }
    
    public String getPlainApiKey() { return plainApiKey; }
    public void setPlainApiKey(String plainApiKey) { this.plainApiKey = plainApiKey; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServiceAccount)) return false;
        ServiceAccount that = (ServiceAccount) o;
        return id != null && id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
    
    @Override
    public String toString() {
        return "ServiceAccount{" +
                "id='" + id + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", tenantId='" + tenantId + '\'' +
                ", active=" + active +
                '}';
    }
}