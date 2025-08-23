package com.hive.notification.controller;

import com.hive.notification.domain.entity.Notification;
import com.hive.notification.domain.enums.NotificationChannel;
import com.hive.notification.dto.*;
import com.hive.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for notification operations
 */
@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "Notification management API")
public class NotificationController {

    private final NotificationService notificationService;

    @Autowired
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    @Operation(summary = "Send a single notification")
    public ResponseEntity<NotificationResponse> sendNotification(
            @Valid @RequestBody NotificationRequest request) {
        NotificationResponse response = notificationService.sendNotification(request);
        HttpStatus status = response.isSuccess() ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }

    @PostMapping("/bulk")
    @Operation(summary = "Send bulk notifications")
    public ResponseEntity<List<NotificationResponse>> sendBulkNotifications(
            @Valid @RequestBody BulkNotificationRequest request) {
        List<NotificationResponse> responses = notificationService.sendBulkNotifications(request);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/scheduled")
    @Operation(summary = "Schedule a notification for later delivery")
    public ResponseEntity<NotificationResponse> scheduleNotification(
            @Valid @RequestBody NotificationRequest request,
            @RequestParam @Parameter(description = "Scheduled delivery time") LocalDateTime scheduledTime) {
        NotificationResponse response = notificationService.scheduleNotification(request, scheduledTime);
        HttpStatus status = response.isSuccess() ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }

    @PostMapping("/template/{templateId}")
    @Operation(summary = "Send notification using a template")
    public ResponseEntity<NotificationResponse> sendTemplateNotification(
            @PathVariable String templateId,
            @RequestParam String tenantId,
            @RequestParam String recipientId,
            @RequestParam String recipientContact,
            @RequestParam NotificationChannel channel,
            @RequestBody Map<String, Object> variables) {
        
        NotificationResponse response = notificationService.sendTemplateNotification(
            tenantId, templateId, recipientId, recipientContact, channel, variables);
        
        HttpStatus status = response.isSuccess() ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }

    @GetMapping("/{notificationId}")
    @Operation(summary = "Get notification by ID")
    public ResponseEntity<Notification> getNotification(
            @PathVariable UUID notificationId) {
        Notification notification = notificationService.getNotification(notificationId);
        return ResponseEntity.ok(notification);
    }

    @GetMapping
    @Operation(summary = "Get notifications for a user")
    public ResponseEntity<Page<Notification>> getUserNotifications(
            @RequestParam String tenantId,
            @RequestParam String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        List<Notification> notifications = notificationService.getUserNotifications(tenantId, userId, page, size);
        Page<Notification> notificationPage = new PageImpl<>(notifications, PageRequest.of(page, size), notifications.size());
        return ResponseEntity.ok(notificationPage);
    }

    @GetMapping("/count/unread")
    @Operation(summary = "Get unread notifications count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @RequestParam String tenantId,
            @RequestParam String userId) {
        
        long count = notificationService.getUnreadNotificationsCount(tenantId, userId);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @PutMapping("/{notificationId}/read")
    @Operation(summary = "Mark notification as read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable UUID notificationId,
            @RequestParam String userId) {
        
        notificationService.markAsRead(notificationId, userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<Void> markAllAsRead(
            @RequestParam String tenantId,
            @RequestParam String userId) {
        
        notificationService.markAllAsRead(tenantId, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{notificationId}/status")
    @Operation(summary = "Get notification delivery status")
    public ResponseEntity<Map<String, Object>> getDeliveryStatus(
            @PathVariable UUID notificationId) {
        
        Map<String, Object> status = notificationService.getDeliveryStatus(notificationId);
        return ResponseEntity.ok(status);
    }

    @PutMapping("/{notificationId}/status")
    @Operation(summary = "Update notification status (for external providers)")
    public ResponseEntity<Void> updateNotificationStatus(
            @PathVariable UUID notificationId,
            @Valid @RequestBody NotificationStatusUpdate statusUpdate) {
        
        notificationService.updateNotificationStatus(notificationId, statusUpdate);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{notificationId}/retry")
    @Operation(summary = "Retry failed notification")
    public ResponseEntity<Void> retryNotification(
            @PathVariable UUID notificationId) {
        
        notificationService.retryNotification(notificationId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{notificationId}")
    @Operation(summary = "Cancel notification")
    public ResponseEntity<Void> cancelNotification(
            @PathVariable UUID notificationId,
            @RequestParam(defaultValue = "Cancelled by user") String reason) {
        
        notificationService.cancelNotification(notificationId, reason);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/campaign/{campaignId}")
    @Operation(summary = "Get notifications by campaign")
    public ResponseEntity<Page<Notification>> getCampaignNotifications(
            @PathVariable String campaignId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        List<Notification> notifications = notificationService.getCampaignNotifications(campaignId, page, size);
        Page<Notification> notificationPage = new PageImpl<>(notifications, PageRequest.of(page, size), notifications.size());
        return ResponseEntity.ok(notificationPage);
    }

    @GetMapping("/analytics")
    @Operation(summary = "Get notification analytics")
    public ResponseEntity<Map<String, Object>> getAnalytics(
            @RequestParam String tenantId,
            @RequestParam LocalDateTime from,
            @RequestParam LocalDateTime to) {
        
        Map<String, Object> analytics = notificationService.getNotificationAnalytics(tenantId, from, to);
        return ResponseEntity.ok(analytics);
    }

    @GetMapping("/optimal-channel")
    @Operation(summary = "Get optimal delivery channel for user")
    public ResponseEntity<Map<String, Object>> getOptimalChannel(
            @RequestParam String tenantId,
            @RequestParam String userId,
            @RequestParam String category) {
        
        NotificationChannel channel = notificationService.getOptimalChannel(tenantId, userId, category);
        return ResponseEntity.ok(Map.of("optimalChannel", channel));
    }

    @GetMapping("/check-allowed")
    @Operation(summary = "Check if notification is allowed by user preferences")
    public ResponseEntity<Map<String, Boolean>> checkNotificationAllowed(
            @RequestParam String tenantId,
            @RequestParam String userId,
            @RequestParam String category,
            @RequestParam NotificationChannel channel) {
        
        boolean allowed = notificationService.isNotificationAllowed(tenantId, userId, category, channel);
        return ResponseEntity.ok(Map.of("allowed", allowed));
    }

    @GetMapping("/templates")
    @Operation(summary = "Get available notification templates")
    public ResponseEntity<List<Map<String, Object>>> getNotificationTemplates(
            @RequestParam String tenantId,
            @RequestParam(required = false) NotificationChannel channel) {
        
        List<Map<String, Object>> templates = notificationService.getNotificationTemplates(tenantId, channel);
        return ResponseEntity.ok(templates);
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate notification request")
    public ResponseEntity<Map<String, Object>> validateNotification(
            @Valid @RequestBody NotificationRequest request) {
        
        try {
            notificationService.validateNotificationRequest(request);
            return ResponseEntity.ok(Map.of("valid", true, "message", "Notification request is valid"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                Map.of("valid", false, "message", e.getMessage()));
        }
    }

    // Webhook endpoints for external providers
    
    @PostMapping("/webhooks/email/delivery")
    @Operation(summary = "Email delivery status webhook")
    public ResponseEntity<Void> handleEmailDeliveryWebhook(
            @RequestBody Map<String, Object> payload) {
        
        // Process email delivery status updates
        // This would typically parse provider-specific payload and update notification status
        return ResponseEntity.ok().build();
    }

    @PostMapping("/webhooks/sms/delivery")
    @Operation(summary = "SMS delivery status webhook")
    public ResponseEntity<Void> handleSmsDeliveryWebhook(
            @RequestBody Map<String, Object> payload) {
        
        // Process SMS delivery status updates
        return ResponseEntity.ok().build();
    }

    @PostMapping("/webhooks/push/delivery")
    @Operation(summary = "Push notification delivery status webhook")
    public ResponseEntity<Void> handlePushDeliveryWebhook(
            @RequestBody Map<String, Object> payload) {
        
        // Process push notification delivery status updates
        return ResponseEntity.ok().build();
    }

    // Tracking endpoints
    
    @GetMapping("/{notificationId}/track/open")
    @Operation(summary = "Track notification open (pixel tracking)")
    public ResponseEntity<byte[]> trackOpen(
            @PathVariable UUID notificationId,
            @RequestParam(required = false) String userId) {
        
        // Update notification status to READ
        try {
            if (userId != null) {
                notificationService.markAsRead(notificationId, userId);
            } else {
                // Update status without user validation for email pixel tracking
                notificationService.updateNotificationStatus(notificationId, 
                    new NotificationStatusUpdate(
                        com.hive.notification.domain.enums.NotificationStatus.READ, 
                        notificationId.toString(), 
                        LocalDateTime.now()));
            }
        } catch (Exception e) {
            // Log error but don't fail the tracking pixel
        }
        
        // Return 1x1 transparent pixel
        byte[] pixel = {
            (byte) 0x47, (byte) 0x49, (byte) 0x46, (byte) 0x38, (byte) 0x39, (byte) 0x61,
            (byte) 0x01, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x80, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xFF, (byte) 0xFF,
            (byte) 0xFF, (byte) 0x21, (byte) 0xF9, (byte) 0x04, (byte) 0x01, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x2C, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x01, (byte) 0x00,
            (byte) 0x00, (byte) 0x02, (byte) 0x02, (byte) 0x04, (byte) 0x01, (byte) 0x00,
            (byte) 0x3B
        };
        
        return ResponseEntity.ok()
            .header("Content-Type", "image/gif")
            .header("Cache-Control", "no-cache, no-store, must-revalidate")
            .body(pixel);
    }

    @GetMapping("/{notificationId}/track/click")
    @Operation(summary = "Track notification click and redirect")
    public ResponseEntity<Void> trackClick(
            @PathVariable UUID notificationId,
            @RequestParam String url) {
        
        // Track the click
        try {
            notificationService.updateNotificationStatus(notificationId,
                new NotificationStatusUpdate(
                    com.hive.notification.domain.enums.NotificationStatus.read,
                    notificationId.toString(),
                    LocalDateTime.now()));
        } catch (Exception e) {
            // Log error but continue with redirect
        }
        
        // Redirect to original URL
        return ResponseEntity.status(HttpStatus.FOUND)
            .header("Location", url)
            .build();
    }
}