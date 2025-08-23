package com.enterprise.security.mfa.model;

public enum MfaType {
    TOTP("Time-based One-Time Password", true),
    WEBAUTHN_PLATFORM("WebAuthn Platform Authenticator", true),
    WEBAUTHN_CROSS_PLATFORM("WebAuthn Cross-Platform Authenticator", true),
    SMS("SMS Code", false),
    EMAIL("Email Code", false),
    BACKUP_CODES("Backup Recovery Codes", false);
    
    private final String displayName;
    private final boolean isPhishingResistant;
    
    MfaType(String displayName, boolean isPhishingResistant) {
        this.displayName = displayName;
        this.isPhishingResistant = isPhishingResistant;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public boolean isPhishingResistant() {
        return isPhishingResistant;
    }
    
    public boolean requiresDeviceRegistration() {
        return this == TOTP || this == WEBAUTHN_PLATFORM || this == WEBAUTHN_CROSS_PLATFORM;
    }
    
    public boolean isWebAuthn() {
        return this == WEBAUTHN_PLATFORM || this == WEBAUTHN_CROSS_PLATFORM;
    }
    
    public boolean supportsBiometrics() {
        return this == WEBAUTHN_PLATFORM;
    }
}