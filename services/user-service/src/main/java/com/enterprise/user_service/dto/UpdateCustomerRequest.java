package com.enterprise.user_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO for updating customer information (full update)
 * Maps to the UpdateCustomerRequest schema in the OpenAPI specification
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update customer information")
public class UpdateCustomerRequest {
    
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
    
    // Extended profile fields
    @Schema(description = "Customer middle name", maxLength = 50, example = "Michael")
    @Size(max = 50, message = "Middle name cannot exceed 50 characters")
    private String middleName;
    
    @Schema(description = "Customer nickname", maxLength = 50, example = "Johnny")
    @Size(max = 50, message = "Nickname cannot exceed 50 characters")
    private String nickname;
    
    @Schema(description = "Customer gender", allowableValues = {"MALE", "FEMALE", "OTHER", "PREFER_NOT_TO_SAY"})
    private String gender;
    
    @Schema(description = "Customer nationality (ISO 3166-1 alpha-2)", pattern = "^[A-Z]{2}$", example = "US")
    @Pattern(regexp = "^[A-Z]{2}$", message = "Nationality must be a valid ISO 3166-1 alpha-2 code")
    private String nationality;
    
    @Schema(description = "Customer occupation", maxLength = 100, example = "Software Engineer")
    @Size(max = 100, message = "Occupation cannot exceed 100 characters")
    private String occupation;
    
    @Schema(description = "Customer company", maxLength = 100, example = "Tech Corp Inc.")
    @Size(max = 100, message = "Company cannot exceed 100 characters")
    private String company;
    
    @Schema(description = "Customer marital status", allowableValues = {"SINGLE", "MARRIED", "DIVORCED", "WIDOWED", "SEPARATED", "DOMESTIC_PARTNERSHIP"})
    private String maritalStatus;
    
    @Schema(description = "Customer education level", allowableValues = {"HIGH_SCHOOL", "ASSOCIATE_DEGREE", "BACHELOR_DEGREE", "MASTER_DEGREE", "DOCTORATE", "PROFESSIONAL_DEGREE", "OTHER"})
    private String educationLevel;
    
    @Schema(description = "Customer bio", maxLength = 1000, example = "Passionate about technology and innovation.")
    @Size(max = 1000, message = "Bio cannot exceed 1000 characters")
    private String bio;
    
    @Schema(description = "Customer website URL", maxLength = 255, example = "https://johndoe.com")
    @Size(max = 255, message = "Website URL cannot exceed 255 characters")
    private String websiteUrl;
    
    @Schema(description = "LinkedIn profile URL", maxLength = 255, example = "https://linkedin.com/in/johndoe")
    @Size(max = 255, message = "LinkedIn URL cannot exceed 255 characters")
    private String linkedinUrl;
    
    @Schema(description = "Twitter handle", maxLength = 50, example = "@johndoe")
    @Size(max = 50, message = "Twitter handle cannot exceed 50 characters")
    private String twitterHandle;
    
    // Preferences
    @Schema(description = "Marketing consent flag")
    private Boolean marketingConsent;
    
    @Schema(description = "Third party data sharing consent")
    private Boolean thirdPartyDataSharing;
    
    @Schema(description = "Profile visibility setting", allowableValues = {"PUBLIC", "PRIVATE", "FRIENDS_ONLY"})
    private String profileVisibility;
    
    @Schema(description = "Preferred contact method", allowableValues = {"EMAIL", "PHONE", "SMS", "MAIL"})
    private String preferredContactMethod;
    
    // Localization preferences
    @Schema(description = "Preferred language", pattern = "^[a-z]{2}-[A-Z]{2}$", example = "en-US")
    @Pattern(regexp = "^[a-z]{2}-[A-Z]{2}$", message = "Language must be in format 'en-US'")
    private String preferredLanguage;
    
    @Schema(description = "Preferred currency", pattern = "^[A-Z]{3}$", example = "USD")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid ISO 4217 code")
    private String preferredCurrency;
    
    @Schema(description = "Timezone", maxLength = 50, example = "America/New_York")
    @Size(max = 50, message = "Timezone cannot exceed 50 characters")
    private String timezone;
    
    @Schema(description = "Date format preference", allowableValues = {"MM_DD_YYYY", "DD_MM_YYYY", "YYYY_MM_DD"})
    private String dateFormat;
    
    // Emergency contact
    @Schema(description = "Emergency contact name", maxLength = 100, example = "Jane Doe")
    @Size(max = 100, message = "Emergency contact name cannot exceed 100 characters")
    private String emergencyContactName;
    
    @Schema(description = "Emergency contact phone", maxLength = 20, example = "+1234567890")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid emergency contact phone format")
    private String emergencyContactPhone;
}