package com.enterprise.security.mfa.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "trusted_devices")
public class TrustedDevice {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(nullable = false)
    private String userId;
    
    @Column(nullable = false, unique = true)
    private String deviceFingerprint;
    
    @Column(nullable = false)
    private String deviceName;
    
    @Column
    private String userAgent;
    
    @Column
    private String ipAddress;
    
    @Column
    private String location;
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(nullable = false)
    private LocalDateTime expiresAt;
    
    @Column
    private LocalDateTime lastUsedAt;
    
    @Column(nullable = false)
    private boolean isActive = true;
    
    @Column
    private String revokedBy;
    
    @Column
    private LocalDateTime revokedAt;
    
    @Column
    private String revokeReason;
    
    // Constructors
    public TrustedDevice() {}
    
    public TrustedDevice(String userId, String deviceFingerprint, String deviceName, int trustDurationDays) {
        this.userId = userId;
        this.deviceFingerprint = deviceFingerprint;
        this.deviceName = deviceName;
        this.expiresAt = LocalDateTime.now().plusDays(trustDurationDays);
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
    
    public String getDeviceFingerprint() {
        return deviceFingerprint;
    }
    
    public void setDeviceFingerprint(String deviceFingerprint) {
        this.deviceFingerprint = deviceFingerprint;
    }
    
    public String getDeviceName() {
        return deviceName;
    }
    
    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }
    
    public void setLastUsedAt(LocalDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    public String getRevokedBy() {
        return revokedBy;
    }
    
    public void setRevokedBy(String revokedBy) {
        this.revokedBy = revokedBy;
    }
    
    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }
    
    public void setRevokedAt(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }
    
    public String getRevokeReason() {
        return revokeReason;
    }
    
    public void setRevokeReason(String revokeReason) {
        this.revokeReason = revokeReason;
    }
    
    // Utility methods
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    public boolean isValid() {
        return isActive && !isExpired();
    }
    
    public void markAsUsed() {
        this.lastUsedAt = LocalDateTime.now();
    }
    
    public void revoke(String revokedBy, String reason) {
        this.isActive = false;
        this.revokedBy = revokedBy;
        this.revokedAt = LocalDateTime.now();
        this.revokeReason = reason;
    }
    
    public int getDaysUntilExpiry() {
        if (isExpired()) {
            return 0;
        }
        return (int) java.time.Duration.between(LocalDateTime.now(), expiresAt).toDays();
    }
}