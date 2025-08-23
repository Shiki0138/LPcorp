package com.enterprise.user_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Customer Preferences DTO for API responses
 * Maps to the CustomerPreferences schema in the OpenAPI specification
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Customer communication and system preferences")
public class CustomerPreferencesDto {
    
    @Schema(description = "Communication preferences")
    private CommunicationPreferencesDto communication;
    
    @Schema(description = "Display preferences")
    private DisplayPreferencesDto display;
    
    @Schema(description = "Privacy preferences")
    private PrivacyPreferencesDto privacy;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Communication preferences")
    public static class CommunicationPreferencesDto {
        
        @Schema(description = "Email preferences")
        private EmailPreferencesDto email;
        
        @Schema(description = "SMS preferences")
        private SmsPreferencesDto sms;
        
        @Schema(description = "Push notification preferences")
        private PushPreferencesDto push;
        
        @Schema(description = "Communication frequency", allowableValues = {"IMMEDIATE", "DAILY", "WEEKLY", "MONTHLY", "NEVER"})
        private String frequency;
        
        @Schema(description = "Quiet hours start time", example = "22:00")
        private String quietHoursStart;
        
        @Schema(description = "Quiet hours end time", example = "08:00")
        private String quietHoursEnd;
        
        @Schema(description = "Weekend communications allowed")
        private Boolean weekendCommunications;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Email communication preferences")
    public static class EmailPreferencesDto {
        
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
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "SMS communication preferences")
    public static class SmsPreferencesDto {
        
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
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Push notification preferences")
    public static class PushPreferencesDto {
        
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
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Display and localization preferences")
    public static class DisplayPreferencesDto {
        
        @Schema(description = "Language preference", pattern = "^[a-z]{2}-[A-Z]{2}$", example = "en-US")
        private String language;
        
        @Schema(description = "Currency preference", pattern = "^[A-Z]{3}$", example = "USD")
        private String currency;
        
        @Schema(description = "Timezone preference", example = "America/New_York")
        private String timezone;
        
        @Schema(description = "Date format preference", allowableValues = {"MM/DD/YYYY", "DD/MM/YYYY", "YYYY-MM-DD"})
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
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Privacy and data preferences")
    public static class PrivacyPreferencesDto {
        
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
}