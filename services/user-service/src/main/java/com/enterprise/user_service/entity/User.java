package com.enterprise.user_service.entity;

import com.enterprise.user_service.util.AttributeEncryptor;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Where;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Enhanced User entity for customer management
 * Includes profile management, verification, tier system, and GDPR compliance
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_email", columnList = "email"),
    @Index(name = "idx_user_status", columnList = "status"),
    @Index(name = "idx_user_tier", columnList = "tier"),
    @Index(name = "idx_user_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"password"})
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "UPDATE users SET deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Where(clause = "deleted = false")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;
    
    @Email
    @NotBlank
    @Column(name = "email", unique = true, nullable = false, length = 255)
    @Convert(converter = AttributeEncryptor.class)
    private String email;
    
    @NotBlank
    @Column(name = "first_name", nullable = false, length = 50)
    @Convert(converter = AttributeEncryptor.class)
    private String firstName;
    
    @NotBlank
    @Column(name = "last_name", nullable = false, length = 50)
    @Convert(converter = AttributeEncryptor.class)
    private String lastName;
    
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    @Column(name = "phone_number", length = 20)
    @Convert(converter = AttributeEncryptor.class)
    private String phoneNumber;
    
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private CustomerStatus status = CustomerStatus.PENDING_VERIFICATION;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "tier", nullable = false, length = 20)
    @Builder.Default
    private CustomerTier tier = CustomerTier.BRONZE;
    
    // Verification fields
    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private boolean emailVerified = false;
    
    @Column(name = "phone_verified", nullable = false)
    @Builder.Default
    private boolean phoneVerified = false;
    
    @Column(name = "identity_verified", nullable = false)
    @Builder.Default
    private boolean identityVerified = false;
    
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
    
    // Profile completion and statistics
    @Column(name = "profile_completion_score")
    @Builder.Default
    private Integer profileCompletionScore = 0;
    
    @Column(name = "total_orders")
    @Builder.Default
    private Long totalOrders = 0L;
    
    @Column(name = "total_spent", precision = 19, scale = 2)
    @Builder.Default
    private java.math.BigDecimal totalSpent = java.math.BigDecimal.ZERO;
    
    @Column(name = "last_order_at")
    private LocalDateTime lastOrderAt;
    
    // Marketing and terms consent
    @Column(name = "marketing_consent", nullable = false)
    @Builder.Default
    private boolean marketingConsent = false;
    
    @Column(name = "terms_accepted", nullable = false)
    @Builder.Default
    private boolean termsAccepted = false;
    
    @Column(name = "terms_accepted_at")
    private LocalDateTime termsAcceptedAt;
    
    // GDPR and privacy
    @Column(name = "gdpr_consent", nullable = false)
    @Builder.Default
    private boolean gdprConsent = false;
    
    @Column(name = "gdpr_consent_at")
    private LocalDateTime gdprConsentAt;
    
    @Column(name = "data_retention_days")
    @Builder.Default
    private Integer dataRetentionDays = 2555; // 7 years default
    
    // Relationships
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private UserProfile profile;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Address> addresses = new ArrayList<>();
    
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private UserPreferences preferences;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<UserVerification> verifications = new ArrayList<>();
    
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
    
    // Soft delete
    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    @Column(name = "deleted_by", length = 100)
    private String deletedBy;
    
    @Version
    private Long version;
    
    // Business methods
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    public boolean isActive() {
        return status == CustomerStatus.ACTIVE;
    }
    
    public boolean isFullyVerified() {
        return emailVerified && phoneVerified && identityVerified;
    }
    
    public void markAsDeleted(String deletedBy) {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedBy;
    }
    
    public void updateTotalSpent(java.math.BigDecimal amount) {
        this.totalSpent = this.totalSpent.add(amount);
        this.totalOrders = this.totalOrders + 1;
        this.lastOrderAt = LocalDateTime.now();
        calculateTier();
    }
    
    public void calculateTier() {
        if (totalSpent.compareTo(java.math.BigDecimal.valueOf(100000)) >= 0) {
            this.tier = CustomerTier.VIP;
        } else if (totalSpent.compareTo(java.math.BigDecimal.valueOf(50000)) >= 0) {
            this.tier = CustomerTier.PLATINUM;
        } else if (totalSpent.compareTo(java.math.BigDecimal.valueOf(10000)) >= 0) {
            this.tier = CustomerTier.GOLD;
        } else if (totalSpent.compareTo(java.math.BigDecimal.valueOf(1000)) >= 0) {
            this.tier = CustomerTier.SILVER;
        } else {
            this.tier = CustomerTier.BRONZE;
        }
    }
    
    public enum CustomerStatus {
        ACTIVE,
        INACTIVE,
        SUSPENDED,
        PENDING_VERIFICATION
    }
    
    public enum CustomerTier {
        BRONZE,
        SILVER,
        GOLD,
        PLATINUM,
        VIP
    }
}