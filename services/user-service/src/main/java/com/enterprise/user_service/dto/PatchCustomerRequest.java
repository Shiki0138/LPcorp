package com.enterprise.user_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO for partially updating customer information
 * Maps to the PatchCustomerRequest schema in the OpenAPI specification
 * At least one field must be provided for a valid patch request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to partially update customer information (at least one field required)")
public class PatchCustomerRequest {
    
    @Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
    @Schema(description = "Customer first name", minLength = 1, maxLength = 50, example = "John")
    private String firstName;
    
    @Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
    @Schema(description = "Customer last name", minLength = 1, maxLength = 50, example = "Doe")
    private String lastName;
    
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    @Schema(description = "Customer phone number in international format", 
           pattern = "^\\+?[1-9]\\d{1,14}$", example = "+1234567890")
    private String phoneNumber;
    
    @Past(message = "Date of birth must be in the past")
    @Schema(description = "Customer date of birth", format = "date", example = "1990-05-15")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;
    
    @Schema(description = "Customer account status", 
           allowableValues = {"active", "inactive", "suspended", "pending_verification"},
           example = "active")
    private String status;
    
    // Additional commonly updated fields
    @Schema(description = "Customer middle name", maxLength = 50, example = "Michael")
    @Size(max = 50, message = "Middle name cannot exceed 50 characters")
    private String middleName;
    
    @Schema(description = "Customer nickname", maxLength = 50, example = "Johnny")
    @Size(max = 50, message = "Nickname cannot exceed 50 characters")
    private String nickname;
    
    @Schema(description = "Marketing consent flag")
    private Boolean marketingConsent;
    
    @Schema(description = "Profile visibility setting", allowableValues = {"PUBLIC", "PRIVATE", "FRIENDS_ONLY"})
    private String profileVisibility;
    
    @Schema(description = "Preferred language", pattern = "^[a-z]{2}-[A-Z]{2}$", example = "en-US")
    @Pattern(regexp = "^[a-z]{2}-[A-Z]{2}$", message = "Language must be in format 'en-US'")
    private String preferredLanguage;
    
    @Schema(description = "Preferred currency", pattern = "^[A-Z]{3}$", example = "USD")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid ISO 4217 code")
    private String preferredCurrency;
    
    @Schema(description = "Timezone", maxLength = 50, example = "America/New_York")
    @Size(max = 50, message = "Timezone cannot exceed 50 characters")
    private String timezone;
    
    /**
     * Validates that at least one field is provided for patch request
     */
    public boolean hasAtLeastOneField() {
        return firstName != null || lastName != null || phoneNumber != null || 
               dateOfBirth != null || status != null || middleName != null || 
               nickname != null || marketingConsent != null || profileVisibility != null ||
               preferredLanguage != null || preferredCurrency != null || timezone != null;
    }
    
    /**
     * Counts the number of non-null fields
     */
    public int getFieldCount() {
        int count = 0;
        if (firstName != null) count++;
        if (lastName != null) count++;
        if (phoneNumber != null) count++;
        if (dateOfBirth != null) count++;
        if (status != null) count++;
        if (middleName != null) count++;
        if (nickname != null) count++;
        if (marketingConsent != null) count++;
        if (profileVisibility != null) count++;
        if (preferredLanguage != null) count++;
        if (preferredCurrency != null) count++;
        if (timezone != null) count++;
        return count;
    }
}