package com.enterprise.security.rbac.model.enums;

/**
 * Permission type enumeration
 */
public enum PermissionType {
    SYSTEM("System", "System-level permission"),
    RESOURCE("Resource", "Resource-based permission"),
    FUNCTIONAL("Functional", "Functional permission for specific operations"),
    DATA("Data", "Data access permission"),
    API("API", "API access permission"),
    UI("UI", "User interface permission"),
    ADMINISTRATIVE("Administrative", "Administrative permission");
    
    private final String displayName;
    private final String description;
    
    PermissionType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isSystemLevel() {
        return this == SYSTEM || this == ADMINISTRATIVE;
    }
}