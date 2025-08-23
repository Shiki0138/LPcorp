package com.enterprise.user_service.entity;

import com.enterprise.user_service.util.AttributeEncryptor;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * UserProfile entity for extended user information
 * Contains additional profile data, demographics, and personal information
 */
@Entity
@Table(name = "user_profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class UserProfile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
    
    // Personal Information (PII - Encrypted)
    @Column(name = "middle_name", length = 50)
    @Convert(converter = AttributeEncryptor.class)
    private String middleName;
    
    @Column(name = "nickname", length = 50)
    private String nickname;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 20)
    private Gender gender;
    
    @Column(name = "nationality", length = 2)
    private String nationality; // ISO 3166-1 alpha-2
    
    @Column(name = "place_of_birth", length = 100)
    @Convert(converter = AttributeEncryptor.class)
    private String placeOfBirth;
    
    // Contact preferences
    @Column(name = "preferred_contact_method", length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ContactMethod preferredContactMethod = ContactMethod.EMAIL;
    
    @Column(name = "emergency_contact_name", length = 100)
    @Convert(converter = AttributeEncryptor.class)
    private String emergencyContactName;
    
    @Column(name = "emergency_contact_phone", length = 20)
    @Convert(converter = AttributeEncryptor.class)
    private String emergencyContactPhone;
    
    // Professional information
    @Column(name = "occupation", length = 100)
    private String occupation;
    
    @Column(name = "company", length = 100)
    private String company;
    
    @Column(name = "annual_income", precision = 19, scale = 2)
    private java.math.BigDecimal annualIncome;
    
    // Demographics
    @Enumerated(EnumType.STRING)
    @Column(name = "marital_status", length = 20)
    private MaritalStatus maritalStatus;
    
    @Column(name = "number_of_children")
    @Min(0)
    @Max(20)
    private Integer numberOfChildren;
    
    @Column(name = "education_level", length = 50)
    @Enumerated(EnumType.STRING)
    private EducationLevel educationLevel;
    
    // Interests and preferences
    @Column(name = "interests", length = 1000)
    private String interests; // JSON array of interests
    
    @Column(name = "bio", length = 1000)
    private String bio;
    
    @Column(name = "website_url", length = 255)
    private String websiteUrl;
    
    @Column(name = "linkedin_url", length = 255)
    private String linkedinUrl;
    
    @Column(name = "twitter_handle", length = 50)
    private String twitterHandle;
    
    // Avatar and media
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;
    
    @Column(name = "avatar_updated_at")
    private LocalDateTime avatarUpdatedAt;
    
    // Privacy settings
    @Column(name = "profile_visibility", length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ProfileVisibility profileVisibility = ProfileVisibility.PRIVATE;
    
    @Column(name = "allow_marketing_emails", nullable = false)
    @Builder.Default
    private boolean allowMarketingEmails = false;
    
    @Column(name = "allow_third_party_sharing", nullable = false)
    @Builder.Default
    private boolean allowThirdPartySharing = false;
    
    // Verification status
    @Column(name = "identity_document_type", length = 50)
    private String identityDocumentType;
    
    @Column(name = "identity_document_number", length = 100)
    @Convert(converter = AttributeEncryptor.class)
    private String identityDocumentNumber;
    
    @Column(name = "identity_document_expiry")
    private java.time.LocalDate identityDocumentExpiry;
    
    @Column(name = "identity_document_country", length = 2)
    private String identityDocumentCountry;
    
    // Behavioral data
    @Column(name = "last_profile_update")
    private LocalDateTime lastProfileUpdate;
    
    @Column(name = "login_count")
    @Builder.Default
    private Long loginCount = 0L;
    
    @Column(name = "profile_views")
    @Builder.Default
    private Long profileViews = 0L;
    
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
    public enum Gender {
        MALE, FEMALE, OTHER, PREFER_NOT_TO_SAY
    }
    
    public enum ContactMethod {
        EMAIL, PHONE, SMS, MAIL
    }
    
    public enum MaritalStatus {
        SINGLE, MARRIED, DIVORCED, WIDOWED, SEPARATED, DOMESTIC_PARTNERSHIP
    }
    
    public enum EducationLevel {
        HIGH_SCHOOL, ASSOCIATE_DEGREE, BACHELOR_DEGREE, MASTER_DEGREE, 
        DOCTORATE, PROFESSIONAL_DEGREE, OTHER
    }
    
    public enum ProfileVisibility {
        PUBLIC, PRIVATE, FRIENDS_ONLY
    }
    
    // Helper methods
    public String getFullName() {
        StringBuilder fullName = new StringBuilder(user.getFirstName());
        if (middleName != null && !middleName.trim().isEmpty()) {
            fullName.append(" ").append(middleName);
        }
        fullName.append(" ").append(user.getLastName());
        return fullName.toString();
    }
    
    public boolean isProfileComplete() {
        int completedFields = 0;
        int totalFields = 15; // Key fields for profile completion
        
        if (user.getFirstName() != null) completedFields++;
        if (user.getLastName() != null) completedFields++;
        if (user.getEmail() != null) completedFields++;
        if (user.getPhoneNumber() != null) completedFields++;
        if (user.getDateOfBirth() != null) completedFields++;
        if (gender != null) completedFields++;
        if (nationality != null) completedFields++;
        if (occupation != null) completedFields++;
        if (maritalStatus != null) completedFields++;
        if (educationLevel != null) completedFields++;
        if (bio != null && !bio.trim().isEmpty()) completedFields++;
        if (avatarUrl != null) completedFields++;
        if (!user.getAddresses().isEmpty()) completedFields++;
        if (user.isEmailVerified()) completedFields++;
        if (user.isPhoneVerified()) completedFields++;
        
        return (completedFields * 100 / totalFields) >= 80; // 80% completion threshold
    }
    
    public int calculateCompletionScore() {
        int completedFields = 0;
        int totalFields = 15;
        
        if (user.getFirstName() != null) completedFields++;
        if (user.getLastName() != null) completedFields++;
        if (user.getEmail() != null) completedFields++;
        if (user.getPhoneNumber() != null) completedFields++;
        if (user.getDateOfBirth() != null) completedFields++;
        if (gender != null) completedFields++;
        if (nationality != null) completedFields++;
        if (occupation != null) completedFields++;
        if (maritalStatus != null) completedFields++;
        if (educationLevel != null) completedFields++;
        if (bio != null && !bio.trim().isEmpty()) completedFields++;
        if (avatarUrl != null) completedFields++;
        if (!user.getAddresses().isEmpty()) completedFields++;
        if (user.isEmailVerified()) completedFields++;
        if (user.isPhoneVerified()) completedFields++;
        
        return (completedFields * 100) / totalFields;
    }
    
    public void incrementLoginCount() {
        this.loginCount = this.loginCount == null ? 1 : this.loginCount + 1;
    }
    
    public void incrementProfileViews() {
        this.profileViews = this.profileViews == null ? 1 : this.profileViews + 1;
    }
}