package com.enterprise.security.rbac.model;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.Instant;

/**
 * Direct user permission grant entity
 */
@Entity
@Table(name = "rbac_user_permission_grants", indexes = {
    @Index(name = "idx_upg_user_id", columnList = "userId"),
    @Index(name = "idx_upg_permission_id", columnList = "permissionId"),
    @Index(name = "idx_upg_active", columnList = "active"),
    @Index(name = "idx_upg_expires", columnList = "expiresAt")
})
public class UserPermissionGrant {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id", nullable = false)
    @NotNull
    private Permission permission;
    
    @Column(nullable = false)
    private Boolean active = true;
    
    @Column
    private Instant effectiveFrom = Instant.now();
    
    @Column
    private Instant expiresAt;
    
    @Column
    private String resourceId; // Specific resource instance ID
    
    @Column
    private String constraints; // Additional constraints in JSON format
    
    @Column
    private String grantedBy;
    
    @Column
    private String grantReason;
    
    @Column
    private String approvedBy;
    
    @Column
    private Instant approvedAt;
    
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    
    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
    
    // Constructors
    public UserPermissionGrant() {}
    
    public UserPermissionGrant(User user, Permission permission, String grantedBy) {
        this.user = user;
        this.permission = permission;
        this.grantedBy = grantedBy;
    }
    
    // Business methods
    public boolean isActive() {
        return active && !isExpired();
    }
    
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }
    
    public boolean isEffective() {
        Instant now = Instant.now();
        return (effectiveFrom == null || effectiveFrom.isBefore(now)) && 
               (expiresAt == null || expiresAt.isAfter(now));
    }
    
    public boolean appliesToResource(String resourceIdentifier) {
        return resourceId == null || resourceId.equals(resourceIdentifier);
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public Permission getPermission() { return permission; }
    public void setPermission(Permission permission) { this.permission = permission; }
    
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    
    public Instant getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(Instant effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    
    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }
    
    public String getConstraints() { return constraints; }
    public void setConstraints(String constraints) { this.constraints = constraints; }
    
    public String getGrantedBy() { return grantedBy; }
    public void setGrantedBy(String grantedBy) { this.grantedBy = grantedBy; }
    
    public String getGrantReason() { return grantReason; }
    public void setGrantReason(String grantReason) { this.grantReason = grantReason; }
    
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    
    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}