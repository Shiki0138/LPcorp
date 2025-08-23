package com.enterprise.security.rbac.model;

import com.enterprise.security.rbac.model.enums.EmergencyAccessStatus;
import javax.persistence.*;
import javax.validation.constraints.*;
import java.time.Instant;

/**
 * Emergency access entity for break-glass scenarios
 */
@Entity
@Table(name = "rbac_emergency_access", indexes = {
    @Index(name = "idx_ea_user_id", columnList = "userId"),
    @Index(name = "idx_ea_status", columnList = "status"),
    @Index(name = "idx_ea_expires", columnList = "expiresAt"),
    @Index(name = "idx_ea_requested", columnList = "requestedAt")
})
public class EmergencyAccess {
    
    @Id
    private String id;
    
    @Column(nullable = false)
    @NotBlank(message = "User ID is required")
    private String userId;
    
    @Column
    private String resourceId;
    
    @Column
    private String action;
    
    @Column(length = 1000)
    @NotBlank(message = "Justification is required")
    private String justification;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmergencyAccessStatus status = EmergencyAccessStatus.PENDING;
    
    @Column(nullable = false)
    private Instant requestedAt = Instant.now();
    
    @Column
    private String requestedBy;
    
    @Column
    private Instant approvedAt;
    
    @Column
    private String approvedBy;
    
    @Column
    private Instant rejectedAt;
    
    @Column
    private String rejectedBy;
    
    @Column(length = 500)
    private String rejectionReason;
    
    @Column
    private Instant revokedAt;
    
    @Column
    private String revokedBy;
    
    @Column(length = 500)
    private String revocationReason;
    
    @Column
    private Instant expiresAt;
    
    @Column(length = 1000)
    private String conditions; // Special conditions or restrictions
    
    // Constructors
    public EmergencyAccess() {}
    
    public EmergencyAccess(String userId, String resourceId, String action, String justification, String requestedBy) {
        this.userId = userId;
        this.resourceId = resourceId;
        this.action = action;
        this.justification = justification;
        this.requestedBy = requestedBy;
    }
    
    // Business methods
    public boolean isActive() {
        return status == EmergencyAccessStatus.ACTIVE && !isExpired();
    }
    
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }
    
    public boolean isPending() {
        return status == EmergencyAccessStatus.PENDING;
    }
    
    public boolean isApproved() {
        return status == EmergencyAccessStatus.ACTIVE;
    }
    
    public boolean isRejected() {
        return status == EmergencyAccessStatus.REJECTED;
    }
    
    public boolean isRevoked() {
        return status == EmergencyAccessStatus.REVOKED;
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }
    
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    
    public String getJustification() { return justification; }
    public void setJustification(String justification) { this.justification = justification; }
    
    public EmergencyAccessStatus getStatus() { return status; }
    public void setStatus(EmergencyAccessStatus status) { this.status = status; }
    
    public Instant getRequestedAt() { return requestedAt; }
    public void setRequestedAt(Instant requestedAt) { this.requestedAt = requestedAt; }
    
    public String getRequestedBy() { return requestedBy; }
    public void setRequestedBy(String requestedBy) { this.requestedBy = requestedBy; }
    
    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }
    
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    
    public Instant getRejectedAt() { return rejectedAt; }
    public void setRejectedAt(Instant rejectedAt) { this.rejectedAt = rejectedAt; }
    
    public String getRejectedBy() { return rejectedBy; }
    public void setRejectedBy(String rejectedBy) { this.rejectedBy = rejectedBy; }
    
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    
    public Instant getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }
    
    public String getRevokedBy() { return revokedBy; }
    public void setRevokedBy(String revokedBy) { this.revokedBy = revokedBy; }
    
    public String getRevocationReason() { return revocationReason; }
    public void setRevocationReason(String revocationReason) { this.revocationReason = revocationReason; }
    
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    
    public String getConditions() { return conditions; }
    public void setConditions(String conditions) { this.conditions = conditions; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmergencyAccess)) return false;
        EmergencyAccess that = (EmergencyAccess) o;
        return id != null && id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
    
    @Override
    public String toString() {
        return "EmergencyAccess{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", status=" + status +
                ", requestedAt=" + requestedAt +
                '}';
    }
}