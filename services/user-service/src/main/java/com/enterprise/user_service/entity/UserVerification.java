package com.enterprise.user_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * UserVerification entity for managing user verification processes
 * Handles identity verification, document verification, and KYC processes
 */
@Entity
@Table(name = "user_verifications", indexes = {
    @Index(name = "idx_verification_user_id", columnList = "user_id"),
    @Index(name = "idx_verification_type", columnList = "verification_type"),
    @Index(name = "idx_verification_status", columnList = "status"),
    @Index(name = "idx_verification_created", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class UserVerification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_type", nullable = false, length = 30)
    private VerificationType verificationType;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private VerificationStatus status = VerificationStatus.PENDING;
    
    // Document information
    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", length = 30)
    private DocumentType documentType;
    
    @Column(name = "document_number", length = 100)
    private String documentNumber; // Should be encrypted in production
    
    @Column(name = "issuing_country", length = 2)
    private String issuingCountry;
    
    @Column(name = "document_expiry_date")
    private java.time.LocalDate documentExpiryDate;
    
    // Verification data
    @Column(name = "verification_code", length = 10)
    private String verificationCode;
    
    @Column(name = "verification_token", length = 255)
    private String verificationToken;
    
    @Column(name = "verification_url", length = 500)
    private String verificationUrl;
    
    @Column(name = "external_verification_id", length = 100)
    private String externalVerificationId;
    
    // Processing information
    @Column(name = "provider", length = 50)
    private String provider; // e.g., "Jumio", "Onfido", "Manual"
    
    @Column(name = "processing_time_seconds")
    private Integer processingTimeSeconds;
    
    @Column(name = "confidence_score", precision = 5, scale = 2)
    private java.math.BigDecimal confidenceScore; // 0.00 - 100.00
    
    @Column(name = "risk_score", precision = 5, scale = 2)
    private java.math.BigDecimal riskScore; // 0.00 - 100.00
    
    // Results and feedback
    @Column(name = "verification_result", length = 1000)
    private String verificationResult; // JSON with verification details
    
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;
    
    @Column(name = "manual_review_reason", length = 500)
    private String manualReviewReason;
    
    @Column(name = "reviewer_id", length = 100)
    private String reviewerId;
    
    @Column(name = "reviewer_notes", length = 1000)
    private String reviewerNotes;
    
    // Timestamps
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
    
    // Retry and attempt tracking
    @Column(name = "attempt_number")
    @Builder.Default
    private Integer attemptNumber = 1;
    
    @Column(name = "max_attempts")
    @Builder.Default
    private Integer maxAttempts = 3;
    
    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;
    
    // Communication tracking
    @Column(name = "notification_sent", nullable = false)
    @Builder.Default
    private boolean notificationSent = false;
    
    @Column(name = "notification_sent_at")
    private LocalDateTime notificationSentAt;
    
    @Column(name = "reminder_count")
    @Builder.Default
    private Integer reminderCount = 0;
    
    @Column(name = "last_reminder_at")
    private LocalDateTime lastReminderAt;
    
    // Metadata
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(name = "device_fingerprint", length = 255)
    private String deviceFingerprint;
    
    @Column(name = "metadata", length = 2000)
    private String metadata; // JSON with additional verification metadata
    
    // Audit fields
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @CreatedBy
    @Column(name = "created_by", length = 100)
    private String createdBy;
    
    @LastModifiedBy
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
    
    @Version
    private Long version;
    
    // Enums
    public enum VerificationType {
        EMAIL,
        PHONE,
        IDENTITY,
        ADDRESS,
        BANK_ACCOUNT,
        TAX_ID,
        BUSINESS_REGISTRATION,
        FACE_MATCH,
        LIVENESS_CHECK
    }
    
    public enum VerificationStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        EXPIRED,
        CANCELLED,
        REQUIRES_MANUAL_REVIEW,
        UNDER_REVIEW,
        APPROVED,
        REJECTED
    }
    
    public enum DocumentType {
        PASSPORT,
        DRIVERS_LICENSE,
        NATIONAL_ID,
        UTILITY_BILL,
        BANK_STATEMENT,
        TAX_DOCUMENT,
        BUSINESS_LICENSE,
        EMPLOYMENT_LETTER,
        INSURANCE_DOCUMENT
    }
    
    // Helper methods
    public boolean isCompleted() {
        return status == VerificationStatus.COMPLETED || 
               status == VerificationStatus.APPROVED;
    }
    
    public boolean isFailed() {
        return status == VerificationStatus.FAILED || 
               status == VerificationStatus.REJECTED ||
               status == VerificationStatus.EXPIRED;
    }
    
    public boolean isPending() {
        return status == VerificationStatus.PENDING || 
               status == VerificationStatus.IN_PROGRESS ||
               status == VerificationStatus.UNDER_REVIEW ||
               status == VerificationStatus.REQUIRES_MANUAL_REVIEW;
    }
    
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
    
    public boolean canRetry() {
        return attemptNumber < maxAttempts && !isCompleted();
    }
    
    public void incrementAttempt() {
        this.attemptNumber++;
        if (attemptNumber >= maxAttempts) {
            this.status = VerificationStatus.FAILED;
            this.rejectionReason = "Maximum verification attempts exceeded";
        }
    }
    
    public void markAsCompleted() {
        this.status = VerificationStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.processedAt = this.processedAt == null ? LocalDateTime.now() : this.processedAt;
    }
    
    public void markAsFailed(String reason) {
        this.status = VerificationStatus.FAILED;
        this.rejectionReason = reason;
        this.processedAt = LocalDateTime.now();
    }
    
    public void markAsRejected(String reason, String reviewerId) {
        this.status = VerificationStatus.REJECTED;
        this.rejectionReason = reason;
        this.reviewerId = reviewerId;
        this.reviewedAt = LocalDateTime.now();
        this.processedAt = LocalDateTime.now();
    }
    
    public void markAsApproved(String reviewerId, String notes) {
        this.status = VerificationStatus.APPROVED;
        this.reviewerId = reviewerId;
        this.reviewerNotes = notes;
        this.reviewedAt = LocalDateTime.now();
        this.completedAt = LocalDateTime.now();
    }
    
    public void requireManualReview(String reason) {
        this.status = VerificationStatus.REQUIRES_MANUAL_REVIEW;
        this.manualReviewReason = reason;
    }
    
    public void startProcessing(String provider) {
        this.status = VerificationStatus.IN_PROGRESS;
        this.provider = provider;
        this.submittedAt = LocalDateTime.now();
    }
    
    public void sendNotification() {
        this.notificationSent = true;
        this.notificationSentAt = LocalDateTime.now();
    }
    
    public void sendReminder() {
        this.reminderCount = this.reminderCount == null ? 1 : this.reminderCount + 1;
        this.lastReminderAt = LocalDateTime.now();
    }
    
    public boolean needsReminder() {
        if (isCompleted() || isFailed()) {
            return false;
        }
        
        // Send reminder if no reminder sent in last 24 hours and verification is older than 1 hour
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        LocalDateTime oneDayAgo = LocalDateTime.now().minusHours(24);
        
        return createdAt.isBefore(oneHourAgo) && 
               (lastReminderAt == null || lastReminderAt.isBefore(oneDayAgo));
    }
    
    public long getProcessingTimeSeconds() {
        if (submittedAt != null && processedAt != null) {
            return java.time.Duration.between(submittedAt, processedAt).getSeconds();
        }
        return 0;
    }
    
    public void setExpiryTime(int hoursFromNow) {
        this.expiresAt = LocalDateTime.now().plusHours(hoursFromNow);
    }
    
    public boolean isHighRisk() {
        return riskScore != null && riskScore.compareTo(java.math.BigDecimal.valueOf(70)) >= 0;
    }
    
    public boolean isHighConfidence() {
        return confidenceScore != null && confidenceScore.compareTo(java.math.BigDecimal.valueOf(90)) >= 0;
    }
}