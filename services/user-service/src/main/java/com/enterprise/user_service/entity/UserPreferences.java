package com.enterprise.user_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * UserPreferences entity for managing user communication and display preferences
 * Handles consent management, communication settings, and personalization options
 */
@Entity
@Table(name = "user_preferences")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class UserPreferences {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
    
    // Communication Preferences - Email
    @Column(name = "email_marketing", nullable = false)
    @Builder.Default
    private boolean emailMarketing = false;
    
    @Column(name = "email_transactional", nullable = false)
    @Builder.Default
    private boolean emailTransactional = true;
    
    @Column(name = "email_newsletter", nullable = false)
    @Builder.Default
    private boolean emailNewsletter = false;
    
    @Column(name = "email_promotions", nullable = false)
    @Builder.Default
    private boolean emailPromotions = false;
    
    @Column(name = "email_product_updates", nullable = false)
    @Builder.Default
    private boolean emailProductUpdates = false;
    
    @Column(name = "email_security_alerts", nullable = false)
    @Builder.Default
    private boolean emailSecurityAlerts = true;
    
    // Communication Preferences - SMS
    @Column(name = "sms_marketing", nullable = false)
    @Builder.Default
    private boolean smsMarketing = false;
    
    @Column(name = "sms_transactional", nullable = false)
    @Builder.Default
    private boolean smsTransactional = false;
    
    @Column(name = "sms_order_updates", nullable = false)
    @Builder.Default
    private boolean smsOrderUpdates = false;
    
    @Column(name = "sms_security_alerts", nullable = false)
    @Builder.Default
    private boolean smsSecurityAlerts = false;
    
    // Communication Preferences - Push Notifications
    @Column(name = "push_enabled", nullable = false)
    @Builder.Default
    private boolean pushEnabled = false;
    
    @Column(name = "push_order_updates", nullable = false)
    @Builder.Default
    private boolean pushOrderUpdates = false;
    
    @Column(name = "push_promotions", nullable = false)
    @Builder.Default
    private boolean pushPromotions = false;
    
    @Column(name = "push_news", nullable = false)
    @Builder.Default
    private boolean pushNews = false;
    
    @Column(name = "push_reminders", nullable = false)
    @Builder.Default
    private boolean pushReminders = false;
    
    // Communication Timing Preferences
    @Enumerated(EnumType.STRING)
    @Column(name = "communication_frequency", length = 20)
    @Builder.Default
    private CommunicationFrequency communicationFrequency = CommunicationFrequency.WEEKLY;
    
    @Column(name = "quiet_hours_start", length = 5)
    @Builder.Default
    private String quietHoursStart = "22:00"; // 10 PM
    
    @Column(name = "quiet_hours_end", length = 5)
    @Builder.Default
    private String quietHoursEnd = "08:00"; // 8 AM
    
    @Column(name = "weekend_communications", nullable = false)
    @Builder.Default
    private boolean weekendCommunications = true;
    
    // Display Preferences
    @Pattern(regexp = "^[a-z]{2}-[A-Z]{2}$", message = "Language must be in format 'en-US'")
    @Column(name = "language", length = 10)
    @Builder.Default
    private String language = "en-US";
    
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid ISO 4217 code")
    @Column(name = "currency", length = 3)
    @Builder.Default
    private String currency = "USD";
    
    @Column(name = "timezone", length = 50)
    @Builder.Default
    private String timezone = "America/New_York";
    
    @Enumerated(EnumType.STRING)
    @Column(name = "date_format", length = 20)
    @Builder.Default
    private DateFormat dateFormat = DateFormat.MM_DD_YYYY;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "number_format", length = 20)
    @Builder.Default
    private NumberFormat numberFormat = NumberFormat.US;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "theme", length = 20)
    @Builder.Default
    private Theme theme = Theme.LIGHT;
    
    // Privacy and Data Preferences
    @Column(name = "data_sharing_third_party", nullable = false)
    @Builder.Default
    private boolean dataSharingThirdParty = false;
    
    @Column(name = "data_sharing_partners", nullable = false)
    @Builder.Default
    private boolean dataSharingPartners = false;
    
    @Column(name = "analytics_tracking", nullable = false)
    @Builder.Default
    private boolean analyticsTracking = true;
    
    @Column(name = "personalized_ads", nullable = false)
    @Builder.Default
    private boolean personalizedAds = false;
    
    @Column(name = "location_tracking", nullable = false)
    @Builder.Default
    private boolean locationTracking = false;
    
    // Shopping and Commerce Preferences
    @Column(name = "save_payment_methods", nullable = false)
    @Builder.Default
    private boolean savePaymentMethods = false;
    
    @Column(name = "save_shipping_addresses", nullable = false)
    @Builder.Default
    private boolean saveShippingAddresses = true;
    
    @Column(name = "wishlist_public", nullable = false)
    @Builder.Default
    private boolean wishlistPublic = false;
    
    @Column(name = "purchase_history_public", nullable = false)
    @Builder.Default
    private boolean purchaseHistoryPublic = false;
    
    @Column(name = "auto_reorder_enabled", nullable = false)
    @Builder.Default
    private boolean autoReorderEnabled = false;
    
    @Column(name = "price_alerts_enabled", nullable = false)
    @Builder.Default
    private boolean priceAlertsEnabled = false;
    
    // Accessibility Preferences
    @Column(name = "high_contrast_mode", nullable = false)
    @Builder.Default
    private boolean highContrastMode = false;
    
    @Column(name = "large_text_mode", nullable = false)
    @Builder.Default
    private boolean largeTextMode = false;
    
    @Column(name = "screen_reader_optimized", nullable = false)
    @Builder.Default
    private boolean screenReaderOptimized = false;
    
    @Column(name = "keyboard_navigation", nullable = false)
    @Builder.Default
    private boolean keyboardNavigation = false;
    
    // Content Preferences
    @Column(name = "content_maturity_level", length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ContentMaturityLevel contentMaturityLevel = ContentMaturityLevel.GENERAL;
    
    @Column(name = "preferred_content_categories", length = 1000)
    private String preferredContentCategories; // JSON array
    
    @Column(name = "blocked_content_categories", length = 1000)
    private String blockedContentCategories; // JSON array
    
    // Consent tracking
    @Column(name = "marketing_consent_date")
    private LocalDateTime marketingConsentDate;
    
    @Column(name = "analytics_consent_date")
    private LocalDateTime analyticsConsentDate;
    
    @Column(name = "cookies_consent_date")
    private LocalDateTime cookiesConsentDate;
    
    @Column(name = "gdpr_consent_version", length = 10)
    private String gdprConsentVersion;
    
    @Column(name = "terms_version_accepted", length = 10)
    private String termsVersionAccepted;
    
    @Column(name = "privacy_policy_version_accepted", length = 10)
    private String privacyPolicyVersionAccepted;
    
    // Preferences metadata
    @Column(name = "preferences_last_updated")
    private LocalDateTime preferencesLastUpdated;
    
    @Column(name = "preferences_reminder_sent")
    private LocalDateTime preferencesReminderSent;
    
    @Column(name = "unsubscribe_token", length = 100)
    private String unsubscribeToken;
    
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
    public enum CommunicationFrequency {
        IMMEDIATE, DAILY, WEEKLY, MONTHLY, NEVER
    }
    
    public enum DateFormat {
        MM_DD_YYYY("MM/dd/yyyy"),
        DD_MM_YYYY("dd/MM/yyyy"),
        YYYY_MM_DD("yyyy-MM-dd");
        
        private final String format;
        
        DateFormat(String format) {
            this.format = format;
        }
        
        public String getFormat() {
            return format;
        }
    }
    
    public enum NumberFormat {
        US, EUROPEAN, INTERNATIONAL
    }
    
    public enum Theme {
        LIGHT, DARK, AUTO
    }
    
    public enum ContentMaturityLevel {
        GENERAL, TEEN, MATURE, ADULT
    }
    
    // Helper methods
    public boolean hasAnyMarketingConsent() {
        return emailMarketing || smsMarketing || pushPromotions || emailPromotions;
    }
    
    public boolean hasAnyTransactionalConsent() {
        return emailTransactional || smsTransactional || pushOrderUpdates;
    }
    
    public void updateMarketingConsent(boolean consent) {
        this.emailMarketing = consent;
        this.smsMarketing = consent;
        this.pushPromotions = consent;
        this.emailPromotions = consent;
        this.emailNewsletter = consent;
        if (consent) {
            this.marketingConsentDate = LocalDateTime.now();
        }
    }
    
    public void updateAllCommunications(boolean enabled) {
        this.emailMarketing = enabled;
        this.emailNewsletter = enabled;
        this.emailPromotions = enabled;
        this.emailProductUpdates = enabled;
        this.smsMarketing = enabled;
        this.smsOrderUpdates = enabled;
        this.pushPromotions = enabled;
        this.pushNews = enabled;
        this.pushReminders = enabled;
    }
    
    public boolean isInQuietHours() {
        LocalDateTime now = LocalDateTime.now();
        int currentHour = now.getHour();
        int startHour = Integer.parseInt(quietHoursStart.split(":")[0]);
        int endHour = Integer.parseInt(quietHoursEnd.split(":")[0]);
        
        if (startHour > endHour) { // Crosses midnight
            return currentHour >= startHour || currentHour < endHour;
        } else {
            return currentHour >= startHour && currentHour < endHour;
        }
    }
    
    public void generateUnsubscribeToken() {
        this.unsubscribeToken = UUID.randomUUID().toString().replace("-", "");
    }
    
    public void updatePreferencesTimestamp() {
        this.preferencesLastUpdated = LocalDateTime.now();
    }
}