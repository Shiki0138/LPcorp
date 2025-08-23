package com.enterprise.security.rbac.model.enums;

/**
 * Risk level enumeration
 */
public enum RiskLevel {
    LOW(0, "Low", "Low risk operation"),
    MEDIUM(1, "Medium", "Medium risk operation"),
    HIGH(2, "High", "High risk operation requiring additional controls"),
    CRITICAL(3, "Critical", "Critical risk operation requiring maximum controls");
    
    private final int level;
    private final String displayName;
    private final String description;
    
    RiskLevel(int level, String displayName, String description) {
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
    
    public boolean requiresApproval() {
        return this.level >= HIGH.level;
    }
    
    public boolean requiresAudit() {
        return this.level >= MEDIUM.level;
    }
    
    public boolean requiresMFA() {
        return this.level >= HIGH.level;
    }
}