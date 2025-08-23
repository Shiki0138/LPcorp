package com.enterprise.security.rbac.model;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Set;
import java.util.HashSet;

/**
 * User delegation entity for permission delegation
 */
@Entity
@Table(name = "rbac_user_delegations", indexes = {
    @Index(name = "idx_ud_delegator", columnList = "delegatorId"),
    @Index(name = "idx_ud_delegate", columnList = "delegateId"),
    @Index(name = "idx_ud_active", columnList = "active"),
    @Index(name = "idx_ud_expires", columnList = "expiresAt")
})
public class UserDelegation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delegator_id", nullable = false)
    @NotNull
    private User delegator;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delegate_id", nullable = false)
    @NotNull
    private User delegate;
    
    @Column(nullable = false)
    private Boolean active = true;
    
    @Column
    private Instant effectiveFrom = Instant.now();
    
    @Column
    private Instant expiresAt;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DelegationType delegationType = DelegationType.PARTIAL;
    
    // Specific permissions to delegate (if partial delegation)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "rbac_delegation_permissions",
        joinColumns = @JoinColumn(name = "delegation_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> delegatedPermissions = new HashSet<>();
    
    // Specific roles to delegate (if role-based delegation)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "rbac_delegation_roles",
        joinColumns = @JoinColumn(name = "delegation_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> delegatedRoles = new HashSet<>();
    
    @Column
    private String context; // Delegation context
    
    @Column
    private String constraints; // Constraints in JSON format
    
    @Column
    private String delegationReason;
    
    @Column
    private String approvedBy;
    
    @Column
    private Instant approvedAt;
    
    @Column(nullable = false)
    private Boolean canDelegate = false; // Can the delegate further delegate
    
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    
    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
    
    // Constructors
    public UserDelegation() {}
    
    public UserDelegation(User delegator, User delegate, DelegationType type) {
        this.delegator = delegator;
        this.delegate = delegate;
        this.delegationType = type;
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
    
    public boolean isFullDelegation() {
        return delegationType == DelegationType.FULL;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public User getDelegator() { return delegator; }
    public void setDelegator(User delegator) { this.delegator = delegator; }
    
    public User getDelegate() { return delegate; }
    public void setDelegate(User delegate) { this.delegate = delegate; }
    
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    
    public Instant getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(Instant effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    
    public DelegationType getDelegationType() { return delegationType; }
    public void setDelegationType(DelegationType delegationType) { this.delegationType = delegationType; }
    
    public Set<Permission> getDelegatedPermissions() { return delegatedPermissions; }
    public void setDelegatedPermissions(Set<Permission> delegatedPermissions) { this.delegatedPermissions = delegatedPermissions; }
    
    public Set<Role> getDelegatedRoles() { return delegatedRoles; }
    public void setDelegatedRoles(Set<Role> delegatedRoles) { this.delegatedRoles = delegatedRoles; }
    
    public String getContext() { return context; }
    public void setContext(String context) { this.context = context; }
    
    public String getConstraints() { return constraints; }
    public void setConstraints(String constraints) { this.constraints = constraints; }
    
    public String getDelegationReason() { return delegationReason; }
    public void setDelegationReason(String delegationReason) { this.delegationReason = delegationReason; }
    
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    
    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }
    
    public Boolean getCanDelegate() { return canDelegate; }
    public void setCanDelegate(Boolean canDelegate) { this.canDelegate = canDelegate; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

enum DelegationType {
    FULL("Full", "Full authority delegation"),
    PARTIAL("Partial", "Partial permission delegation"),
    ROLE_BASED("Role-based", "Role-based delegation"),
    TEMPORARY("Temporary", "Temporary delegation with expiry");
    
    private final String displayName;
    private final String description;
    
    DelegationType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}