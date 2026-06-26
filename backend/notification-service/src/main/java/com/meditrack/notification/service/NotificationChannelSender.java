package com.meditrack.notification.service;

import com.meditrack.notification.domain.NotificationChannel;
import com.meditrack.notification.entity.NotificationEntity;

public interface NotificationChannelSender {

    NotificationChannel getChannel();

    ChannelDeliveryResult send(NotificationEntity notification, String subject, String body);

    ChannelDeliveryResult recall(NotificationEntity notification);
}
