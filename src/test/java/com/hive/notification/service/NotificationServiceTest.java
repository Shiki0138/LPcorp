package com.hive.notification.service;

import com.hive.notification.domain.entity.Notification;
import com.hive.notification.domain.enums.NotificationChannel;
import com.hive.notification.domain.enums.NotificationPriority;
import com.hive.notification.domain.enums.NotificationStatus;
import com.hive.notification.dto.NotificationRequest;
import com.hive.notification.dto.NotificationResponse;
import com.hive.notification.dto.BulkNotificationRequest;
import com.hive.notification.repository.NotificationRepository;
import com.hive.notification.service.impl.NotificationServiceImpl;
import com.hive.notification.service.channel.NotificationChannelProvider;
import com.hive.notification.service.template.TemplateService;
import com.hive.notification.service.preference.PreferenceService;
import com.hive.notification.service.delivery.DeliveryOptimizationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for NotificationService
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private com.hive.notification.repository.NotificationQueueRepository queueRepository;
    @Mock private com.hive.notification.repository.NotificationTemplateRepository templateRepository;
    @Mock private com.hive.notification.repository.NotificationPreferenceRepository preferenceRepository;
    @Mock private TemplateService templateService;
    @Mock private PreferenceService preferenceService;
    @Mock private DeliveryOptimizationService deliveryOptimizationService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private NotificationChannelProvider emailChannelProvider;
    @Mock private NotificationChannelProvider smsChannelProvider;

    private NotificationService notificationService;
    private List<NotificationChannelProvider> channelProviders;

    @BeforeEach
    void setUp() {
        channelProviders = Arrays.asList(emailChannelProvider, smsChannelProvider);
        
        when(emailChannelProvider.getSupportedChannel()).thenReturn(NotificationChannel.EMAIL);
        when(smsChannelProvider.getSupportedChannel()).thenReturn(NotificationChannel.SMS);
        
        notificationService = new NotificationServiceImpl(
            notificationRepository,
            queueRepository,
            templateRepository,
            preferenceRepository,
            channelProviders,
            templateService,
            preferenceService,
            deliveryOptimizationService,
            eventPublisher
        );
    }

    @Test
    void testSendNotification_Success() {
        // Given
        NotificationRequest request = createValidNotificationRequest();
        Notification savedNotification = createNotificationFromRequest(request);
        
        when(preferenceService.isNotificationAllowed(anyString(), anyString(), anyString(), any()))
            .thenReturn(true);
        when(notificationRepository.save(any(Notification.class)))
            .thenReturn(savedNotification);
        
        // When
        NotificationResponse response = notificationService.sendNotification(request);
        
        // Then
        assertTrue(response.isSuccess());
        assertEquals(savedNotification.getId(), response.getNotificationId());
        verify(notificationRepository).save(any(Notification.class));
        verify(queueRepository).save(any());
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void testSendNotification_BlockedByPreferences() {
        // Given
        NotificationRequest request = createValidNotificationRequest();
        
        when(preferenceService.isNotificationAllowed(anyString(), anyString(), anyString(), any()))
            .thenReturn(false);
        
        // When
        NotificationResponse response = notificationService.sendNotification(request);
        
        // Then
        assertFalse(response.isSuccess());
        assertEquals("Notification blocked by user preferences", response.getMessage());
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void testSendBulkNotifications_Success() {
        // Given
        BulkNotificationRequest request = createValidBulkNotificationRequest();
        
        when(preferenceService.isNotificationAllowed(anyString(), anyString(), anyString(), any()))
            .thenReturn(true);
        when(notificationRepository.save(any(Notification.class)))
            .thenAnswer(invocation -> {
                Notification notification = invocation.getArgument(0);
                notification.setId(UUID.randomUUID());
                return notification;
            });
        
        // When
        List<NotificationResponse> responses = notificationService.sendBulkNotifications(request);
        
        // Then
        assertEquals(2, responses.size());
        assertTrue(responses.stream().allMatch(NotificationResponse::isSuccess));
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    void testScheduleNotification_Success() {
        // Given
        NotificationRequest request = createValidNotificationRequest();
        LocalDateTime scheduledTime = LocalDateTime.now().plusHours(2);
        Notification savedNotification = createNotificationFromRequest(request);
        savedNotification.setScheduledAt(scheduledTime);
        
        when(preferenceService.isNotificationAllowed(anyString(), anyString(), anyString(), any()))
            .thenReturn(true);
        when(notificationRepository.save(any(Notification.class)))
            .thenReturn(savedNotification);
        
        // When
        NotificationResponse response = notificationService.scheduleNotification(request, scheduledTime);
        
        // Then
        assertTrue(response.isSuccess());
        assertEquals(scheduledTime, response.getScheduledAt());
    }

    @Test
    void testSendTemplateNotification_Success() {
        // Given
        String tenantId = "tenant1";
        String templateId = "welcome-email";
        String recipientId = "user1";
        String recipientContact = "user@example.com";
        NotificationChannel channel = NotificationChannel.EMAIL;
        Map<String, Object> variables = Map.of("name", "John", "company", "Hive");
        
        com.hive.notification.domain.entity.NotificationTemplate template = createMockTemplate();
        TemplateService.RenderedTemplate renderedTemplate = new TemplateService.RenderedTemplate(
            "Welcome John!", "Welcome to Hive, John!", "<h1>Welcome to Hive, John!</h1>"
        );
        
        when(templateService.getLatestTemplate(tenantId, templateId)).thenReturn(template);
        when(templateService.renderTemplate(template.getId(), variables)).thenReturn(renderedTemplate);
        when(preferenceService.isNotificationAllowed(anyString(), anyString(), anyString(), any()))
            .thenReturn(true);
        when(notificationRepository.save(any(Notification.class)))
            .thenAnswer(invocation -> {
                Notification notification = invocation.getArgument(0);
                notification.setId(UUID.randomUUID());
                return notification;
            });
        
        // When
        NotificationResponse response = notificationService.sendTemplateNotification(
            tenantId, templateId, recipientId, recipientContact, channel, variables);
        
        // Then
        assertTrue(response.isSuccess());
        verify(templateService).getLatestTemplate(tenantId, templateId);
        verify(templateService).renderTemplate(template.getId(), variables);
    }

    @Test
    void testGetNotification_Success() {
        // Given
        UUID notificationId = UUID.randomUUID();
        Notification notification = createMockNotification();
        notification.setId(notificationId);
        
        when(notificationRepository.findById(notificationId))
            .thenReturn(Optional.of(notification));
        
        // When
        Notification result = notificationService.getNotification(notificationId);
        
        // Then
        assertNotNull(result);
        assertEquals(notificationId, result.getId());
    }

    @Test
    void testGetNotification_NotFound() {
        // Given
        UUID notificationId = UUID.randomUUID();
        
        when(notificationRepository.findById(notificationId))
            .thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(com.hive.notification.exception.NotificationNotFoundException.class, () -> {
            notificationService.getNotification(notificationId);
        });
    }

    @Test
    void testMarkAsRead_Success() {
        // Given
        UUID notificationId = UUID.randomUUID();
        String userId = "user1";
        Notification notification = createMockNotification();
        notification.setId(notificationId);
        notification.setRecipientId(userId);
        
        when(notificationRepository.findById(notificationId))
            .thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class)))
            .thenReturn(notification);
        
        // When
        notificationService.markAsRead(notificationId, userId);
        
        // Then
        verify(notificationRepository).save(notification);
        verify(eventPublisher).publishEvent(any());
        assertEquals(NotificationStatus.READ, notification.getStatus());
        assertNotNull(notification.getReadAt());
    }

    @Test
    void testMarkAsRead_UnauthorizedUser() {
        // Given
        UUID notificationId = UUID.randomUUID();
        String userId = "user1";
        String wrongUserId = "user2";
        Notification notification = createMockNotification();
        notification.setId(notificationId);
        notification.setRecipientId(userId);
        
        when(notificationRepository.findById(notificationId))
            .thenReturn(Optional.of(notification));
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            notificationService.markAsRead(notificationId, wrongUserId);
        });
    }

    @Test
    void testCancelNotification_Success() {
        // Given
        UUID notificationId = UUID.randomUUID();
        String reason = "User cancelled";
        Notification notification = createMockNotification();
        notification.setId(notificationId);
        notification.setStatus(NotificationStatus.QUEUED);
        
        when(notificationRepository.findById(notificationId))
            .thenReturn(Optional.of(notification));
        when(queueRepository.findByNotificationId(notificationId))
            .thenReturn(new ArrayList<>());
        
        // When
        notificationService.cancelNotification(notificationId, reason);
        
        // Then
        assertEquals(NotificationStatus.CANCELLED, notification.getStatus());
        assertEquals(reason, notification.getErrorMessage());
        verify(notificationRepository).save(notification);
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void testGetUnreadNotificationsCount_Success() {
        // Given
        String tenantId = "tenant1";
        String userId = "user1";
        long expectedCount = 5;
        
        when(notificationRepository.countByTenantIdAndRecipientIdAndReadAtIsNull(tenantId, userId))
            .thenReturn(expectedCount);
        
        // When
        long actualCount = notificationService.getUnreadNotificationsCount(tenantId, userId);
        
        // Then
        assertEquals(expectedCount, actualCount);
    }

    @Test
    void testGetOptimalChannel_Success() {
        // Given
        String tenantId = "tenant1";
        String userId = "user1";
        String category = "marketing";
        NotificationChannel expectedChannel = NotificationChannel.EMAIL;
        
        when(deliveryOptimizationService.getOptimalChannel(tenantId, userId, category))
            .thenReturn(expectedChannel);
        
        // When
        NotificationChannel actualChannel = notificationService.getOptimalChannel(tenantId, userId, category);
        
        // Then
        assertEquals(expectedChannel, actualChannel);
    }

    @Test
    void testValidateNotificationRequest_Valid() {
        // Given
        NotificationRequest request = createValidNotificationRequest();
        
        // When & Then
        assertDoesNotThrow(() -> {
            notificationService.validateNotificationRequest(request);
        });
    }

    @Test
    void testValidateNotificationRequest_InvalidTenantId() {
        // Given
        NotificationRequest request = createValidNotificationRequest();
        request.setTenantId(null);
        
        // When & Then
        assertThrows(com.hive.notification.exception.NotificationValidationException.class, () -> {
            notificationService.validateNotificationRequest(request);
        });
    }

    @Test
    void testValidateNotificationRequest_InvalidChannel() {
        // Given
        NotificationRequest request = createValidNotificationRequest();
        request.setChannel(null);
        
        // When & Then
        assertThrows(com.hive.notification.exception.NotificationValidationException.class, () -> {
            notificationService.validateNotificationRequest(request);
        });
    }

    // Helper methods for creating test data
    
    private NotificationRequest createValidNotificationRequest() {
        NotificationRequest request = new NotificationRequest();
        request.setTenantId("tenant1");
        request.setRecipientId("user1");
        request.setRecipientContact("user@example.com");
        request.setChannel(NotificationChannel.EMAIL);
        request.setPriority(NotificationPriority.NORMAL);
        request.setSubject("Test Subject");
        request.setContent("Test Content");
        request.setCategory("test");
        return request;
    }

    private BulkNotificationRequest createValidBulkNotificationRequest() {
        BulkNotificationRequest request = new BulkNotificationRequest();
        request.setTemplate(createValidNotificationRequest());
        
        List<BulkNotificationRequest.NotificationRecipient> recipients = Arrays.asList(
            new BulkNotificationRequest.NotificationRecipient("user1", "user1@example.com"),
            new BulkNotificationRequest.NotificationRecipient("user2", "user2@example.com")
        );
        request.setRecipients(recipients);
        request.setBatchSize(10);
        
        return request;
    }

    private Notification createNotificationFromRequest(NotificationRequest request) {
        Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setTenantId(request.getTenantId());
        notification.setRecipientId(request.getRecipientId());
        notification.setRecipientContact(request.getRecipientContact());
        notification.setChannel(request.getChannel());
        notification.setSubject(request.getSubject());
        notification.setContent(request.getContent());
        notification.setStatus(NotificationStatus.QUEUED);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setUpdatedAt(LocalDateTime.now());
        return notification;
    }

    private Notification createMockNotification() {
        Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setTenantId("tenant1");
        notification.setRecipientId("user1");
        notification.setRecipientContact("user@example.com");
        notification.setChannel(NotificationChannel.EMAIL);
        notification.setSubject("Test Subject");
        notification.setContent("Test Content");
        notification.setStatus(NotificationStatus.QUEUED);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setUpdatedAt(LocalDateTime.now());
        return notification;
    }

    private com.hive.notification.domain.entity.NotificationTemplate createMockTemplate() {
        com.hive.notification.domain.entity.NotificationTemplate template = 
            new com.hive.notification.domain.entity.NotificationTemplate();
        template.setId(UUID.randomUUID());
        template.setTenantId("tenant1");
        template.setName("welcome-email");
        template.setVersion("1.0");
        template.setChannel(NotificationChannel.EMAIL);
        template.setSubjectTemplate("Welcome ${name}!");
        template.setContentTemplate("Welcome to ${company}, ${name}!");
        template.setHtmlTemplate("<h1>Welcome to ${company}, ${name}!</h1>");
        template.setStatus(com.hive.notification.domain.entity.NotificationTemplate.TemplateStatus.APPROVED);
        return template;
    }
}