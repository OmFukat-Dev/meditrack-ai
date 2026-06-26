package com.meditrack.alert.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meditrack.alert.entity.Notification;
import com.meditrack.alert.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import org.junit.jupiter.api.extension.ExtendWith;
import com.meditrack.security.ServiceJwtUtil;
import org.springframework.http.client.ClientHttpRequestInterceptor;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private RestTemplateBuilder restTemplateBuilder;

    @Mock
    private ServiceJwtUtil serviceJwtUtil;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicReference<Notification> storedNotification = new AtomicReference<>();

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplateBuilder().build();
        when(restTemplateBuilder.additionalInterceptors(any(ClientHttpRequestInterceptor[].class))).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);
        notificationService = new NotificationService(
            notificationRepository,
            auditService,
            objectMapper,
            restTemplateBuilder,
            serviceJwtUtil,
            "http://notification-service"
        );
        server = MockRestServiceServer.bindTo(restTemplate).build();

        when(notificationRepository.save(any())).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            storedNotification.set(notification);
            return notification;
        });
    }

    @Test
    void sendNotificationDelegatesToNotificationServiceAndPersistsProviderReference() {
        Notification notification = baseNotification();

        server.expect(requestTo("http://notification-service/api/notifications"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess(
                """
                {
                  "requestId": "req-1",
                  "overallStatus": "COMPLETED",
                  "successCount": 1,
                  "failureCount": 0,
                  "scheduledCount": 0,
                  "message": "Notification delivered successfully",
                  "results": [
                    {
                      "notificationId": 101,
                      "channel": "EMAIL",
                      "status": "DELIVERED",
                      "provider": "SIMULATED_EMAIL",
                      "responseCode": "SIM-200",
                      "responseMessage": "Email delivered successfully",
                      "templateUsed": null
                    }
                  ]
                }
                """,
                MediaType.APPLICATION_JSON
            ));

        boolean success = notificationService.sendNotification(notification);

        assertTrue(success);
        assertEquals(Notification.NotificationStatus.DELIVERED, notification.getStatus());
        assertEquals("101", notification.getProviderReference());
        assertNotNull(notification.getSentAt());
        verify(auditService).logNotification(notification, true);
        server.verify();
    }

    @Test
    void recallNotificationDelegatesToNotificationServiceRecallEndpoint() {
        Notification notification = baseNotification();
        notification.setProviderReference("101");
        notification.setStatus(Notification.NotificationStatus.DELIVERED);
        storedNotification.set(notification);
        when(notificationRepository.findById(anyString())).thenAnswer(invocation -> Optional.ofNullable(storedNotification.get()));

        server.expect(requestTo("http://notification-service/api/notifications/101/recall"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess(
                """
                {
                  "notificationId": 101,
                  "channel": "EMAIL",
                  "status": "RECALLED",
                  "provider": "SIMULATED_EMAIL",
                  "responseCode": "SIM-RECALL",
                  "responseMessage": "Notification recalled",
                  "templateUsed": null
                }
                """,
                MediaType.APPLICATION_JSON
            ));

        boolean success = notificationService.recallNotification(notification);

        assertTrue(success);
        assertEquals(Notification.NotificationStatus.RECALLED, notification.getStatus());
        verify(auditService).logNotificationRecall(notification, true);
        server.verify();
    }

    private Notification baseNotification() {
        Notification notification = new Notification();
        notification.setId("notif-1");
        notification.setRecipient("patient-1");
        notification.setMessage("Vitals are critical");
        notification.setNotificationType(Notification.NotificationType.EMAIL);
        notification.setPriority(Notification.NotificationPriority.HIGH);
        notification.setCreatedAt(LocalDateTime.of(2026, 4, 22, 10, 0));
        return notification;
    }
}
