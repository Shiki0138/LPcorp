package com.enterprise.security.rbac.model;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.Instant;

/**
 * Role-Permission assignment entity
 */
@Entity
@Table(name = "rbac_role_permission_assignments", indexes = {
    @Index(name = "idx_rpa_role_id", columnList = "roleId"),
    @Index(name = "idx_rpa_permission_id", columnList = "permissionId"),
    @Index(name = "idx_rpa_active", columnList = "active")
})
public class RolePermissionAssignment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    @NotNull
    private Role role;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id", nullable = false)
    @NotNull
    private Permission permission;
    
    @Column(nullable = false)
    private Boolean active = true;
    
    @Column
    private String constraints; // Additional constraints in JSON format
    
    @Column
    private String assignedBy;
    
    @Column
    private String assignmentReason;
    
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    
    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
    
    // Constructors
    public RolePermissionAssignment() {}
    
    public RolePermissionAssignment(Role role, Permission permission, String assignedBy) {
        this.role = role;
        this.permission = permission;
        this.assignedBy = assignedBy;
    }
    
    // Business methods
    public boolean isActive() {
        return active;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    
    public Permission getPermission() { return permission; }
    public void setPermission(Permission permission) { this.permission = permission; }
    
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    
    public String getConstraints() { return constraints; }
    public void setConstraints(String constraints) { this.constraints = constraints; }
    
    public String getAssignedBy() { return assignedBy; }
    public void setAssignedBy(String assignedBy) { this.assignedBy = assignedBy; }
    
    public String getAssignmentReason() { return assignmentReason; }
    public void setAssignmentReason(String assignmentReason) { this.assignmentReason = assignmentReason; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}