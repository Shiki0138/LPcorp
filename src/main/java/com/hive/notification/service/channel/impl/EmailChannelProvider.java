package com.hive.notification.service.channel.impl;

import com.hive.notification.domain.entity.Notification;
import com.hive.notification.domain.enums.NotificationChannel;
import com.hive.notification.service.channel.NotificationChannelProvider;
import com.hive.notification.exception.NotificationDeliveryException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.*;

/**
 * Email notification channel provider
 */
@Component
public class EmailChannelProvider implements NotificationChannelProvider {

    private static final Logger logger = LoggerFactory.getLogger(EmailChannelProvider.class);

    private final JavaMailSender mailSender;
    
    @Value("${notification.email.from-address:noreply@hive.com}")
    private String fromAddress;
    
    @Value("${notification.email.from-name:Hive Notifications}")
    private String fromName;
    
    @Value("${notification.email.track-opens:true}")
    private boolean trackOpens;
    
    @Value("${notification.email.track-clicks:true}")
    private boolean trackClicks;

    // Metrics tracking
    private long totalSent = 0;
    private long totalDelivered = 0;
    private long totalFailed = 0;
    private final List<Long> responseTimes = new ArrayList<>();

    @Autowired
    public EmailChannelProvider(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public NotificationChannel getSupportedChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public ChannelDeliveryResult sendNotification(Notification notification) {
        long startTime = System.currentTimeMillis();
        
        try {
            validateNotification(notification);
            
            MimeMessage message = createMimeMessage(notification);
            mailSender.send(message);
            
            long responseTime = System.currentTimeMillis() - startTime;
            updateMetrics(true, responseTime);
            
            // Generate tracking ID for email
            String trackingId = UUID.randomUUID().toString();
            notification.addTrackingData("email_tracking_id", trackingId);
            notification.addTrackingData("sent_at", System.currentTimeMillis());
            
            logger.info("Email sent successfully to {} for notification {}", 
                       notification.getRecipientContact(), notification.getId());
            
            return new ChannelDeliveryResult(true, trackingId, "Email sent successfully");
            
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            updateMetrics(false, responseTime);
            
            logger.error("Failed to send email to {} for notification {}: {}", 
                        notification.getRecipientContact(), notification.getId(), e.getMessage());
            
            return new ChannelDeliveryResult(false, null, "Failed to send email: " + e.getMessage(), e);
        }
    }

    private MimeMessage createMimeMessage(Notification notification) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        // Basic message setup
        helper.setFrom(fromAddress, fromName);
        helper.setTo(notification.getRecipientContact());
        helper.setSubject(notification.getSubject());
        
        // Set content
        if (notification.getHtmlContent() != null && !notification.getHtmlContent().isEmpty()) {
            String htmlContent = notification.getHtmlContent();
            
            // Add tracking pixels if enabled
            if (trackOpens) {
                htmlContent = addOpenTrackingPixel(htmlContent, notification);
            }
            
            if (trackClicks) {
                htmlContent = addClickTracking(htmlContent, notification);
            }
            
            helper.setText(notification.getContent(), htmlContent);
        } else {
            helper.setText(notification.getContent(), false);
        }
        
        // Add headers for tracking and identification
        message.setHeader("X-Notification-ID", notification.getId().toString());
        message.setHeader("X-Tenant-ID", notification.getTenantId());
        message.setHeader("X-Campaign-ID", notification.getCampaignId() != null ? notification.getCampaignId() : "");
        
        // Handle channel-specific configuration
        Map<String, Object> channelConfig = notification.getChannelConfig();
        if (channelConfig != null) {
            // Reply-to address
            if (channelConfig.containsKey("replyTo")) {
                helper.setReplyTo((String) channelConfig.get("replyTo"));
            }
            
            // CC and BCC
            if (channelConfig.containsKey("cc")) {
                Object cc = channelConfig.get("cc");
                if (cc instanceof String) {
                    helper.setCc((String) cc);
                } else if (cc instanceof String[]) {
                    helper.setCc((String[]) cc);
                }
            }
            
            if (channelConfig.containsKey("bcc")) {
                Object bcc = channelConfig.get("bcc");
                if (bcc instanceof String) {
                    helper.setBcc((String) bcc);
                } else if (bcc instanceof String[]) {
                    helper.setBcc((String[]) bcc);
                }
            }
            
            // Priority
            if (channelConfig.containsKey("priority")) {
                String priority = (String) channelConfig.get("priority");
                message.setHeader("X-Priority", priority);
            }
            
            // Custom headers
            if (channelConfig.containsKey("customHeaders")) {
                @SuppressWarnings("unchecked")
                Map<String, String> customHeaders = (Map<String, String>) channelConfig.get("customHeaders");
                for (Map.Entry<String, String> header : customHeaders.entrySet()) {
                    message.setHeader(header.getKey(), header.getValue());
                }
            }
        }
        
        return message;
    }

    private String addOpenTrackingPixel(String htmlContent, Notification notification) {
        String trackingUrl = String.format(
            "https://api.hive.com/notifications/%s/track/open", 
            notification.getId()
        );
        
        String trackingPixel = String.format(
            "<img src=\"%s\" width=\"1\" height=\"1\" style=\"display:none;\" alt=\"\" />",
            trackingUrl
        );
        
        // Insert before closing body tag if present, otherwise append
        if (htmlContent.contains("</body>")) {
            return htmlContent.replace("</body>", trackingPixel + "</body>");
        } else {
            return htmlContent + trackingPixel;
        }
    }

    private String addClickTracking(String htmlContent, Notification notification) {
        // Simple click tracking - replace links with tracking URLs
        // In production, this would be more sophisticated
        return htmlContent.replaceAll(
            "href=\"(https?://[^\"]+)\"", 
            String.format("href=\"https://api.hive.com/notifications/%s/track/click?url=$1\"", 
                         notification.getId())
        );
    }

    @Override
    public boolean canHandle(Notification notification) {
        return notification.getChannel() == NotificationChannel.EMAIL &&
               isValidEmailAddress(notification.getRecipientContact());
    }

    @Override
    public void validateNotification(Notification notification) {
        if (!canHandle(notification)) {
            throw new NotificationDeliveryException("Cannot handle notification: " + notification.getId());
        }
        
        if (notification.getSubject() == null || notification.getSubject().isEmpty()) {
            throw new NotificationDeliveryException("Email subject is required");
        }
        
        if (notification.getContent() == null || notification.getContent().isEmpty()) {
            throw new NotificationDeliveryException("Email content is required");
        }
        
        if (!isValidEmailAddress(notification.getRecipientContact())) {
            throw new NotificationDeliveryException("Invalid email address: " + notification.getRecipientContact());
        }
    }

    @Override
    public ChannelConfigRequirements getConfigRequirements() {
        List<String> required = Arrays.asList("subject", "content");
        List<String> optional = Arrays.asList("htmlContent", "replyTo", "cc", "bcc", "priority", "customHeaders");
        
        Map<String, String> descriptions = new HashMap<>();
        descriptions.put("subject", "Email subject line");
        descriptions.put("content", "Email plain text content");
        descriptions.put("htmlContent", "Email HTML content");
        descriptions.put("replyTo", "Reply-to email address");
        descriptions.put("cc", "Carbon copy recipients");
        descriptions.put("bcc", "Blind carbon copy recipients");
        descriptions.put("priority", "Email priority (high, normal, low)");
        descriptions.put("customHeaders", "Custom email headers");
        
        return new ChannelConfigRequirements(required, optional, descriptions);
    }

    @Override
    public boolean isHealthy() {
        try {
            // Simple health check - try to create a message
            MimeMessage testMessage = mailSender.createMimeMessage();
            return testMessage != null;
        } catch (Exception e) {
            logger.warn("Email provider health check failed: {}", e.getMessage());
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

    private boolean isValidEmailAddress(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
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