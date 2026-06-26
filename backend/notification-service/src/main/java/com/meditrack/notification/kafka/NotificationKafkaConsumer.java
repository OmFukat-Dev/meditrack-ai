package com.meditrack.notification.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meditrack.notification.dto.NotificationRequest;
import com.meditrack.notification.service.NotificationDispatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class NotificationKafkaConsumer {

    private static final Logger logger = LoggerFactory.getLogger(NotificationKafkaConsumer.class);

    private final ObjectMapper objectMapper;
    private final NotificationDispatchService dispatchService;

    public NotificationKafkaConsumer(ObjectMapper objectMapper, NotificationDispatchService dispatchService) {
        this.objectMapper = objectMapper;
        this.dispatchService = dispatchService;
    }

    @KafkaListener(
        topics = "${meditrack.notification.kafka.topic:notification-requests}",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeNotification(@Payload String message) {
        try {
            NotificationRequest request = objectMapper.readValue(message, NotificationRequest.class);
            dispatchService.sendNotification(request);
            logger.info("Processed notification request from Kafka");
        } catch (Exception e) {
            logger.error("Failed to process notification Kafka message: {}", e.getMessage(), e);
        }
    }
}
