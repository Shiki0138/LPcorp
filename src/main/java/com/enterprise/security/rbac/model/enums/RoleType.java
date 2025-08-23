package com.enterprise.security.rbac.model.enums;

/**
 * Role type enumeration
 */
public enum RoleType {
    SYSTEM("System", "System-defined role that cannot be modified"),
    BUILT_IN("Built-in", "Pre-defined role with standard permissions"),
    CUSTOM("Custom", "Custom role created by administrators"),
    TEMPLATE("Template", "Template role for creating other roles"),
    FUNCTIONAL("Functional", "Role based on job function"),
    ORGANIZATIONAL("Organizational", "Role based on organizational hierarchy"),
    PROJECT("Project", "Project-specific role"),
    TEMPORARY("Temporary", "Temporary role with expiration");
    
    private final String displayName;
    private final String description;
    
    RoleType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isModifiable() {
        return this != SYSTEM;
    }
    
    public boolean isPredefined() {
        return this == SYSTEM || this == BUILT_IN;
    }
}