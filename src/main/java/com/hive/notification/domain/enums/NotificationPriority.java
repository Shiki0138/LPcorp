package com.hive.notification.domain.enums;

/**
 * Priority levels for notification processing
 */
public enum NotificationPriority {
    LOW(1, "Low priority - process when convenient"),
    NORMAL(5, "Normal priority - standard processing"),
    HIGH(8, "High priority - expedited processing"),
    URGENT(10, "Urgent priority - immediate processing");

    private final int level;
    private final String description;

    NotificationPriority(int level, String description) {
        this.level = level;
        this.description = description;
    }

    public int getLevel() {
        return level;
    }

    public String getDescription() {
        return description;
    }

    public boolean isHigherThan(NotificationPriority other) {
        return this.level > other.level;
    }
}