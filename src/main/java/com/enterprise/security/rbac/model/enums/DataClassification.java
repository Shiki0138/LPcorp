package com.enterprise.security.rbac.model.enums;

/**
 * Data classification enumeration
 */
public enum DataClassification {
    PUBLIC(0, "Public", "Information that can be freely shared"),
    INTERNAL(1, "Internal", "Information for internal use only"),
    CONFIDENTIAL(2, "Confidential", "Confidential business information"),
    RESTRICTED(3, "Restricted", "Highly sensitive restricted information"),
    TOP_SECRET(4, "Top Secret", "Highest level classified information");
    
    private final int level;
    private final String displayName;
    private final String description;
    
    DataClassification(int level, String displayName, String description) {
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
    
    public boolean isHigherThan(DataClassification other) {
        return this.level > other.level;
    }
    
    public boolean isAtLeast(DataClassification other) {
        return this.level >= other.level;
    }
    
    public boolean requiresSpecialHandling() {
        return this.level >= CONFIDENTIAL.level;
    }
}