package com.hive.notification.domain.enums;

/**
 * Enumeration of available notification channels
 */
public enum NotificationChannel {
    EMAIL("email", "Email Notifications", true),
    SMS("sms", "SMS Notifications", true),
    PUSH("push", "Push Notifications", true),
    IN_APP("in_app", "In-App Notifications", false),
    WHATSAPP("whatsapp", "WhatsApp Notifications", true),
    SLACK("slack", "Slack Notifications", true),
    TEAMS("teams", "Microsoft Teams Notifications", true),
    WEBHOOK("webhook", "Webhook Notifications", true);

    private final String code;
    private final String displayName;
    private final boolean external;

    NotificationChannel(String code, String displayName, boolean external) {
        this.code = code;
        this.displayName = displayName;
        this.external = external;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isExternal() {
        return external;
    }

    public static NotificationChannel fromCode(String code) {
        for (NotificationChannel channel : values()) {
            if (channel.code.equals(code)) {
                return channel;
            }
        }
        throw new IllegalArgumentException("Unknown notification channel: " + code);
    }
}