package com.enterprise.security.rbac.model.enums;

/**
 * Emergency access status enumeration
 */
public enum EmergencyAccessStatus {
    PENDING("Pending", "Emergency access request is pending approval"),
    ACTIVE("Active", "Emergency access is active"),
    REJECTED("Rejected", "Emergency access request was rejected"),
    REVOKED("Revoked", "Emergency access was revoked"),
    EXPIRED("Expired", "Emergency access has expired");
    
    private final String displayName;
    private final String description;
    
    EmergencyAccessStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isActive() {
        return this == ACTIVE;
    }
    
    public boolean isFinal() {
        return this == REJECTED || this == REVOKED || this == EXPIRED;
    }
}