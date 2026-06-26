package com.meditrack.notification.service;

import com.meditrack.notification.config.NotificationProperties;
import com.meditrack.notification.domain.NotificationChannel;
import com.meditrack.notification.domain.NotificationType;
import com.meditrack.notification.entity.NotificationEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PushNotificationSenderTest {

    @Test
    void simulatedPushSendReturnsSuccess() {
        NotificationProperties properties = new NotificationProperties();
        properties.getPush().setSimulated(true);

        PushNotificationSender sender = new PushNotificationSender(properties);
        NotificationEntity notification = new NotificationEntity();
        notification.setId(1L);
        notification.setRecipientId("user-123");
        notification.setNotificationType(NotificationType.ALERT);
        notification.setChannelType(NotificationChannel.PUSH);
        notification.setSourceService("alert-service");
        notification.setMessage("Vitals are critical");
        notification.setPriorityLevel(1);

        ChannelDeliveryResult result = sender.send(notification, "Critical alert", "Vitals are critical");

        assertTrue(result.isSuccess());
        assertEquals("SIMULATED_PUSH", result.getProvider());
        assertEquals("SIM-200", result.getResponseCode());
    }

    @Test
    void simulatedPushRecallReturnsSuccess() {
        NotificationProperties properties = new NotificationProperties();
        properties.getPush().setSimulated(true);

        PushNotificationSender sender = new PushNotificationSender(properties);
        NotificationEntity notification = new NotificationEntity();
        notification.setId(1L);
        notification.setRecipientId("user-123");
        notification.setNotificationType(NotificationType.ALERT);
        notification.setChannelType(NotificationChannel.PUSH);
        notification.setSourceService("alert-service");
        notification.setMessage("Vitals are critical");
        notification.setPriorityLevel(1);

        ChannelDeliveryResult result = sender.recall(notification);

        assertTrue(result.isSuccess());
        assertEquals("SIMULATED_PUSH", result.getProvider());
        assertEquals("SIM-RECALL", result.getResponseCode());
    }
}
