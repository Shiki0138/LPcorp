package com.enterprise.security.rbac.model.enums;

/**
 * Permission scope enumeration
 */
public enum PermissionScope {
    GLOBAL("Global", "Global scope across all tenants and resources"),
    TENANT("Tenant", "Tenant-wide scope"),
    DEPARTMENT("Department", "Department-level scope"),
    PROJECT("Project", "Project-level scope"),
    INSTANCE("Instance", "Specific resource instance scope"),
    OWNED("Owned", "Only resources owned by the user"),
    DELEGATED("Delegated", "Resources delegated to the user");
    
    private final String displayName;
    private final String description;
    
    PermissionScope(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isGlobalScope() {
        return this == GLOBAL;
    }
    
    public boolean isRestrictedScope() {
        return this == INSTANCE || this == OWNED;
    }
}