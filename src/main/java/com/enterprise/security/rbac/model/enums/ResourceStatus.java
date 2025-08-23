package com.enterprise.security.rbac.model.enums;

/**
 * Resource status enumeration
 */
public enum ResourceStatus {
    ACTIVE("Active", "Resource is active and accessible"),
    INACTIVE("Inactive", "Resource is inactive"),
    ARCHIVED("Archived", "Resource is archived"),
    DELETED("Deleted", "Resource is marked for deletion"),
    MAINTENANCE("Maintenance", "Resource is under maintenance"),
    QUARANTINED("Quarantined", "Resource is quarantined for security reasons");
    
    private final String displayName;
    private final String description;
    
    ResourceStatus(String displayName, String description) {
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
    
    public boolean isUnavailable() {
        return this == MAINTENANCE || this == QUARANTINED || this == DELETED;
    }
}