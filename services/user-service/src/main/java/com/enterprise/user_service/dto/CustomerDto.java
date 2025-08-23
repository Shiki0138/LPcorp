package com.enterprise.user_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Customer DTO for API responses
 * Maps to the Customer schema in the OpenAPI specification
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Customer information")
public class CustomerDto {
    
    @Schema(description = "Customer unique identifier", format = "uuid", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;
    
    @Schema(description = "Customer email address", format = "email", example = "john.doe@example.com")
    private String email;
    
    @Schema(description = "Customer first name", example = "John")
    private String firstName;
    
    @Schema(description = "Customer last name", example = "Doe")
    private String lastName;
    
    @Schema(description = "Customer phone number", example = "+1234567890")
    private String phoneNumber;
    
    @Schema(description = "Customer date of birth", format = "date", example = "1990-05-15")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;
    
    @Schema(description = "Customer account status", 
           allowableValues = {"active", "inactive", "suspended", "pending_verification"},
           example = "active")
    private String status;
    
    @Schema(description = "Customer tier level", 
           allowableValues = {"bronze", "silver", "gold", "platinum", "vip"},
           example = "gold")
    private String tier;
    
    @Schema(description = "Account creation timestamp", format = "date-time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime createdAt;
    
    @Schema(description = "Last update timestamp", format = "date-time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime updatedAt;
    
    @Schema(description = "Verification completion timestamp", format = "date-time", nullable = true)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime verifiedAt;
    
    // Additional fields for detailed response
    @Schema(description = "Email verification status")
    private Boolean emailVerified;
    
    @Schema(description = "Phone verification status")
    private Boolean phoneVerified;
    
    @Schema(description = "Identity verification status")
    private Boolean identityVerified;
    
    @Schema(description = "Profile completion score (0-100)")
    private Integer profileCompletionScore;
    
    @Schema(description = "Total number of orders")
    private Long totalOrders;
    
    @Schema(description = "Total amount spent", example = "1250.50")
    private BigDecimal totalSpent;
    
    @Schema(description = "Last order timestamp", format = "date-time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime lastOrderAt;
    
    @Schema(description = "Marketing consent status")
    private Boolean marketingConsent;
    
    @Schema(description = "Terms acceptance status")
    private Boolean termsAccepted;
    
    @Schema(description = "Terms acceptance timestamp", format = "date-time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime termsAcceptedAt;
}