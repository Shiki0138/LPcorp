package com.enterprise.security.rbac.model.enums;

/**
 * Permission status enumeration
 */
public enum PermissionStatus {
    ACTIVE("Active", "Permission is active and can be granted"),
    INACTIVE("Inactive", "Permission is inactive"),
    DEPRECATED("Deprecated", "Permission is deprecated and should not be used"),
    SUSPENDED("Suspended", "Permission is temporarily suspended"),
    PENDING_APPROVAL("Pending Approval", "Permission is pending approval");
    
    private final String displayName;
    private final String description;
    
    PermissionStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isGrantable() {
        return this == ACTIVE;
    }
    
    public boolean isUsable() {
        return this == ACTIVE || this == DEPRECATED;
    }
}