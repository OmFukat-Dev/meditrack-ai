package com.meditrack.alert.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PushNotificationGatewayService {

    private static final Logger logger = LoggerFactory.getLogger(PushNotificationGatewayService.class);

    public boolean sendPushNotification(PushNotificationService.PushNotificationRequest request) {
        logger.info(
            "Simulated push gateway send to {} with title {}",
            request.getDeviceToken(),
            request.getTitle()
        );
        return true;
    }
}
