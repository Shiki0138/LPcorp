package com.enterprise.user_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Address DTO for API responses
 * Maps to the Address schema in the OpenAPI specification
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Customer address information")
public class AddressDto {
    
    @Schema(description = "Address unique identifier", format = "uuid", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;
    
    @Schema(description = "Address type", allowableValues = {"billing", "shipping", "both"}, example = "billing")
    private String type;
    
    @Schema(description = "Default address flag", example = "true")
    private boolean isDefault;
    
    @Schema(description = "Address line 1", maxLength = 100, example = "123 Main Street")
    private String line1;
    
    @Schema(description = "Address line 2 (optional)", maxLength = 100, example = "Apt 4B", nullable = true)
    private String line2;
    
    @Schema(description = "City name", maxLength = 50, example = "New York")
    private String city;
    
    @Schema(description = "State or province", maxLength = 50, example = "NY")
    private String state;
    
    @Schema(description = "Postal or ZIP code", maxLength = 20, example = "10001")
    private String postalCode;
    
    @Schema(description = "Country code (ISO 3166-1 alpha-2)", pattern = "^[A-Z]{2}$", example = "US")
    private String country;
    
    // Extended address fields
    @Schema(description = "Address label", maxLength = 50, example = "Home")
    private String label;
    
    @Schema(description = "Company name", maxLength = 100, example = "Acme Corp")
    private String companyName;
    
    @Schema(description = "Attention to", maxLength = 100, example = "John Doe")
    private String attentionTo;
    
    @Schema(description = "Address validation status")
    private Boolean isValidated;
    
    @Schema(description = "Validation timestamp", format = "date-time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime validatedAt;
    
    @Schema(description = "Address usage count")
    private Long usageCount;
    
    @Schema(description = "Last usage timestamp", format = "date-time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime lastUsedAt;
    
    @Schema(description = "Delivery instructions", maxLength = 500, example = "Leave at front door")
    private String deliveryInstructions;
    
    @Schema(description = "Delivery preference", allowableValues = {
        "FRONT_DOOR", "BACK_DOOR", "SIDE_ENTRANCE", "LEAVE_WITH_CONCIERGE", 
        "LEAVE_WITH_NEIGHBOR", "SECURE_LOCATION", "SIGNATURE_REQUIRED", "NO_SAFE_DROP"
    }, example = "FRONT_DOOR")
    private String deliveryPreference;
    
    @Schema(description = "Address creation timestamp", format = "date-time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime createdAt;
    
    @Schema(description = "Address last update timestamp", format = "date-time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime updatedAt;
    
    // Computed fields
    @Schema(description = "Formatted address string", example = "123 Main Street\\nApt 4B\\nNew York, NY 10001\\nUS")
    private String formattedAddress;
    
    @Schema(description = "Short address string", example = "123 Main Street, Apt 4B, New York, NY 10001")
    private String shortAddress;
    
    @Schema(description = "International address flag")
    private Boolean isInternational;
}