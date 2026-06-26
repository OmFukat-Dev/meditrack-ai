package com.meditrack.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meditrack.notification.config.NotificationProperties;
import com.meditrack.notification.domain.NotificationChannel;
import com.meditrack.notification.domain.NotificationStatus;
import com.meditrack.notification.domain.NotificationType;
import com.meditrack.notification.domain.RecipientType;
import com.meditrack.notification.dto.NotificationChannelResult;
import com.meditrack.notification.dto.NotificationRequest;
import com.meditrack.notification.dto.NotificationResponse;
import com.meditrack.notification.entity.NotificationDeliveryLogEntity;
import com.meditrack.notification.entity.NotificationEntity;
import com.meditrack.notification.repository.NotificationDeliveryLogRepository;
import com.meditrack.notification.repository.NotificationRepository;
import com.meditrack.notification.repository.NotificationTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationDispatchServiceTest {

    private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
    private final NotificationDeliveryLogRepository deliveryLogRepository = mock(NotificationDeliveryLogRepository.class);
    private final NotificationTemplateRepository templateRepository = mock(NotificationTemplateRepository.class);
    private final NotificationChannelSender emailSender = mock(NotificationChannelSender.class);
    private final NotificationProperties properties = new NotificationProperties();
    private final NotificationTemplateService templateService = new NotificationTemplateService(templateRepository, new ObjectMapper());
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final AtomicLong idSequence = new AtomicLong(100);
    private final AtomicReference<NotificationEntity> storedNotification = new AtomicReference<>();

    private NotificationDispatchService dispatchService;

    @BeforeEach
    void setUp() {
        properties.getRetry().setMaxAttempts(3);
        properties.getRetry().setDelaySeconds(30);

        when(emailSender.getChannel()).thenReturn(NotificationChannel.EMAIL);
        when(emailSender.send(any(), anyString(), anyString())).thenAnswer(invocation ->
            ChannelDeliveryResult.success(NotificationChannel.EMAIL, "SIMULATED_EMAIL", "SIM-200", "Email delivered")
        );
        when(emailSender.recall(any())).thenAnswer(invocation ->
            ChannelDeliveryResult.success(NotificationChannel.EMAIL, "SIMULATED_EMAIL", "SIM-RECALL", "Email recall accepted")
        );

        when(notificationRepository.save(any())).thenAnswer(invocation -> {
            NotificationEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(idSequence.incrementAndGet());
            }
            storedNotification.set(entity);
            return entity;
        });
        when(notificationRepository.findById(anyLong())).thenAnswer(invocation ->
            Optional.ofNullable(storedNotification.get())
        );
        when(deliveryLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(templateRepository.findByTemplateNameAndActiveTrue(anyString())).thenReturn(Optional.empty());

        dispatchService = new NotificationDispatchService(
            notificationRepository,
            deliveryLogRepository,
            templateService,
            objectMapper,
            properties,
            List.of(emailSender)
        );
    }

    @Test
    void sendNotificationDeliversAndPersistsAuditLog() {
        NotificationRequest request = new NotificationRequest();
        request.setRecipientId("patient-1");
        request.setRecipientType(RecipientType.EMAIL);
        request.setNotificationType(NotificationType.ALERT);
        request.setChannels(List.of(NotificationChannel.EMAIL));
        request.setSubject("Critical alert");
        request.setMessage("Vitals are critical");
        request.setPriorityLevel(1);
        request.setSourceService("alert-service");
        request.setSourceReference("alert-001");

        NotificationResponse response = dispatchService.sendNotification(request);

        assertEquals("COMPLETED", response.getOverallStatus());
        assertEquals(1, response.getSuccessCount());
        assertEquals(0, response.getFailureCount());
        assertEquals(0, response.getScheduledCount());
        assertEquals(1, response.getResults().size());
        assertEquals(NotificationStatus.DELIVERED, response.getResults().get(0).getStatus());
        assertNotNull(response.getResults().get(0).getNotificationId());
        assertEquals("SIMULATED_EMAIL", response.getResults().get(0).getProvider());
        verify(emailSender).send(any(), anyString(), anyString());
        verify(deliveryLogRepository).save(any(NotificationDeliveryLogEntity.class));
        assertEquals(NotificationStatus.DELIVERED, storedNotification.get().getDeliveryStatus());
        assertNotNull(storedNotification.get().getSentAt());
    }

    @Test
    void sendNotificationSchedulesFutureDeliveryWithoutSendingImmediately() {
        NotificationRequest request = new NotificationRequest();
        request.setRecipientId("patient-1");
        request.setRecipientType(RecipientType.EMAIL);
        request.setNotificationType(NotificationType.ALERT);
        request.setChannels(List.of(NotificationChannel.EMAIL));
        request.setSubject("Critical alert");
        request.setMessage("Vitals are critical");
        request.setPriorityLevel(1);
        request.setSourceService("alert-service");
        request.setSourceReference("alert-002");
        request.setScheduledAt(LocalDateTime.now().plusHours(2));

        NotificationResponse response = dispatchService.sendNotification(request);

        assertEquals("SCHEDULED", response.getOverallStatus());
        assertEquals(0, response.getSuccessCount());
        assertEquals(0, response.getFailureCount());
        assertEquals(1, response.getScheduledCount());
        assertEquals(NotificationStatus.SCHEDULED, response.getResults().get(0).getStatus());
        verify(emailSender, never()).send(any(), anyString(), anyString());
        verify(deliveryLogRepository, never()).save(any(NotificationDeliveryLogEntity.class));
        assertEquals(NotificationStatus.SCHEDULED, storedNotification.get().getDeliveryStatus());
    }

    @Test
    void recallNotificationUsesStoredEntityAndMarksRecalled() {
        NotificationEntity persisted = new NotificationEntity();
        persisted.setId(101L);
        persisted.setRecipientId("patient-1");
        persisted.setRecipientType(RecipientType.EMAIL);
        persisted.setNotificationType(NotificationType.ALERT);
        persisted.setChannelType(NotificationChannel.EMAIL);
        persisted.setSubject("Critical alert");
        persisted.setMessage("Vitals are critical");
        persisted.setPriorityLevel(1);
        persisted.setSourceService("alert-service");
        persisted.setDeliveryStatus(NotificationStatus.DELIVERED);
        storedNotification.set(persisted);
        when(notificationRepository.findById(101L)).thenReturn(Optional.of(persisted));

        NotificationChannelResult recalled = dispatchService.recallNotification(101L);

        assertEquals(NotificationStatus.RECALLED, recalled.getStatus());
        assertEquals(NotificationStatus.RECALLED, persisted.getDeliveryStatus());
        verify(emailSender).recall(any(NotificationEntity.class));
        verify(deliveryLogRepository).save(any(NotificationDeliveryLogEntity.class));
    }
}
