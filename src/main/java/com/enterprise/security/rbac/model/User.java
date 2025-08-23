package com.enterprise.security.rbac.model;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * User entity for RBAC system with comprehensive attribute support
 */
@Entity
@Table(name = "rbac_users", indexes = {
    @Index(name = "idx_user_username", columnList = "username"),
    @Index(name = "idx_user_email", columnList = "email"),
    @Index(name = "idx_user_tenant", columnList = "tenantId"),
    @Index(name = "idx_user_status", columnList = "status")
})
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(unique = true, nullable = false, length = 100)
    @NotBlank(message = "Username is required")
    private String username;
    
    @Column(unique = true, nullable = false, length = 255)
    @Email(message = "Valid email is required")
    private String email;
    
    @Column(nullable = false, length = 255)
    @NotBlank(message = "First name is required")
    private String firstName;
    
    @Column(nullable = false, length = 255)
    @NotBlank(message = "Last name is required")
    private String lastName;
    
    @JsonIgnore
    @Column(nullable = false)
    private String passwordHash;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;
    
    @Column(nullable = false)
    private String tenantId;
    
    @Column
    private String departmentId;
    
    @Column
    private String managerId;
    
    @Enumerated(EnumType.STRING)
    private ClearanceLevel clearanceLevel = ClearanceLevel.STANDARD;
    
    @Column
    private String location;
    
    @Column
    private String costCenter;
    
    @Column
    private String employeeNumber;
    
    @Column
    private String jobTitle;
    
    // Time restrictions
    @Embedded
    private TimeRestriction timeRestriction;
    
    // Geographic restrictions
    @Embedded
    private GeographicRestriction geographicRestriction;
    
    // Role assignments with contexts
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<UserRoleAssignment> roleAssignments = new HashSet<>();
    
    // Direct permission grants
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<UserPermissionGrant> directPermissions = new HashSet<>();
    
    // Delegation relationships
    @OneToMany(mappedBy = "delegator", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<UserDelegation> delegatedPermissions = new HashSet<>();
    
    @OneToMany(mappedBy = "delegate", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<UserDelegation> receivedDelegations = new HashSet<>();
    
    // Audit fields
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    
    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
    
    @Column
    private Instant lastLoginAt;
    
    @Column
    private Instant passwordChangedAt;
    
    @Column
    private Boolean mfaEnabled = false;
    
    @Column
    private Integer failedLoginAttempts = 0;
    
    @Column
    private Instant lockedUntil;
    
    // Constructors
    public User() {}
    
    public User(String username, String email, String firstName, String lastName, String tenantId) {
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.tenantId = tenantId;
    }
    
    // Business methods
    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }
    
    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(Instant.now());
    }
    
    public boolean hasRole(String roleName) {
        return roleAssignments.stream()
            .anyMatch(assignment -> assignment.getRole().getName().equals(roleName) && 
                     assignment.isActive());
    }
    
    public boolean hasPermission(String permission) {
        return directPermissions.stream()
            .anyMatch(grant -> grant.getPermission().getName().equals(permission) && 
                      grant.isActive());
    }
    
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    public Set<Role> getActiveRoles() {
        return roleAssignments.stream()
            .filter(UserRoleAssignment::isActive)
            .map(UserRoleAssignment::getRole)
            .collect(java.util.stream.Collectors.toSet());
    }
    
    public Set<Permission> getDirectPermissions() {
        return directPermissions.stream()
            .filter(UserPermissionGrant::isActive)
            .map(UserPermissionGrant::getPermission)
            .collect(java.util.stream.Collectors.toSet());
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    
    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }
    
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    
    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }
    
    public String getManagerId() { return managerId; }
    public void setManagerId(String managerId) { this.managerId = managerId; }
    
    public ClearanceLevel getClearanceLevel() { return clearanceLevel; }
    public void setClearanceLevel(ClearanceLevel clearanceLevel) { this.clearanceLevel = clearanceLevel; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public String getCostCenter() { return costCenter; }
    public void setCostCenter(String costCenter) { this.costCenter = costCenter; }
    
    public String getEmployeeNumber() { return employeeNumber; }
    public void setEmployeeNumber(String employeeNumber) { this.employeeNumber = employeeNumber; }
    
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    
    public TimeRestriction getTimeRestriction() { return timeRestriction; }
    public void setTimeRestriction(TimeRestriction timeRestriction) { this.timeRestriction = timeRestriction; }
    
    public GeographicRestriction getGeographicRestriction() { return geographicRestriction; }
    public void setGeographicRestriction(GeographicRestriction geographicRestriction) { this.geographicRestriction = geographicRestriction; }
    
    public Set<UserRoleAssignment> getRoleAssignments() { return roleAssignments; }
    public void setRoleAssignments(Set<UserRoleAssignment> roleAssignments) { this.roleAssignments = roleAssignments; }
    
    public Set<UserPermissionGrant> getDirectPermissionGrants() { return directPermissions; }
    public void setDirectPermissionGrants(Set<UserPermissionGrant> directPermissions) { this.directPermissions = directPermissions; }
    
    public Set<UserDelegation> getDelegatedPermissions() { return delegatedPermissions; }
    public void setDelegatedPermissions(Set<UserDelegation> delegatedPermissions) { this.delegatedPermissions = delegatedPermissions; }
    
    public Set<UserDelegation> getReceivedDelegations() { return receivedDelegations; }
    public void setReceivedDelegations(Set<UserDelegation> receivedDelegations) { this.receivedDelegations = receivedDelegations; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    
    public Instant getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Instant lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    
    public Instant getPasswordChangedAt() { return passwordChangedAt; }
    public void setPasswordChangedAt(Instant passwordChangedAt) { this.passwordChangedAt = passwordChangedAt; }
    
    public Boolean getMfaEnabled() { return mfaEnabled; }
    public void setMfaEnabled(Boolean mfaEnabled) { this.mfaEnabled = mfaEnabled; }
    
    public Integer getFailedLoginAttempts() { return failedLoginAttempts; }
    public void setFailedLoginAttempts(Integer failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }
    
    public Instant getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(Instant lockedUntil) { this.lockedUntil = lockedUntil; }
}