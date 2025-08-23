package com.enterprise.security.mfa.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "mfa_devices")
public class MfaDevice {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(nullable = false)
    private String userId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MfaType type;
    
    @Column(nullable = false)
    private String deviceName;
    
    @Column(columnDefinition = "TEXT")
    private String encryptedSecret;
    
    @Column
    private String deviceIdentifier; // For WebAuthn credential ID
    
    @Column(columnDefinition = "TEXT")
    private String publicKey; // For WebAuthn public key
    
    @Column
    private Long signatureCount; // For WebAuthn
    
    @Column
    private String phoneNumber; // For SMS
    
    @Column
    private String email; // For email-based MFA
    
    @Column(nullable = false)
    private boolean isActive = true;
    
    @Column(nullable = false)
    private boolean isVerified = false;
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column
    private LocalDateTime lastUsedAt;
    
    @Column
    private LocalDateTime verifiedAt;
    
    // Constructors
    public MfaDevice() {}
    
    public MfaDevice(String userId, MfaType type, String deviceName) {
        this.userId = userId;
        this.type = type;
        this.deviceName = deviceName;
    }
    
    // Getters and Setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public MfaType getType() {
        return type;
    }
    
    public void setType(MfaType type) {
        this.type = type;
    }
    
    public String getDeviceName() {
        return deviceName;
    }
    
    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }
    
    public String getEncryptedSecret() {
        return encryptedSecret;
    }
    
    public void setEncryptedSecret(String encryptedSecret) {
        this.encryptedSecret = encryptedSecret;
    }
    
    public String getDeviceIdentifier() {
        return deviceIdentifier;
    }
    
    public void setDeviceIdentifier(String deviceIdentifier) {
        this.deviceIdentifier = deviceIdentifier;
    }
    
    public String getPublicKey() {
        return publicKey;
    }
    
    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }
    
    public Long getSignatureCount() {
        return signatureCount;
    }
    
    public void setSignatureCount(Long signatureCount) {
        this.signatureCount = signatureCount;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    public boolean isVerified() {
        return isVerified;
    }
    
    public void setVerified(boolean verified) {
        isVerified = verified;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }
    
    public void setLastUsedAt(LocalDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }
    
    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }
    
    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }
    
    // Utility methods
    public void markAsUsed() {
        this.lastUsedAt = LocalDateTime.now();
    }
    
    public void markAsVerified() {
        this.isVerified = true;
        this.verifiedAt = LocalDateTime.now();
    }
    
    public boolean isWebAuthn() {
        return type == MfaType.WEBAUTHN_PLATFORM || type == MfaType.WEBAUTHN_CROSS_PLATFORM;
    }
    
    public boolean isTOTP() {
        return type == MfaType.TOTP;
    }
    
    public boolean isSMS() {
        return type == MfaType.SMS;
    }
    
    public boolean isEmail() {
        return type == MfaType.EMAIL;
    }
}