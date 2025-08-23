package com.enterprise.security.mfa.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "mfa_challenges")
public class MfaChallenge {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(nullable = false)
    private String userId;
    
    @Column(nullable = false)
    private UUID deviceId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MfaType type;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String challenge;
    
    @Column
    private String expectedResponse;
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(nullable = false)
    private LocalDateTime expiresAt;
    
    @Column
    private LocalDateTime usedAt;
    
    @Column(nullable = false)
    private boolean isUsed = false;
    
    @Column
    private int attemptCount = 0;
    
    @Column
    private String sessionId;
    
    @Column
    private String ipAddress;
    
    @Column
    private String userAgent;
    
    // Constructors
    public MfaChallenge() {}
    
    public MfaChallenge(String userId, UUID deviceId, MfaType type, String challenge, int validityMinutes) {
        this.userId = userId;
        this.deviceId = deviceId;
        this.type = type;
        this.challenge = challenge;
        this.expiresAt = LocalDateTime.now().plusMinutes(validityMinutes);
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
    
    public UUID getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(UUID deviceId) {
        this.deviceId = deviceId;
    }
    
    public MfaType getType() {
        return type;
    }
    
    public void setType(MfaType type) {
        this.type = type;
    }
    
    public String getChallenge() {
        return challenge;
    }
    
    public void setChallenge(String challenge) {
        this.challenge = challenge;
    }
    
    public String getExpectedResponse() {
        return expectedResponse;
    }
    
    public void setExpectedResponse(String expectedResponse) {
        this.expectedResponse = expectedResponse;
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
    
    public LocalDateTime getUsedAt() {
        return usedAt;
    }
    
    public void setUsedAt(LocalDateTime usedAt) {
        this.usedAt = usedAt;
    }
    
    public boolean isUsed() {
        return isUsed;
    }
    
    public void setUsed(boolean used) {
        isUsed = used;
    }
    
    public int getAttemptCount() {
        return attemptCount;
    }
    
    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    // Utility methods
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    public boolean isValid() {
        return !isUsed && !isExpired();
    }
    
    public void incrementAttemptCount() {
        this.attemptCount++;
    }
    
    public void markAsUsed() {
        this.isUsed = true;
        this.usedAt = LocalDateTime.now();
    }
    
    public int getRemainingMinutes() {
        if (isExpired()) {
            return 0;
        }
        return (int) java.time.Duration.between(LocalDateTime.now(), expiresAt).toMinutes();
    }
}