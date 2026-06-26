package com.meditrack.notification.service;

import com.meditrack.notification.config.NotificationProperties;
import com.meditrack.notification.domain.NotificationChannel;
import com.meditrack.notification.domain.NotificationType;
import com.meditrack.notification.entity.NotificationEntity;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class EmailNotificationSenderTest {

    @Test
    void simulatedEmailSendReturnsSuccess() {
        NotificationProperties properties = new NotificationProperties();
        properties.getEmail().setSimulated(true);

        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailNotificationSender sender = new EmailNotificationSender(properties, mailSender);

        NotificationEntity notification = new NotificationEntity();
        notification.setId(1L);
        notification.setRecipientId("patient@example.com");
        notification.setNotificationType(NotificationType.ALERT);
        notification.setChannelType(NotificationChannel.EMAIL);
        notification.setSourceService("alert-service");
        notification.setMessage("Vitals are critical");

        ChannelDeliveryResult result = sender.send(notification, "Critical alert", "Vitals are critical");

        assertTrue(result.isSuccess());
        assertEquals("SIMULATED_EMAIL", result.getProvider());
        assertEquals("SIM-200", result.getResponseCode());
        verifyNoInteractions(mailSender);
    }

    @Test
    void simulatedEmailRecallReturnsSuccess() {
        NotificationProperties properties = new NotificationProperties();
        properties.getEmail().setSimulated(true);

        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailNotificationSender sender = new EmailNotificationSender(properties, mailSender);

        NotificationEntity notification = new NotificationEntity();
        notification.setId(1L);
        notification.setRecipientId("patient@example.com");
        notification.setNotificationType(NotificationType.ALERT);
        notification.setChannelType(NotificationChannel.EMAIL);
        notification.setSourceService("alert-service");
        notification.setMessage("Vitals are critical");

        ChannelDeliveryResult result = sender.recall(notification);

        assertTrue(result.isSuccess());
        assertEquals("SIMULATED_EMAIL", result.getProvider());
        assertEquals("SIM-RECALL", result.getResponseCode());
        verifyNoInteractions(mailSender);
    }
}
