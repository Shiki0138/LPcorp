package com.hive.notification.service.delivery;

import com.hive.notification.domain.enums.NotificationChannel;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Service for optimizing notification delivery
 */
@Service
public class DeliveryOptimizationService {

    /**
     * Get optimal channel for user based on historical data and preferences
     */
    public NotificationChannel getOptimalChannel(String tenantId, String userId, String category) {
        // This would use ML models and historical data to determine the best channel
        // For now, we'll use simple heuristics
        
        LocalTime now = LocalTime.now();
        
        // Business hours: prefer email or in-app
        if (now.isAfter(LocalTime.of(9, 0)) && now.isBefore(LocalTime.of(17, 0))) {
            return NotificationChannel.EMAIL;
        }
        
        // Evening: prefer push or SMS
        if (now.isAfter(LocalTime.of(17, 0)) && now.isBefore(LocalTime.of(22, 0))) {
            return NotificationChannel.PUSH;
        }
        
        // Default to in-app for quiet hours
        return NotificationChannel.IN_APP;
    }

    /**
     * Get optimal channel from a list of available channels
     */
    public NotificationChannel getOptimalChannel(String tenantId, String userId, 
                                               List<NotificationChannel> availableChannels, String category) {
        if (availableChannels == null || availableChannels.isEmpty()) {
            return NotificationChannel.EMAIL; // Default fallback
        }
        
        NotificationChannel optimal = getOptimalChannel(tenantId, userId, category);
        
        // Return optimal if available, otherwise first available
        return availableChannels.contains(optimal) ? optimal : availableChannels.get(0);
    }

    /**
     * Determine optimal delivery time for user
     */
    public LocalDateTime getOptimalDeliveryTime(String tenantId, String userId, String category) {
        // This would analyze user behavior patterns to determine best delivery time
        // For now, return immediate delivery during business hours, otherwise schedule for next business day
        
        LocalDateTime now = LocalDateTime.now();
        LocalTime currentTime = now.toLocalTime();
        
        // If within business hours, deliver immediately
        if (currentTime.isAfter(LocalTime.of(9, 0)) && currentTime.isBefore(LocalTime.of(17, 0))) {
            return now;
        }
        
        // Otherwise, schedule for 9 AM next business day
        LocalDateTime nextBusinessDay = now.plusDays(1);
        while (isWeekend(nextBusinessDay)) {
            nextBusinessDay = nextBusinessDay.plusDays(1);
        }
        
        return nextBusinessDay.withHour(9).withMinute(0).withSecond(0);
    }

    /**
     * Calculate retry delay based on previous attempts
     */
    public long calculateRetryDelay(int attemptNumber, NotificationChannel channel) {
        // Exponential backoff with channel-specific base delays
        long baseDelay = switch (channel) {
            case EMAIL -> 60; // 1 minute
            case SMS -> 30;   // 30 seconds
            case PUSH -> 15;  // 15 seconds
            default -> 60;    // 1 minute default
        };
        
        // Exponential backoff: base * 2^(attempt-1), capped at 1 hour
        long delay = (long) (baseDelay * Math.pow(2, attemptNumber - 1));
        return Math.min(delay, 3600); // Cap at 1 hour
    }

    /**
     * Determine if it's a good time to send notifications to user
     */
    public boolean isOptimalTimeToSend(String tenantId, String userId, NotificationChannel channel) {
        LocalTime now = LocalTime.now();
        
        return switch (channel) {
            case EMAIL -> now.isAfter(LocalTime.of(8, 0)) && now.isBefore(LocalTime.of(20, 0));
            case SMS -> now.isAfter(LocalTime.of(9, 0)) && now.isBefore(LocalTime.of(21, 0));
            case PUSH -> true; // Push notifications can be sent anytime
            case IN_APP -> true; // In-app notifications are always okay
            default -> now.isAfter(LocalTime.of(9, 0)) && now.isBefore(LocalTime.of(18, 0));
        };
    }

    /**
     * Get channel priority score for user
     */
    public int getChannelPriorityScore(String tenantId, String userId, NotificationChannel channel, String category) {
        // This would use ML models to score channels based on user engagement
        // For now, return static scores based on channel effectiveness
        
        return switch (channel) {
            case PUSH -> 10;
            case EMAIL -> 8;
            case SMS -> 7;
            case IN_APP -> 6;
            case WHATSAPP -> 5;
            case SLACK -> 4;
            case TEAMS -> 3;
            case WEBHOOK -> 2;
        };
    }

    private boolean isWeekend(LocalDateTime dateTime) {
        int dayOfWeek = dateTime.getDayOfWeek().getValue();
        return dayOfWeek == 6 || dayOfWeek == 7; // Saturday or Sunday
    }
}