package com.enterprise.user_service.entity;

import com.enterprise.user_service.util.AttributeEncryptor;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
 * Address entity for user address management
 * Supports multiple addresses per user with different types
 */
@Entity
@Table(name = "addresses", indexes = {
    @Index(name = "idx_address_user_id", columnList = "user_id"),
    @Index(name = "idx_address_type", columnList = "type"),
    @Index(name = "idx_address_default", columnList = "is_default"),
    @Index(name = "idx_address_country", columnList = "country")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Address {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private AddressType type;
    
    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private boolean isDefault = false;
    
    // Address components (PII - Encrypted)
    @NotBlank
    @Size(max = 100)
    @Column(name = "line1", nullable = false, length = 100)
    @Convert(converter = AttributeEncryptor.class)
    private String line1;
    
    @Size(max = 100)
    @Column(name = "line2", length = 100)
    @Convert(converter = AttributeEncryptor.class)
    private String line2;
    
    @NotBlank
    @Size(max = 50)
    @Column(name = "city", nullable = false, length = 50)
    @Convert(converter = AttributeEncryptor.class)
    private String city;
    
    @NotBlank
    @Size(max = 50)
    @Column(name = "state", nullable = false, length = 50)
    private String state;
    
    @NotBlank
    @Size(max = 20)
    @Column(name = "postal_code", nullable = false, length = 20)
    private String postalCode;
    
    @NotBlank
    @Pattern(regexp = "^[A-Z]{2}$", message = "Country must be a valid ISO 3166-1 alpha-2 code")
    @Column(name = "country", nullable = false, length = 2)
    private String country;
    
    // Additional address information
    @Column(name = "label", length = 50)
    private String label; // e.g., "Home", "Work", "Parents", etc.
    
    @Column(name = "company_name", length = 100)
    private String companyName;
    
    @Column(name = "attention_to", length = 100)
    private String attentionTo; // "c/o" or "Attn:"
    
    // Geographic coordinates (for delivery optimization)
    @Column(name = "latitude", precision = 10, scale = 8)
    private java.math.BigDecimal latitude;
    
    @Column(name = "longitude", precision = 11, scale = 8)
    private java.math.BigDecimal longitude;
    
    // Validation status
    @Column(name = "is_validated", nullable = false)
    @Builder.Default
    private boolean isValidated = false;
    
    @Column(name = "validated_at")
    private LocalDateTime validatedAt;
    
    @Column(name = "validation_service", length = 50)
    private String validationService; // e.g., "Google", "USPS", etc.
    
    // Usage statistics
    @Column(name = "usage_count")
    @Builder.Default
    private Long usageCount = 0L;
    
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;
    
    // Delivery preferences
    @Column(name = "delivery_instructions", length = 500)
    private String deliveryInstructions;
    
    @Column(name = "access_code", length = 20)
    @Convert(converter = AttributeEncryptor.class)
    private String accessCode;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_preference", length = 30)
    @Builder.Default
    private DeliveryPreference deliveryPreference = DeliveryPreference.FRONT_DOOR;
    
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
    public enum AddressType {
        BILLING,
        SHIPPING,
        BOTH
    }
    
    public enum DeliveryPreference {
        FRONT_DOOR,
        BACK_DOOR,
        SIDE_ENTRANCE,
        LEAVE_WITH_CONCIERGE,
        LEAVE_WITH_NEIGHBOR,
        SECURE_LOCATION,
        SIGNATURE_REQUIRED,
        NO_SAFE_DROP
    }
    
    // Helper methods
    public String getFormattedAddress() {
        StringBuilder formatted = new StringBuilder();
        
        if (companyName != null && !companyName.trim().isEmpty()) {
            formatted.append(companyName).append("\n");
        }
        
        if (attentionTo != null && !attentionTo.trim().isEmpty()) {
            formatted.append("c/o ").append(attentionTo).append("\n");
        }
        
        formatted.append(line1);
        
        if (line2 != null && !line2.trim().isEmpty()) {
            formatted.append("\n").append(line2);
        }
        
        formatted.append("\n")
                .append(city).append(", ")
                .append(state).append(" ")
                .append(postalCode).append("\n")
                .append(country);
        
        return formatted.toString();
    }
    
    public String getShortAddress() {
        StringBuilder short_addr = new StringBuilder(line1);
        if (line2 != null && !line2.trim().isEmpty()) {
            short_addr.append(", ").append(line2);
        }
        short_addr.append(", ").append(city)
                .append(", ").append(state)
                .append(" ").append(postalCode);
        return short_addr.toString();
    }
    
    public boolean isSameAddress(Address other) {
        if (other == null) return false;
        
        return this.line1.equals(other.line1) &&
               Objects.equals(this.line2, other.line2) &&
               this.city.equals(other.city) &&
               this.state.equals(other.state) &&
               this.postalCode.equals(other.postalCode) &&
               this.country.equals(other.country);
    }
    
    public void incrementUsage() {
        this.usageCount = this.usageCount == null ? 1 : this.usageCount + 1;
        this.lastUsedAt = LocalDateTime.now();
    }
    
    public void markAsValidated(String service) {
        this.isValidated = true;
        this.validatedAt = LocalDateTime.now();
        this.validationService = service;
    }
    
    public boolean canBeDefault() {
        return type == AddressType.BOTH || type == AddressType.BILLING;
    }
    
    public boolean isInternational() {
        return !"US".equals(country);
    }
    
    private static class Objects {
        public static boolean equals(Object a, Object b) {
            return (a == b) || (a != null && a.equals(b));
        }
    }
}