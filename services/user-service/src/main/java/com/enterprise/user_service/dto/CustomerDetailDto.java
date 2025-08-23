package com.enterprise.user_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

/**
 * Detailed Customer DTO for comprehensive API responses
 * Extends CustomerDto with related entities (addresses, preferences, segments, statistics)
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Detailed customer information with related data")
public class CustomerDetailDto extends CustomerDto {
    
    @Schema(description = "Customer addresses")
    private List<AddressDto> addresses;
    
    @Schema(description = "Customer preferences")
    private CustomerPreferencesDto preferences;
    
    @Schema(description = "Customer segments")
    private List<CustomerSegmentDto> segments;
    
    @Schema(description = "Customer statistics")
    private CustomerStatisticsDto statistics;
    
    @Schema(description = "Customer profile information")
    private UserProfileDto profile;
    
    @Builder(builderMethodName = "detailBuilder")
    public CustomerDetailDto(CustomerDto customer, List<AddressDto> addresses, 
                            CustomerPreferencesDto preferences, List<CustomerSegmentDto> segments,
                            CustomerStatisticsDto statistics, UserProfileDto profile) {
        super();
        // Copy fields from base CustomerDto
        if (customer != null) {
            this.setId(customer.getId());
            this.setEmail(customer.getEmail());
            this.setFirstName(customer.getFirstName());
            this.setLastName(customer.getLastName());
            this.setPhoneNumber(customer.getPhoneNumber());
            this.setDateOfBirth(customer.getDateOfBirth());
            this.setStatus(customer.getStatus());
            this.setTier(customer.getTier());
            this.setCreatedAt(customer.getCreatedAt());
            this.setUpdatedAt(customer.getUpdatedAt());
            this.setVerifiedAt(customer.getVerifiedAt());
            this.setEmailVerified(customer.getEmailVerified());
            this.setPhoneVerified(customer.getPhoneVerified());
            this.setIdentityVerified(customer.getIdentityVerified());
            this.setProfileCompletionScore(customer.getProfileCompletionScore());
            this.setTotalOrders(customer.getTotalOrders());
            this.setTotalSpent(customer.getTotalSpent());
            this.setLastOrderAt(customer.getLastOrderAt());
            this.setMarketingConsent(customer.getMarketingConsent());
            this.setTermsAccepted(customer.getTermsAccepted());
            this.setTermsAcceptedAt(customer.getTermsAcceptedAt());
        }
        
        this.addresses = addresses;
        this.preferences = preferences;
        this.segments = segments;
        this.statistics = statistics;
        this.profile = profile;
    }
}