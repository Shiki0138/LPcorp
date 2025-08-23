package com.enterprise.user_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating an existing address
 * Maps to the UpdateAddressRequest schema in the OpenAPI specification
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update an existing address")
public class UpdateAddressRequest {
    
    @NotBlank(message = "Address type is required")
    @Schema(description = "Address type", allowableValues = {"billing", "shipping", "both"}, example = "billing", required = true)
    private String type;
    
    @Builder.Default
    @Schema(description = "Default address flag", example = "false", defaultValue = "false")
    private boolean isDefault = false;
    
    @NotBlank(message = "Address line 1 is required")
    @Size(max = 100, message = "Address line 1 cannot exceed 100 characters")
    @Schema(description = "Address line 1", maxLength = 100, example = "123 Main Street", required = true)
    private String line1;
    
    @Size(max = 100, message = "Address line 2 cannot exceed 100 characters")
    @Schema(description = "Address line 2 (optional)", maxLength = 100, example = "Apt 4B")
    private String line2;
    
    @NotBlank(message = "City is required")
    @Size(max = 50, message = "City cannot exceed 50 characters")
    @Schema(description = "City name", maxLength = 50, example = "New York", required = true)
    private String city;
    
    @NotBlank(message = "State is required")
    @Size(max = 50, message = "State cannot exceed 50 characters")
    @Schema(description = "State or province", maxLength = 50, example = "NY", required = true)
    private String state;
    
    @NotBlank(message = "Postal code is required")
    @Size(max = 20, message = "Postal code cannot exceed 20 characters")
    @Schema(description = "Postal or ZIP code", maxLength = 20, example = "10001", required = true)
    private String postalCode;
    
    @NotBlank(message = "Country is required")
    @Pattern(regexp = "^[A-Z]{2}$", message = "Country must be a valid ISO 3166-1 alpha-2 code")
    @Schema(description = "Country code (ISO 3166-1 alpha-2)", pattern = "^[A-Z]{2}$", example = "US", required = true)
    private String country;
    
    // Optional extended fields
    @Size(max = 50, message = "Label cannot exceed 50 characters")
    @Schema(description = "Address label", maxLength = 50, example = "Home")
    private String label;
    
    @Size(max = 100, message = "Company name cannot exceed 100 characters")
    @Schema(description = "Company name", maxLength = 100, example = "Acme Corp")
    private String companyName;
    
    @Size(max = 100, message = "Attention to cannot exceed 100 characters")
    @Schema(description = "Attention to", maxLength = 100, example = "John Doe")
    private String attentionTo;
    
    @Size(max = 500, message = "Delivery instructions cannot exceed 500 characters")
    @Schema(description = "Delivery instructions", maxLength = 500, example = "Leave at front door")
    private String deliveryInstructions;
    
    @Size(max = 20, message = "Access code cannot exceed 20 characters")
    @Schema(description = "Building or gate access code", maxLength = 20, example = "1234")
    private String accessCode;
    
    @Builder.Default
    @Schema(description = "Delivery preference", allowableValues = {
        "FRONT_DOOR", "BACK_DOOR", "SIDE_ENTRANCE", "LEAVE_WITH_CONCIERGE", 
        "LEAVE_WITH_NEIGHBOR", "SECURE_LOCATION", "SIGNATURE_REQUIRED", "NO_SAFE_DROP"
    }, example = "FRONT_DOOR", defaultValue = "FRONT_DOOR")
    private String deliveryPreference = "FRONT_DOOR";
    
    // Validation methods
    public boolean isValidAddressType() {
        return type != null && 
               (type.equals("billing") || type.equals("shipping") || type.equals("both"));
    }
    
    public boolean isValidDeliveryPreference() {
        return deliveryPreference == null || 
               deliveryPreference.equals("FRONT_DOOR") ||
               deliveryPreference.equals("BACK_DOOR") ||
               deliveryPreference.equals("SIDE_ENTRANCE") ||
               deliveryPreference.equals("LEAVE_WITH_CONCIERGE") ||
               deliveryPreference.equals("LEAVE_WITH_NEIGHBOR") ||
               deliveryPreference.equals("SECURE_LOCATION") ||
               deliveryPreference.equals("SIGNATURE_REQUIRED") ||
               deliveryPreference.equals("NO_SAFE_DROP");
    }
    
    public boolean isInternational() {
        return !country.equals("US");
    }
}