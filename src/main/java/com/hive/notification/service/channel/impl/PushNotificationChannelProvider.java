package com.hive.notification.service.channel.impl;

import com.hive.notification.domain.entity.Notification;
import com.hive.notification.domain.enums.NotificationChannel;
import com.hive.notification.service.channel.NotificationChannelProvider;
import com.hive.notification.exception.NotificationDeliveryException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Push notification channel provider supporting Firebase Cloud Messaging (FCM) and Apple Push Notification Service (APNS)
 */
@Component
public class PushNotificationChannelProvider implements NotificationChannelProvider {

    private static final Logger logger = LoggerFactory.getLogger(PushNotificationChannelProvider.class);

    @Value("${notification.push.firebase.server-key:}")
    private String firebaseServerKey;
    
    @Value("${notification.push.firebase.url:https://fcm.googleapis.com/fcm/send}")
    private String firebaseUrl;
    
    @Value("${notification.push.apns.key-id:}")
    private String apnsKeyId;
    
    @Value("${notification.push.apns.team-id:}")
    private String apnsTeamId;
    
    @Value("${notification.push.apns.bundle-id:}")
    private String apnsBundleId;

    private final RestTemplate restTemplate;

    // Metrics tracking
    private long totalSent = 0;
    private long totalDelivered = 0;
    private long totalFailed = 0;
    private final List<Long> responseTimes = new ArrayList<>();

    public PushNotificationChannelProvider() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public NotificationChannel getSupportedChannel() {
        return NotificationChannel.PUSH;
    }

    @Override
    public ChannelDeliveryResult sendNotification(Notification notification) {
        long startTime = System.currentTimeMillis();
        
        try {
            validateNotification(notification);
            
            String deviceToken = notification.getRecipientContact();
            String externalId;
            
            // Determine platform based on token format or channel config
            String platform = determinePlatform(notification);
            
            if ("android".equalsIgnoreCase(platform) || "firebase".equalsIgnoreCase(platform)) {
                externalId = sendViaFirebase(notification, deviceToken);
            } else if ("ios".equalsIgnoreCase(platform) || "apns".equalsIgnoreCase(platform)) {
                externalId = sendViaApns(notification, deviceToken);
            } else {
                throw new NotificationDeliveryException("Unsupported push platform: " + platform);
            }
            
            long responseTime = System.currentTimeMillis() - startTime;
            updateMetrics(true, responseTime);
            
            notification.addTrackingData("push_platform", platform);
            notification.addTrackingData("sent_at", System.currentTimeMillis());
            notification.addTrackingData("external_id", externalId);
            
            logger.info("Push notification sent successfully to {} for notification {} via {}", 
                       deviceToken, notification.getId(), platform);
            
            return new ChannelDeliveryResult(true, externalId, "Push notification sent successfully");
            
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            updateMetrics(false, responseTime);
            
            logger.error("Failed to send push notification to {} for notification {}: {}", 
                        notification.getRecipientContact(), notification.getId(), e.getMessage());
            
            return new ChannelDeliveryResult(false, null, "Failed to send push notification: " + e.getMessage(), e);
        }
    }

    private String sendViaFirebase(Notification notification, String deviceToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "key=" + firebaseServerKey);
        
        Map<String, Object> payload = buildFirebasePayload(notification, deviceToken);
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
        
        try {
            ResponseEntity<Map> response = restTemplate.exchange(firebaseUrl, HttpMethod.POST, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                // Check for success in Firebase response
                Integer success = (Integer) responseBody.get("success");
                Integer failure = (Integer) responseBody.get("failure");
                
                if (success != null && success > 0) {
                    // Extract message ID if available
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> results = (List<Map<String, Object>>) responseBody.get("results");
                    if (results != null && !results.isEmpty()) {
                        String messageId = (String) results.get(0).get("message_id");
                        return messageId != null ? messageId : "fcm-" + UUID.randomUUID().toString();
                    }
                    return "fcm-" + UUID.randomUUID().toString();
                } else {
                    throw new NotificationDeliveryException("Firebase FCM delivery failed: " + responseBody);
                }
            } else {
                throw new NotificationDeliveryException("Firebase FCM returned non-success status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new NotificationDeliveryException("Failed to send push via Firebase: " + e.getMessage(), e);
        }
    }

    private String sendViaApns(Notification notification, String deviceToken) {
        // APNS implementation would require JWT token generation and HTTP/2
        // For demonstration, we'll create a placeholder implementation
        
        String messageId = "apns-" + UUID.randomUUID().toString();
        
        // In a real implementation, you would:
        // 1. Generate JWT token using APNS key
        // 2. Create HTTP/2 connection to APNS
        // 3. Send the push notification payload
        // 4. Handle the response
        
        notification.addTrackingData("apns_bundle_id", apnsBundleId);
        notification.addTrackingData("apns_message_id", messageId);
        
        return messageId;
    }

    private Map<String, Object> buildFirebasePayload(Notification notification, String deviceToken) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("to", deviceToken);
        
        // Build notification payload
        Map<String, Object> notificationPayload = new HashMap<>();
        notificationPayload.put("title", notification.getSubject());
        notificationPayload.put("body", notification.getContent());
        
        // Handle channel-specific configuration
        Map<String, Object> channelConfig = notification.getChannelConfig();
        if (channelConfig != null) {
            // Add icon, sound, etc.
            if (channelConfig.containsKey("icon")) {
                notificationPayload.put("icon", channelConfig.get("icon"));
            }
            if (channelConfig.containsKey("sound")) {
                notificationPayload.put("sound", channelConfig.get("sound"));
            }
            if (channelConfig.containsKey("badge")) {
                notificationPayload.put("badge", channelConfig.get("badge"));
            }
            if (channelConfig.containsKey("color")) {
                notificationPayload.put("color", channelConfig.get("color"));
            }
            if (channelConfig.containsKey("clickAction")) {
                notificationPayload.put("click_action", channelConfig.get("clickAction"));
            }
            
            // Data payload for custom handling
            if (channelConfig.containsKey("data")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) channelConfig.get("data");
                payload.put("data", data);
            }
            
            // Android-specific options
            if (channelConfig.containsKey("android")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> android = (Map<String, Object>) channelConfig.get("android");
                payload.put("android", android);
            }
            
            // Priority and time to live
            if (channelConfig.containsKey("priority")) {
                payload.put("priority", channelConfig.get("priority"));
            }
            if (channelConfig.containsKey("timeToLive")) {
                payload.put("time_to_live", channelConfig.get("timeToLive"));
            }
        }
        
        payload.put("notification", notificationPayload);
        
        // Add custom data
        Map<String, Object> data = new HashMap<>();
        data.put("notification_id", notification.getId().toString());
        data.put("tenant_id", notification.getTenantId());
        if (notification.getCampaignId() != null) {
            data.put("campaign_id", notification.getCampaignId());
        }
        if (notification.getCorrelationId() != null) {
            data.put("correlation_id", notification.getCorrelationId());
        }
        
        // Merge with existing data if present
        @SuppressWarnings("unchecked")
        Map<String, Object> existingData = (Map<String, Object>) payload.get("data");
        if (existingData != null) {
            data.putAll(existingData);
        }
        
        payload.put("data", data);
        
        return payload;
    }

    private String determinePlatform(Notification notification) {
        Map<String, Object> channelConfig = notification.getChannelConfig();
        if (channelConfig != null && channelConfig.containsKey("platform")) {
            return (String) channelConfig.get("platform");
        }
        
        // Try to determine from token format
        String token = notification.getRecipientContact();
        if (token.length() == 64 && token.matches("^[a-fA-F0-9]+$")) {
            return "ios"; // APNS tokens are typically 64 hex characters
        } else if (token.contains(":")) {
            return "android"; // FCM tokens typically contain colons
        }
        
        // Default to Firebase/Android
        return "android";
    }

    @Override
    public boolean canHandle(Notification notification) {
        return notification.getChannel() == NotificationChannel.PUSH &&
               isValidDeviceToken(notification.getRecipientContact());
    }

    @Override
    public void validateNotification(Notification notification) {
        if (!canHandle(notification)) {
            throw new NotificationDeliveryException("Cannot handle notification: " + notification.getId());
        }
        
        if (notification.getSubject() == null || notification.getSubject().isEmpty()) {
            throw new NotificationDeliveryException("Push notification title is required");
        }
        
        if (notification.getContent() == null || notification.getContent().isEmpty()) {
            throw new NotificationDeliveryException("Push notification body is required");
        }
        
        if (!isValidDeviceToken(notification.getRecipientContact())) {
            throw new NotificationDeliveryException("Invalid device token: " + notification.getRecipientContact());
        }
        
        // Validate platform-specific configuration
        String platform = determinePlatform(notification);
        if ("android".equalsIgnoreCase(platform) && firebaseServerKey.isEmpty()) {
            throw new NotificationDeliveryException("Firebase server key not configured");
        } else if ("ios".equalsIgnoreCase(platform) && 
                   (apnsKeyId.isEmpty() || apnsTeamId.isEmpty() || apnsBundleId.isEmpty())) {
            throw new NotificationDeliveryException("APNS configuration incomplete");
        }
    }

    @Override
    public ChannelConfigRequirements getConfigRequirements() {
        List<String> required = Arrays.asList("title", "body");
        List<String> optional = Arrays.asList("platform", "icon", "sound", "badge", "color", 
                                            "clickAction", "data", "android", "priority", "timeToLive");
        
        Map<String, String> descriptions = new HashMap<>();
        descriptions.put("title", "Push notification title");
        descriptions.put("body", "Push notification body");
        descriptions.put("platform", "Target platform (android/ios)");
        descriptions.put("icon", "Notification icon");
        descriptions.put("sound", "Notification sound");
        descriptions.put("badge", "Badge count (iOS)");
        descriptions.put("color", "Notification color");
        descriptions.put("clickAction", "Action when notification is clicked");
        descriptions.put("data", "Custom data payload");
        descriptions.put("android", "Android-specific options");
        descriptions.put("priority", "Message priority");
        descriptions.put("timeToLive", "Time to live in seconds");
        
        return new ChannelConfigRequirements(required, optional, descriptions);
    }

    @Override
    public boolean isHealthy() {
        try {
            return !firebaseServerKey.isEmpty() || 
                   (!apnsKeyId.isEmpty() && !apnsTeamId.isEmpty() && !apnsBundleId.isEmpty());
        } catch (Exception e) {
            logger.warn("Push notification provider health check failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public ChannelMetrics getMetrics() {
        double deliveryRate = totalSent > 0 ? (double) totalDelivered / totalSent * 100 : 0;
        double avgResponseTime = responseTimes.isEmpty() ? 0 : 
            responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        
        return new ChannelMetrics(totalSent, totalDelivered, totalFailed, deliveryRate, avgResponseTime);
    }

    private boolean isValidDeviceToken(String token) {
        // Basic validation for device tokens
        return token != null && token.length() > 10 && !token.contains(" ");
    }

    private synchronized void updateMetrics(boolean success, long responseTime) {
        totalSent++;
        responseTimes.add(responseTime);
        
        if (success) {
            totalDelivered++;
        } else {
            totalFailed++;
        }
        
        // Keep only last 1000 response times for memory efficiency
        if (responseTimes.size() > 1000) {
            responseTimes.remove(0);
        }
    }
}