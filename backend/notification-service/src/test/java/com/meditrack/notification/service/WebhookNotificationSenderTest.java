package com.meditrack.notification.service;

import com.meditrack.notification.config.NotificationProperties;
import com.meditrack.notification.domain.NotificationChannel;
import com.meditrack.notification.domain.NotificationType;
import com.meditrack.notification.entity.NotificationEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebhookNotificationSenderTest {

    @Test
    void simulatedWebhookSendReturnsSuccess() {
        NotificationProperties properties = new NotificationProperties();
        properties.getWebhook().setSimulated(true);

        WebhookNotificationSender sender = new WebhookNotificationSender(properties);
        NotificationEntity notification = new NotificationEntity();
        notification.setId(1L);
        notification.setRecipientId("https://example.com/webhook");
        notification.setNotificationType(NotificationType.ALERT);
        notification.setChannelType(NotificationChannel.WEBHOOK);
        notification.setSourceService("alert-service");
        notification.setMessage("Vitals are critical");

        ChannelDeliveryResult result = sender.send(notification, "Critical alert", "Vitals are critical");

        assertTrue(result.isSuccess());
        assertEquals("SIMULATED_WEBHOOK", result.getProvider());
        assertEquals("SIM-200", result.getResponseCode());
    }

    @Test
    void simulatedWebhookRecallReturnsSuccess() {
        NotificationProperties properties = new NotificationProperties();
        properties.getWebhook().setSimulated(true);

        WebhookNotificationSender sender = new WebhookNotificationSender(properties);
        NotificationEntity notification = new NotificationEntity();
        notification.setId(1L);
        notification.setRecipientId("https://example.com/webhook");
        notification.setNotificationType(NotificationType.ALERT);
        notification.setChannelType(NotificationChannel.WEBHOOK);
        notification.setSourceService("alert-service");
        notification.setMessage("Vitals are critical");

        ChannelDeliveryResult result = sender.recall(notification);

        assertTrue(result.isSuccess());
        assertEquals("SIMULATED_WEBHOOK", result.getProvider());
        assertEquals("SIM-RECALL", result.getResponseCode());
    }
}
