package com.hive.notification.domain.enums;

/**
 * Delivery strategy for multi-channel notifications
 */
public enum DeliveryStrategy {
    SINGLE_CHANNEL("single", "Send through one preferred channel"),
    FAILOVER("failover", "Try channels in order until successful"),
    BROADCAST("broadcast", "Send through all configured channels"),
    SMART("smart", "AI-optimized channel selection"),
    TIME_BASED("time_based", "Channel selection based on time of day"),
    A_B_TEST("a_b_test", "A/B test different channels");

    private final String code;
    private final String description;

    DeliveryStrategy(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}