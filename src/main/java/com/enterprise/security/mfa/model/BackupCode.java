package com.enterprise.security.mfa.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "backup_codes")
public class BackupCode {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(nullable = false)
    private String userId;
    
    @Column(nullable = false, unique = true)
    private String hashedCode;
    
    @Column(nullable = false)
    private boolean isUsed = false;
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column
    private LocalDateTime usedAt;
    
    @Column
    private String usedFromIp;
    
    @Column
    private String usedFromUserAgent;
    
    // Constructors
    public BackupCode() {}
    
    public BackupCode(String userId, String hashedCode) {
        this.userId = userId;
        this.hashedCode = hashedCode;
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
    
    public String getHashedCode() {
        return hashedCode;
    }
    
    public void setHashedCode(String hashedCode) {
        this.hashedCode = hashedCode;
    }
    
    public boolean isUsed() {
        return isUsed;
    }
    
    public void setUsed(boolean used) {
        isUsed = used;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUsedAt() {
        return usedAt;
    }
    
    public void setUsedAt(LocalDateTime usedAt) {
        this.usedAt = usedAt;
    }
    
    public String getUsedFromIp() {
        return usedFromIp;
    }
    
    public void setUsedFromIp(String usedFromIp) {
        this.usedFromIp = usedFromIp;
    }
    
    public String getUsedFromUserAgent() {
        return usedFromUserAgent;
    }
    
    public void setUsedFromUserAgent(String usedFromUserAgent) {
        this.usedFromUserAgent = usedFromUserAgent;
    }
    
    // Utility methods
    public void markAsUsed(String ipAddress, String userAgent) {
        this.isUsed = true;
        this.usedAt = LocalDateTime.now();
        this.usedFromIp = ipAddress;
        this.usedFromUserAgent = userAgent;
    }
    
    public boolean isValid() {
        return !isUsed;
    }
}