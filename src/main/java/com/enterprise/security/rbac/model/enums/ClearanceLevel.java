package com.enterprise.security.rbac.model.enums;

/**
 * Security clearance level enumeration
 */
public enum ClearanceLevel {
    PUBLIC(0, "Public", "Public access level"),
    STANDARD(1, "Standard", "Standard employee access level"),
    ELEVATED(2, "Elevated", "Elevated access for supervisors"),
    CONFIDENTIAL(3, "Confidential", "Confidential information access"),
    SECRET(4, "Secret", "Secret information access"),
    TOP_SECRET(5, "Top Secret", "Top secret information access");
    
    private final int level;
    private final String displayName;
    private final String description;
    
    ClearanceLevel(int level, String displayName, String description) {
        this.level = level;
        this.displayName = displayName;
        this.description = description;
    }
    
    public int getLevel() {
        return level;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isAtLeast(ClearanceLevel other) {
        return this.level >= other.level;
    }
    
    public boolean isHigherThan(ClearanceLevel other) {
        return this.level > other.level;
    }
    
    public boolean canAccess(ClearanceLevel requiredLevel) {
        return isAtLeast(requiredLevel);
    }
}