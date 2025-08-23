package com.enterprise.security.rbac.model.enums;

/**
 * Role status enumeration
 */
public enum RoleStatus {
    ACTIVE("Active", "Role is active and can be assigned"),
    INACTIVE("Inactive", "Role is inactive and cannot be assigned"),
    DEPRECATED("Deprecated", "Role is deprecated and should not be used for new assignments"),
    SUSPENDED("Suspended", "Role is temporarily suspended"),
    PENDING_APPROVAL("Pending Approval", "Role is pending approval"),
    DRAFT("Draft", "Role is in draft state");
    
    private final String displayName;
    private final String description;
    
    RoleStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isAssignable() {
        return this == ACTIVE;
    }
    
    public boolean isUsable() {
        return this == ACTIVE || this == DEPRECATED;
    }
}