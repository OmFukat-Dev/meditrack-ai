package com.meditrack.notification.service;

import com.meditrack.notification.config.NotificationProperties;
import com.meditrack.notification.domain.NotificationChannel;
import com.meditrack.notification.domain.NotificationType;
import com.meditrack.notification.entity.NotificationEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmsNotificationSenderTest {

    @Test
    void simulatedSmsSendReturnsSuccess() {
        NotificationProperties properties = new NotificationProperties();
        properties.getSms().setSimulated(true);

        SmsNotificationSender sender = new SmsNotificationSender(properties);
        NotificationEntity notification = new NotificationEntity();
        notification.setId(1L);
        notification.setRecipientId("+15551234567");
        notification.setNotificationType(NotificationType.ALERT);
        notification.setChannelType(NotificationChannel.SMS);
        notification.setSourceService("alert-service");
        notification.setMessage("Vitals are critical");
        notification.setPriorityLevel(1);

        ChannelDeliveryResult result = sender.send(notification, "Critical alert", "Vitals are critical");

        assertTrue(result.isSuccess());
        assertEquals("SIMULATED_SMS", result.getProvider());
        assertEquals("SIM-200", result.getResponseCode());
    }

    @Test
    void simulatedSmsRecallReturnsSuccess() {
        NotificationProperties properties = new NotificationProperties();
        properties.getSms().setSimulated(true);

        SmsNotificationSender sender = new SmsNotificationSender(properties);
        NotificationEntity notification = new NotificationEntity();
        notification.setId(1L);
        notification.setRecipientId("+15551234567");
        notification.setNotificationType(NotificationType.ALERT);
        notification.setChannelType(NotificationChannel.SMS);
        notification.setSourceService("alert-service");
        notification.setMessage("Vitals are critical");
        notification.setPriorityLevel(1);

        ChannelDeliveryResult result = sender.recall(notification);

        assertTrue(result.isSuccess());
        assertEquals("SIMULATED_SMS", result.getProvider());
        assertEquals("SIM-RECALL", result.getResponseCode());
    }
}
