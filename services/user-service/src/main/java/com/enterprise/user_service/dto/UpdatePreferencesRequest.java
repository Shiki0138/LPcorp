package com.enterprise.user_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating customer preferences
 * Maps to the UpdatePreferencesRequest schema in the OpenAPI specification
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update customer preferences")
public class UpdatePreferencesRequest {
    
    @Schema(description = "Communication preferences")
    private CommunicationPreferencesRequest communication;
    
    @Schema(description = "Display preferences")
    private DisplayPreferencesRequest display;
    
    @Schema(description = "Privacy preferences")
    private PrivacyPreferencesRequest privacy;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Communication preferences update")
    public static class CommunicationPreferencesRequest {
        
        @Schema(description = "Email preferences")
        private EmailPreferencesRequest email;
        
        @Schema(description = "SMS preferences")
        private SmsPreferencesRequest sms;
        
        @Schema(description = "Push notification preferences")
        private PushPreferencesRequest push;
        
        @Schema(description = "Communication frequency", allowableValues = {"IMMEDIATE", "DAILY", "WEEKLY", "MONTHLY", "NEVER"})
        private String frequency;
        
        @Schema(description = "Quiet hours start time (HH:mm format)", example = "22:00")
        @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "Quiet hours start must be in HH:mm format")
        private String quietHoursStart;
        
        @Schema(description = "Quiet hours end time (HH:mm format)", example = "08:00")
        @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "Quiet hours end must be in HH:mm format")
        private String quietHoursEnd;
        
        @Schema(description = "Weekend communications allowed")
        private Boolean weekendCommunications;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Email preferences update")
    public static class EmailPreferencesRequest {
        
        @Schema(description = "Marketing emails enabled")
        private Boolean marketing;
        
        @Schema(description = "Transactional emails enabled")
        private Boolean transactional;
        
        @Schema(description = "Newsletter subscription")
        private Boolean newsletter;
        
        @Schema(description = "Promotional emails enabled")
        private Boolean promotions;
        
        @Schema(description = "Product updates enabled")
        private Boolean productUpdates;
        
        @Schema(description = "Security alerts enabled")
        private Boolean securityAlerts;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "SMS preferences update")
    public static class SmsPreferencesRequest {
        
        @Schema(description = "Marketing SMS enabled")
        private Boolean marketing;
        
        @Schema(description = "Transactional SMS enabled")
        private Boolean transactional;
        
        @Schema(description = "Order updates via SMS")
        private Boolean orderUpdates;
        
        @Schema(description = "Security alerts via SMS")
        private Boolean securityAlerts;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Push notification preferences update")
    public static class PushPreferencesRequest {
        
        @Schema(description = "Push notifications enabled")
        private Boolean enabled;
        
        @Schema(description = "Order updates push notifications")
        private Boolean orderUpdates;
        
        @Schema(description = "Promotional push notifications")
        private Boolean promotions;
        
        @Schema(description = "News push notifications")
        private Boolean news;
        
        @Schema(description = "Reminder push notifications")
        private Boolean reminders;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Display preferences update")
    public static class DisplayPreferencesRequest {
        
        @Schema(description = "Language preference", pattern = "^[a-z]{2}-[A-Z]{2}$", example = "en-US")
        @Pattern(regexp = "^[a-z]{2}-[A-Z]{2}$", message = "Language must be in format 'en-US'")
        private String language;
        
        @Schema(description = "Currency preference", pattern = "^[A-Z]{3}$", example = "USD")
        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid ISO 4217 code")
        private String currency;
        
        @Schema(description = "Timezone preference", example = "America/New_York")
        private String timezone;
        
        @Schema(description = "Date format preference", allowableValues = {"MM_DD_YYYY", "DD_MM_YYYY", "YYYY_MM_DD"})
        private String dateFormat;
        
        @Schema(description = "Theme preference", allowableValues = {"LIGHT", "DARK", "AUTO"})
        private String theme;
        
        @Schema(description = "Number format preference", allowableValues = {"US", "EUROPEAN", "INTERNATIONAL"})
        private String numberFormat;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Privacy preferences update")
    public static class PrivacyPreferencesRequest {
        
        @Schema(description = "Third party data sharing allowed")
        private Boolean dataSharingThirdParty;
        
        @Schema(description = "Partner data sharing allowed")
        private Boolean dataSharingPartners;
        
        @Schema(description = "Analytics tracking enabled")
        private Boolean analyticsTracking;
        
        @Schema(description = "Personalized ads enabled")
        private Boolean personalizedAds;
        
        @Schema(description = "Location tracking enabled")
        private Boolean locationTracking;
        
        @Schema(description = "Profile visibility", allowableValues = {"PUBLIC", "PRIVATE", "FRIENDS_ONLY"})
        private String profileVisibility;
    }
    
    // Validation methods
    public boolean isValidFrequency() {
        if (communication == null || communication.frequency == null) {
            return true;
        }
        return communication.frequency.equals("IMMEDIATE") ||
               communication.frequency.equals("DAILY") ||
               communication.frequency.equals("WEEKLY") ||
               communication.frequency.equals("MONTHLY") ||
               communication.frequency.equals("NEVER");
    }
    
    public boolean isValidTheme() {
        if (display == null || display.theme == null) {
            return true;
        }
        return display.theme.equals("LIGHT") ||
               display.theme.equals("DARK") ||
               display.theme.equals("AUTO");
    }
    
    public boolean isValidDateFormat() {
        if (display == null || display.dateFormat == null) {
            return true;
        }
        return display.dateFormat.equals("MM_DD_YYYY") ||
               display.dateFormat.equals("DD_MM_YYYY") ||
               display.dateFormat.equals("YYYY_MM_DD");
    }
    
    public boolean isValidProfileVisibility() {
        if (privacy == null || privacy.profileVisibility == null) {
            return true;
        }
        return privacy.profileVisibility.equals("PUBLIC") ||
               privacy.profileVisibility.equals("PRIVATE") ||
               privacy.profileVisibility.equals("FRIENDS_ONLY");
    }
}