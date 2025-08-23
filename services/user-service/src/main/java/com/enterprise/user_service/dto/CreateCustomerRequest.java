package com.enterprise.user_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO for creating a new customer
 * Maps to the CreateCustomerRequest schema in the OpenAPI specification
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a new customer")
public class CreateCustomerRequest {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Schema(description = "Customer email address", format = "email", example = "john.doe@example.com", required = true)
    private String email;
    
    @NotBlank(message = "First name is required")
    @Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
    @Schema(description = "Customer first name", minLength = 1, maxLength = 50, example = "John", required = true)
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
    @Schema(description = "Customer last name", minLength = 1, maxLength = 50, example = "Doe", required = true)
    private String lastName;
    
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    @Schema(description = "Customer phone number in international format", 
           pattern = "^\\+?[1-9]\\d{1,14}$", example = "+1234567890")
    private String phoneNumber;
    
    @Past(message = "Date of birth must be in the past")
    @Schema(description = "Customer date of birth", format = "date", example = "1990-05-15")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$", 
             message = "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character")
    @Schema(description = "Customer password (must meet complexity requirements)", 
           minLength = 8, 
           pattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
           example = "SecureP@ssw0rd!")
    private String password;
    
    @Builder.Default
    @Schema(description = "Marketing consent flag", example = "false", defaultValue = "false")
    private boolean marketingConsent = false;
    
    @Builder.Default
    @Schema(description = "Terms and conditions acceptance flag", example = "true", defaultValue = "false")
    private boolean termsAccepted = false;
    
    // Additional fields for enhanced registration
    @Schema(description = "Customer's preferred language", pattern = "^[a-z]{2}-[A-Z]{2}$", example = "en-US")
    @Pattern(regexp = "^[a-z]{2}-[A-Z]{2}$", message = "Language must be in format 'en-US'")
    private String preferredLanguage;
    
    @Schema(description = "Customer's preferred currency", pattern = "^[A-Z]{3}$", example = "USD")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid ISO 4217 code")
    private String preferredCurrency;
    
    @Schema(description = "Customer's timezone", example = "America/New_York")
    private String timezone;
    
    @Schema(description = "Referral code used during registration", example = "REF123456")
    private String referralCode;
    
    @Schema(description = "Source of registration (web, mobile, api)", example = "web")
    private String registrationSource;
    
    @Schema(description = "UTM campaign parameter for tracking", example = "summer_campaign_2024")
    private String utmCampaign;
    
    @Schema(description = "UTM source parameter for tracking", example = "google")
    private String utmSource;
    
    @Schema(description = "UTM medium parameter for tracking", example = "cpc")
    private String utmMedium;
    
    // GDPR and privacy fields
    @Builder.Default
    @Schema(description = "GDPR consent flag", example = "true", defaultValue = "false")
    private boolean gdprConsent = false;
    
    @Builder.Default
    @Schema(description = "Data processing consent flag", example = "true", defaultValue = "false")
    private boolean dataProcessingConsent = false;
    
    @Builder.Default
    @Schema(description = "Third party data sharing consent flag", example = "false", defaultValue = "false")
    private boolean thirdPartyDataSharing = false;
    
    // Validation groups for different registration flows
    public interface BasicRegistration {}
    public interface FullRegistration {}
    public interface AdminRegistration {}
}