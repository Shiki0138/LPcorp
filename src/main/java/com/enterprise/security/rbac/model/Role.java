package com.enterprise.security.rbac.model;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Role entity supporting hierarchical roles and inheritance
 */
@Entity
@Table(name = "rbac_roles", indexes = {
    @Index(name = "idx_role_name", columnList = "name"),
    @Index(name = "idx_role_tenant", columnList = "tenantId"),
    @Index(name = "idx_role_type", columnList = "roleType"),
    @Index(name = "idx_role_status", columnList = "status")
})
public class Role {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false, length = 100)
    @NotBlank(message = "Role name is required")
    private String name;
    
    @Column(length = 500)
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleType roleType = RoleType.CUSTOM;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleStatus status = RoleStatus.ACTIVE;
    
    @Column(nullable = false)
    private String tenantId;
    
    @Column
    private String departmentId;
    
    @Column
    private Integer hierarchyLevel = 0;
    
    // Role hierarchy - parent roles
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "rbac_role_hierarchy",
        joinColumns = @JoinColumn(name = "child_role_id"),
        inverseJoinColumns = @JoinColumn(name = "parent_role_id")
    )
    private Set<Role> parentRoles = new HashSet<>();
    
    // Role hierarchy - child roles
    @ManyToMany(mappedBy = "parentRoles", fetch = FetchType.LAZY)
    private Set<Role> childRoles = new HashSet<>();
    
    // Permission assignments
    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<RolePermissionAssignment> permissionAssignments = new HashSet<>();
    
    // User assignments
    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<UserRoleAssignment> userAssignments = new HashSet<>();
    
    // Template configuration
    @Embedded
    private RoleTemplate template;
    
    // Constraints and restrictions
    @Column
    private Integer maxUsers;
    
    @Column
    private Integer maxConcurrentSessions;
    
    @Embedded
    private TimeRestriction timeRestriction;
    
    @Embedded
    private GeographicRestriction geographicRestriction;
    
    // Approval workflow
    @Column
    private Boolean requiresApproval = false;
    
    @Column
    private String approverRoleId;
    
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
    public Role() {}
    
    public Role(String name, String description, RoleType roleType, String tenantId) {
        this.name = name;
        this.description = description;
        this.roleType = roleType;
        this.tenantId = tenantId;
    }
    
    // Business methods
    public boolean isActive() {
        return status == RoleStatus.ACTIVE;
    }
    
    public boolean isSystemRole() {
        return roleType == RoleType.SYSTEM;
    }
    
    public boolean hasPermission(String permissionName) {
        return getAllPermissions().stream()
            .anyMatch(p -> p.getName().equals(permissionName));
    }
    
    public Set<Permission> getDirectPermissions() {
        return permissionAssignments.stream()
            .filter(RolePermissionAssignment::isActive)
            .map(RolePermissionAssignment::getPermission)
            .collect(Collectors.toSet());
    }
    
    public Set<Permission> getAllPermissions() {
        Set<Permission> allPermissions = new HashSet<>(getDirectPermissions());
        
        // Add permissions from parent roles (inheritance)
        for (Role parentRole : parentRoles) {
            if (parentRole.isActive()) {
                allPermissions.addAll(parentRole.getAllPermissions());
            }
        }
        
        return allPermissions;
    }
    
    public Set<Role> getAllParentRoles() {
        Set<Role> allParents = new HashSet<>();
        collectParentRoles(allParents);
        return allParents;
    }
    
    private void collectParentRoles(Set<Role> allParents) {
        for (Role parent : parentRoles) {
            if (!allParents.contains(parent)) {
                allParents.add(parent);
                parent.collectParentRoles(allParents);
            }
        }
    }
    
    public boolean isDescendantOf(Role ancestorRole) {
        return getAllParentRoles().contains(ancestorRole);
    }
    
    public boolean canBeAssignedToUser(User user) {
        if (!isActive()) {
            return false;
        }
        
        // Check max users constraint
        if (maxUsers != null && userAssignments.size() >= maxUsers) {
            return false;
        }
        
        // Check department restriction
        if (departmentId != null && !departmentId.equals(user.getDepartmentId())) {
            return false;
        }
        
        // Check clearance level
        if (template != null && template.getRequiredClearanceLevel() != null) {
            if (user.getClearanceLevel().ordinal() < template.getRequiredClearanceLevel().ordinal()) {
                return false;
            }
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
    
    public RoleType getRoleType() { return roleType; }
    public void setRoleType(RoleType roleType) { this.roleType = roleType; }
    
    public RoleStatus getStatus() { return status; }
    public void setStatus(RoleStatus status) { this.status = status; }
    
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    
    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }
    
    public Integer getHierarchyLevel() { return hierarchyLevel; }
    public void setHierarchyLevel(Integer hierarchyLevel) { this.hierarchyLevel = hierarchyLevel; }
    
    public Set<Role> getParentRoles() { return parentRoles; }
    public void setParentRoles(Set<Role> parentRoles) { this.parentRoles = parentRoles; }
    
    public Set<Role> getChildRoles() { return childRoles; }
    public void setChildRoles(Set<Role> childRoles) { this.childRoles = childRoles; }
    
    public Set<RolePermissionAssignment> getPermissionAssignments() { return permissionAssignments; }
    public void setPermissionAssignments(Set<RolePermissionAssignment> permissionAssignments) { this.permissionAssignments = permissionAssignments; }
    
    public Set<UserRoleAssignment> getUserAssignments() { return userAssignments; }
    public void setUserAssignments(Set<UserRoleAssignment> userAssignments) { this.userAssignments = userAssignments; }
    
    public RoleTemplate getTemplate() { return template; }
    public void setTemplate(RoleTemplate template) { this.template = template; }
    
    public Integer getMaxUsers() { return maxUsers; }
    public void setMaxUsers(Integer maxUsers) { this.maxUsers = maxUsers; }
    
    public Integer getMaxConcurrentSessions() { return maxConcurrentSessions; }
    public void setMaxConcurrentSessions(Integer maxConcurrentSessions) { this.maxConcurrentSessions = maxConcurrentSessions; }
    
    public TimeRestriction getTimeRestriction() { return timeRestriction; }
    public void setTimeRestriction(TimeRestriction timeRestriction) { this.timeRestriction = timeRestriction; }
    
    public GeographicRestriction getGeographicRestriction() { return geographicRestriction; }
    public void setGeographicRestriction(GeographicRestriction geographicRestriction) { this.geographicRestriction = geographicRestriction; }
    
    public Boolean getRequiresApproval() { return requiresApproval; }
    public void setRequiresApproval(Boolean requiresApproval) { this.requiresApproval = requiresApproval; }
    
    public String getApproverRoleId() { return approverRoleId; }
    public void setApproverRoleId(String approverRoleId) { this.approverRoleId = approverRoleId; }
    
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
        if (!(o instanceof Role)) return false;
        Role role = (Role) o;
        return id != null && id.equals(role.id);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
    
    @Override
    public String toString() {
        return "Role{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", roleType=" + roleType +
                ", tenantId='" + tenantId + '\'' +
                '}';
    }
}