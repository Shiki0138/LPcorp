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
import java.util.regex.Pattern;

/**
 * SMS notification channel provider using Twilio or AWS SNS
 */
@Component
public class SmsChannelProvider implements NotificationChannelProvider {

    private static final Logger logger = LoggerFactory.getLogger(SmsChannelProvider.class);

    @Value("${notification.sms.provider:twilio}")
    private String smsProvider;
    
    @Value("${notification.sms.twilio.account-sid:}")
    private String twilioAccountSid;
    
    @Value("${notification.sms.twilio.auth-token:}")
    private String twilioAuthToken;
    
    @Value("${notification.sms.twilio.from-number:}")
    private String twilioFromNumber;
    
    @Value("${notification.sms.aws.access-key:}")
    private String awsAccessKey;
    
    @Value("${notification.sms.aws.secret-key:}")
    private String awsSecretKey;
    
    @Value("${notification.sms.aws.region:us-east-1}")
    private String awsRegion;

    private final RestTemplate restTemplate;
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[1-9]\\d{1,14}$");
    private static final int SMS_MAX_LENGTH = 1600; // SMS length limit

    // Metrics tracking
    private long totalSent = 0;
    private long totalDelivered = 0;
    private long totalFailed = 0;
    private final List<Long> responseTimes = new ArrayList<>();

    public SmsChannelProvider() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public NotificationChannel getSupportedChannel() {
        return NotificationChannel.SMS;
    }

    @Override
    public ChannelDeliveryResult sendNotification(Notification notification) {
        long startTime = System.currentTimeMillis();
        
        try {
            validateNotification(notification);
            
            String externalId;
            if ("twilio".equalsIgnoreCase(smsProvider)) {
                externalId = sendViaTwilio(notification);
            } else if ("aws".equalsIgnoreCase(smsProvider)) {
                externalId = sendViaAwsSns(notification);
            } else {
                throw new NotificationDeliveryException("Unsupported SMS provider: " + smsProvider);
            }
            
            long responseTime = System.currentTimeMillis() - startTime;
            updateMetrics(true, responseTime);
            
            notification.addTrackingData("sms_provider", smsProvider);
            notification.addTrackingData("sent_at", System.currentTimeMillis());
            notification.addTrackingData("external_id", externalId);
            
            logger.info("SMS sent successfully to {} for notification {} via {}", 
                       notification.getRecipientContact(), notification.getId(), smsProvider);
            
            return new ChannelDeliveryResult(true, externalId, "SMS sent successfully");
            
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            updateMetrics(false, responseTime);
            
            logger.error("Failed to send SMS to {} for notification {}: {}", 
                        notification.getRecipientContact(), notification.getId(), e.getMessage());
            
            return new ChannelDeliveryResult(false, null, "Failed to send SMS: " + e.getMessage(), e);
        }
    }

    private String sendViaTwilio(Notification notification) {
        String url = String.format("https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json", twilioAccountSid);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(twilioAccountSid, twilioAuthToken);
        
        // Prepare SMS content
        String smsContent = prepareSmsContent(notification);
        
        // Build form data
        StringBuilder formData = new StringBuilder();
        formData.append("From=").append(twilioFromNumber);
        formData.append("&To=").append(notification.getRecipientContact());
        formData.append("&Body=").append(java.net.URLEncoder.encode(smsContent, java.nio.charset.StandardCharsets.UTF_8));
        
        // Add optional parameters from channel config
        Map<String, Object> channelConfig = notification.getChannelConfig();
        if (channelConfig != null) {
            if (channelConfig.containsKey("statusCallback")) {
                formData.append("&StatusCallback=").append(channelConfig.get("statusCallback"));
            }
            if (channelConfig.containsKey("validityPeriod")) {
                formData.append("&ValidityPeriod=").append(channelConfig.get("validityPeriod"));
            }
        }
        
        HttpEntity<String> request = new HttpEntity<>(formData.toString(), headers);
        
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                return (String) responseBody.get("sid");
            } else {
                throw new NotificationDeliveryException("Twilio API returned non-success status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new NotificationDeliveryException("Failed to send SMS via Twilio: " + e.getMessage(), e);
        }
    }

    private String sendViaAwsSns(Notification notification) {
        // AWS SNS implementation would go here
        // This is a simplified version - in production, use AWS SDK
        
        String smsContent = prepareSmsContent(notification);
        
        // For demonstration purposes, we'll simulate AWS SNS call
        String messageId = "aws-sns-" + UUID.randomUUID().toString();
        
        notification.addTrackingData("aws_region", awsRegion);
        
        return messageId;
    }

    private String prepareSmsContent(Notification notification) {
        String content = notification.getContent();
        
        // Add unsubscribe link if required
        Map<String, Object> channelConfig = notification.getChannelConfig();
        if (channelConfig != null && Boolean.TRUE.equals(channelConfig.get("addUnsubscribeLink"))) {
            String unsubscribeUrl = String.format(
                "https://api.hive.com/notifications/unsubscribe?tenant=%s&user=%s&channel=sms", 
                notification.getTenantId(), 
                notification.getRecipientId()
            );
            content += "\n\nTo stop: " + unsubscribeUrl;
        }
        
        // Truncate if too long
        if (content.length() > SMS_MAX_LENGTH) {
            content = content.substring(0, SMS_MAX_LENGTH - 3) + "...";
            logger.warn("SMS content truncated for notification {}", notification.getId());
        }
        
        return content;
    }

    @Override
    public boolean canHandle(Notification notification) {
        return notification.getChannel() == NotificationChannel.SMS &&
               isValidPhoneNumber(notification.getRecipientContact());
    }

    @Override
    public void validateNotification(Notification notification) {
        if (!canHandle(notification)) {
            throw new NotificationDeliveryException("Cannot handle notification: " + notification.getId());
        }
        
        if (notification.getContent() == null || notification.getContent().isEmpty()) {
            throw new NotificationDeliveryException("SMS content is required");
        }
        
        if (!isValidPhoneNumber(notification.getRecipientContact())) {
            throw new NotificationDeliveryException("Invalid phone number: " + notification.getRecipientContact());
        }
        
        // Validate provider configuration
        if ("twilio".equalsIgnoreCase(smsProvider)) {
            if (twilioAccountSid.isEmpty() || twilioAuthToken.isEmpty() || twilioFromNumber.isEmpty()) {
                throw new NotificationDeliveryException("Twilio configuration incomplete");
            }
        } else if ("aws".equalsIgnoreCase(smsProvider)) {
            if (awsAccessKey.isEmpty() || awsSecretKey.isEmpty()) {
                throw new NotificationDeliveryException("AWS SNS configuration incomplete");
            }
        }
    }

    @Override
    public ChannelConfigRequirements getConfigRequirements() {
        List<String> required = Arrays.asList("content");
        List<String> optional = Arrays.asList("statusCallback", "validityPeriod", "addUnsubscribeLink");
        
        Map<String, String> descriptions = new HashMap<>();
        descriptions.put("content", "SMS message content (max 1600 characters)");
        descriptions.put("statusCallback", "URL for delivery status callbacks");
        descriptions.put("validityPeriod", "Message validity period in seconds");
        descriptions.put("addUnsubscribeLink", "Add unsubscribe link to message");
        
        return new ChannelConfigRequirements(required, optional, descriptions);
    }

    @Override
    public boolean isHealthy() {
        try {
            if ("twilio".equalsIgnoreCase(smsProvider)) {
                return !twilioAccountSid.isEmpty() && !twilioAuthToken.isEmpty() && !twilioFromNumber.isEmpty();
            } else if ("aws".equalsIgnoreCase(smsProvider)) {
                return !awsAccessKey.isEmpty() && !awsSecretKey.isEmpty();
            }
            return false;
        } catch (Exception e) {
            logger.warn("SMS provider health check failed: {}", e.getMessage());
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

    private boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber != null && PHONE_PATTERN.matcher(phoneNumber).matches();
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