package com.enterprise.security.rbac.model;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Permission template for creating permission sets
 */
@Entity
@Table(name = "rbac_permission_templates", indexes = {
    @Index(name = "idx_pt_name", columnList = "name"),
    @Index(name = "idx_pt_category", columnList = "category"),
    @Index(name = "idx_pt_tenant", columnList = "tenantId")
})
public class PermissionTemplate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false, length = 200)
    @NotBlank(message = "Template name is required")
    private String name;
    
    @Column(length = 500)
    private String description;
    
    @Column(length = 100)
    private String category;
    
    @Column(nullable = false)
    private String tenantId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TemplateStatus status = TemplateStatus.ACTIVE;
    
    // Permissions included in this template
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "rbac_template_permissions",
        joinColumns = @JoinColumn(name = "template_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();
    
    @Column
    private String templateVersion = "1.0";
    
    @Column
    private String basedOnTemplateId; // For template inheritance
    
    @Column
    private Boolean isSystemTemplate = false;
    
    @Column
    private String targetRole; // Intended role type for this template
    
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    
    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
    
    @Column
    private String createdBy;
    
    @Column
    private String updatedBy;
    
    // Constructors
    public PermissionTemplate() {}
    
    public PermissionTemplate(String name, String description, String tenantId) {
        this.name = name;
        this.description = description;
        this.tenantId = tenantId;
    }
    
    // Business methods
    public boolean isActive() {
        return status == TemplateStatus.ACTIVE;
    }
    
    public boolean isSystemTemplate() {
        return Boolean.TRUE.equals(isSystemTemplate);
    }
    
    public boolean hasPermissions() {
        return !permissions.isEmpty();
    }
    
    public void addPermission(Permission permission) {
        permissions.add(permission);
    }
    
    public void removePermission(Permission permission) {
        permissions.remove(permission);
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    
    public TemplateStatus getStatus() { return status; }
    public void setStatus(TemplateStatus status) { this.status = status; }
    
    public Set<Permission> getPermissions() { return permissions; }
    public void setPermissions(Set<Permission> permissions) { this.permissions = permissions; }
    
    public String getTemplateVersion() { return templateVersion; }
    public void setTemplateVersion(String templateVersion) { this.templateVersion = templateVersion; }
    
    public String getBasedOnTemplateId() { return basedOnTemplateId; }
    public void setBasedOnTemplateId(String basedOnTemplateId) { this.basedOnTemplateId = basedOnTemplateId; }
    
    public Boolean getIsSystemTemplate() { return isSystemTemplate; }
    public void setIsSystemTemplate(Boolean isSystemTemplate) { this.isSystemTemplate = isSystemTemplate; }
    
    public String getTargetRole() { return targetRole; }
    public void setTargetRole(String targetRole) { this.targetRole = targetRole; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}

enum TemplateStatus {
    ACTIVE("Active", "Template is active and can be used"),
    INACTIVE("Inactive", "Template is inactive"),
    DEPRECATED("Deprecated", "Template is deprecated"),
    DRAFT("Draft", "Template is in draft state");
    
    private final String displayName;
    private final String description;
    
    TemplateStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}