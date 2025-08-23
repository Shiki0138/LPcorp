package com.enterprise.security.rbac.model;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Permission entity with resource-based and attribute-based access control support
 */
@Entity
@Table(name = "rbac_permissions", indexes = {
    @Index(name = "idx_permission_name", columnList = "name"),
    @Index(name = "idx_permission_resource", columnList = "resourceType"),
    @Index(name = "idx_permission_tenant", columnList = "tenantId"),
    @Index(name = "idx_permission_category", columnList = "category")
})
public class Permission {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false, length = 200)
    @NotBlank(message = "Permission name is required")
    private String name;
    
    @Column(length = 500)
    private String description;
    
    @Column(nullable = false, length = 100)
    @NotBlank(message = "Resource type is required")
    private String resourceType;
    
    @Column(nullable = false, length = 50)
    @NotBlank(message = "Action is required")
    private String action;
    
    @Column(length = 100)
    private String category;
    
    @Column(nullable = false)
    private String tenantId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PermissionType permissionType = PermissionType.RESOURCE;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PermissionScope scope = PermissionScope.INSTANCE;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PermissionStatus status = PermissionStatus.ACTIVE;
    
    // ABAC attributes
    @Column(columnDefinition = "TEXT")
    private String attributeConstraints; // JSON format
    
    @Column
    private String resourceConstraints; // Resource-specific constraints
    
    @Column
    private String conditionExpression; // SpEL expression for dynamic conditions
    
    // Data classification requirements
    @Enumerated(EnumType.STRING)
    private DataClassification minimumClassification;
    
    @Enumerated(EnumType.STRING)
    private ClearanceLevel requiredClearanceLevel;
    
    // Time and location constraints
    @Embedded
    private TimeRestriction timeRestriction;
    
    @Embedded
    private GeographicRestriction geographicRestriction;
    
    // Role assignments
    @OneToMany(mappedBy = "permission", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<RolePermissionAssignment> roleAssignments = new HashSet<>();
    
    // Direct user grants
    @OneToMany(mappedBy = "permission", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<UserPermissionGrant> userGrants = new HashSet<>();
    
    // Permission templates
    @ManyToMany(mappedBy = "permissions", fetch = FetchType.LAZY)
    private Set<PermissionTemplate> templates = new HashSet<>();
    
    // Risk level and compliance
    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel = RiskLevel.LOW;
    
    @Column
    private String complianceTag;
    
    @Column
    private Boolean auditRequired = false;
    
    @Column
    private Boolean approvalRequired = false;
    
    // Audit fields
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    
    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
    
    @Column
    private String createdBy;
    
    @Column
    private String updatedBy;
    
    // Constructors
    public Permission() {}
    
    public Permission(String name, String resourceType, String action, String tenantId) {
        this.name = name;
        this.resourceType = resourceType;
        this.action = action;
        this.tenantId = tenantId;
    }
    
    // Business methods
    public boolean isActive() {
        return status == PermissionStatus.ACTIVE;
    }
    
    public boolean isSystemPermission() {
        return permissionType == PermissionType.SYSTEM;
    }
    
    public boolean isGlobalScope() {
        return scope == PermissionScope.GLOBAL;
    }
    
    public boolean requiresHighClearance() {
        return requiredClearanceLevel != null && 
               requiredClearanceLevel.ordinal() >= ClearanceLevel.SECRET.ordinal();
    }
    
    public boolean isTimeRestricted() {
        return timeRestriction != null;
    }
    
    public boolean isLocationRestricted() {
        return geographicRestriction != null;
    }
    
    public boolean isHighRisk() {
        return riskLevel == RiskLevel.HIGH || riskLevel == RiskLevel.CRITICAL;
    }
    
    public String getFullName() {
        return resourceType + ":" + action;
    }
    
    public boolean matches(String resource, String actionName) {
        if ("*".equals(this.action) || this.action.equals(actionName)) {
            return "*".equals(this.resourceType) || this.resourceType.equalsIgnoreCase(resource);
        }
        return false;
    }
    
    public boolean canBeGrantedTo(User user) {
        if (!isActive()) {
            return false;
        }
        
        // Check clearance level
        if (requiredClearanceLevel != null && 
            user.getClearanceLevel().ordinal() < requiredClearanceLevel.ordinal()) {
            return false;
        }
        
        // Check data classification clearance
        if (minimumClassification != null) {
            // Implementation depends on user's data access clearance
            // This would typically check against user's classification access levels
        }
        
        return true;
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
    
    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    
    public PermissionType getPermissionType() { return permissionType; }
    public void setPermissionType(PermissionType permissionType) { this.permissionType = permissionType; }
    
    public PermissionScope getScope() { return scope; }
    public void setScope(PermissionScope scope) { this.scope = scope; }
    
    public PermissionStatus getStatus() { return status; }
    public void setStatus(PermissionStatus status) { this.status = status; }
    
    public String getAttributeConstraints() { return attributeConstraints; }
    public void setAttributeConstraints(String attributeConstraints) { this.attributeConstraints = attributeConstraints; }
    
    public String getResourceConstraints() { return resourceConstraints; }
    public void setResourceConstraints(String resourceConstraints) { this.resourceConstraints = resourceConstraints; }
    
    public String getConditionExpression() { return conditionExpression; }
    public void setConditionExpression(String conditionExpression) { this.conditionExpression = conditionExpression; }
    
    public DataClassification getMinimumClassification() { return minimumClassification; }
    public void setMinimumClassification(DataClassification minimumClassification) { this.minimumClassification = minimumClassification; }
    
    public ClearanceLevel getRequiredClearanceLevel() { return requiredClearanceLevel; }
    public void setRequiredClearanceLevel(ClearanceLevel requiredClearanceLevel) { this.requiredClearanceLevel = requiredClearanceLevel; }
    
    public TimeRestriction getTimeRestriction() { return timeRestriction; }
    public void setTimeRestriction(TimeRestriction timeRestriction) { this.timeRestriction = timeRestriction; }
    
    public GeographicRestriction getGeographicRestriction() { return geographicRestriction; }
    public void setGeographicRestriction(GeographicRestriction geographicRestriction) { this.geographicRestriction = geographicRestriction; }
    
    public Set<RolePermissionAssignment> getRoleAssignments() { return roleAssignments; }
    public void setRoleAssignments(Set<RolePermissionAssignment> roleAssignments) { this.roleAssignments = roleAssignments; }
    
    public Set<UserPermissionGrant> getUserGrants() { return userGrants; }
    public void setUserGrants(Set<UserPermissionGrant> userGrants) { this.userGrants = userGrants; }
    
    public Set<PermissionTemplate> getTemplates() { return templates; }
    public void setTemplates(Set<PermissionTemplate> templates) { this.templates = templates; }
    
    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }
    
    public String getComplianceTag() { return complianceTag; }
    public void setComplianceTag(String complianceTag) { this.complianceTag = complianceTag; }
    
    public Boolean getAuditRequired() { return auditRequired; }
    public void setAuditRequired(Boolean auditRequired) { this.auditRequired = auditRequired; }
    
    public Boolean getApprovalRequired() { return approvalRequired; }
    public void setApprovalRequired(Boolean approvalRequired) { this.approvalRequired = approvalRequired; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Permission)) return false;
        Permission that = (Permission) o;
        return id != null && id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
    
    @Override
    public String toString() {
        return "Permission{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", resourceType='" + resourceType + '\'' +
                ", action='" + action + '\'' +
                '}';
    }
}