package com.enterprise.security.rbac.model.enums;

/**
 * User status enumeration
 */
public enum UserStatus {
    ACTIVE("Active", "User is active and can access the system"),
    INACTIVE("Inactive", "User is inactive and cannot access the system"),
    SUSPENDED("Suspended", "User is temporarily suspended"),
    LOCKED("Locked", "User account is locked due to security reasons"),
    PENDING_ACTIVATION("Pending Activation", "User account is pending activation"),
    PENDING_APPROVAL("Pending Approval", "User account is pending approval"),
    EXPIRED("Expired", "User account has expired"),
    DISABLED("Disabled", "User account has been disabled");
    
    private final String displayName;
    private final String description;
    
    UserStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isAccessible() {
        return this == ACTIVE;
    }
    
    public boolean isPending() {
        return this == PENDING_ACTIVATION || this == PENDING_APPROVAL;
    }
    
    public boolean isBlocked() {
        return this == SUSPENDED || this == LOCKED || this == DISABLED;
    }
}