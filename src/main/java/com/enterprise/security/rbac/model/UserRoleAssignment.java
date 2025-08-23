package com.enterprise.security.rbac.model;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.Instant;

/**
 * User-Role assignment entity with context and time constraints
 */
@Entity
@Table(name = "rbac_user_role_assignments", indexes = {
    @Index(name = "idx_ura_user_id", columnList = "userId"),
    @Index(name = "idx_ura_role_id", columnList = "roleId"),
    @Index(name = "idx_ura_active", columnList = "active"),
    @Index(name = "idx_ura_expires", columnList = "expiresAt")
})
public class UserRoleAssignment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    @NotNull
    private Role role;
    
    @Column(nullable = false)
    private Boolean active = true;
    
    @Column
    private Instant effectiveFrom = Instant.now();
    
    @Column
    private Instant expiresAt;
    
    @Column
    private String context; // Assignment context (project, department, etc.)
    
    @Column
    private String assignedBy;
    
    @Column
    private String assignmentReason;
    
    @Column
    private String approvedBy;
    
    @Column
    private Instant approvedAt;
    
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    
    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
    
    // Constructors
    public UserRoleAssignment() {}
    
    public UserRoleAssignment(User user, Role role, String assignedBy) {
        this.user = user;
        this.role = role;
        this.assignedBy = assignedBy;
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
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    
    public Instant getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(Instant effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    
    public String getContext() { return context; }
    public void setContext(String context) { this.context = context; }
    
    public String getAssignedBy() { return assignedBy; }
    public void setAssignedBy(String assignedBy) { this.assignedBy = assignedBy; }
    
    public String getAssignmentReason() { return assignmentReason; }
    public void setAssignmentReason(String assignmentReason) { this.assignmentReason = assignmentReason; }
    
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    
    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}