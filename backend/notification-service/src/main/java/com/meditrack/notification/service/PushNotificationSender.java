package com.meditrack.notification.service;

import com.meditrack.notification.config.NotificationProperties;
import com.meditrack.notification.domain.NotificationChannel;
import com.meditrack.notification.entity.NotificationEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class PushNotificationSender implements NotificationChannelSender {

    private static final Logger logger = LoggerFactory.getLogger(PushNotificationSender.class);

    private final NotificationProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();

    public PushNotificationSender(NotificationProperties properties) {
        this.properties = properties;
    }

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.PUSH;
    }

    @Override
    public ChannelDeliveryResult send(NotificationEntity notification, String subject, String body) {
        String provider = properties.getPush().getProvider();
        String title = subject == null || subject.isBlank() ? defaultTitle(notification) : subject;

        try {
            if (properties.getPush().isSimulated() || properties.getPush().getEndpoint() == null || properties.getPush().getEndpoint().isBlank()) {
                logger.info(
                    "Simulated push delivery to {} with title '{}'",
                    notification.getRecipientId(),
                    title
                );
                ChannelDeliveryResult result = ChannelDeliveryResult.success(NotificationChannel.PUSH, "SIMULATED_PUSH", "SIM-200", "Push simulated successfully");
                result.getMetadata().put("title", title);
                return result;
            }

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("recipient", notification.getRecipientId());
            payload.put("title", title);
            payload.put("body", body);
            payload.put("notificationId", notification.getId());
            payload.put("sourceService", notification.getSourceService());
            payload.put("priorityLevel", notification.getPriorityLevel());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<String> response = restTemplate.postForEntity(
                properties.getPush().getEndpoint(),
                new HttpEntity<>(payload, headers),
                String.class
            );
            if (response.getStatusCode().is2xxSuccessful()) {
                return ChannelDeliveryResult.success(NotificationChannel.PUSH, provider, String.valueOf(response.getStatusCode().value()), "Push accepted by provider");
            }

            return ChannelDeliveryResult.failure(
                NotificationChannel.PUSH,
                provider,
                String.valueOf(response.getStatusCode().value()),
                "Push provider returned non-success status",
                response.getBody()
            );
        } catch (Exception e) {
            logger.error("Failed to send push notification to {}", notification.getRecipientId(), e);
            return ChannelDeliveryResult.failure(NotificationChannel.PUSH, provider, "500", "Push delivery failed", e.getMessage());
        }
    }

    @Override
    public ChannelDeliveryResult recall(NotificationEntity notification) {
        String provider = properties.getPush().getProvider();
        String recallBody = "Previous notification has been recalled";

        try {
            if (properties.getPush().isSimulated() || properties.getPush().getEndpoint() == null || properties.getPush().getEndpoint().isBlank()) {
                logger.info("Simulated push recall for notification {}", notification.getId());
                return ChannelDeliveryResult.success(NotificationChannel.PUSH, "SIMULATED_PUSH", "SIM-RECALL", "Push recall simulated");
            }

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("recipient", notification.getRecipientId());
            payload.put("title", "Notification Recalled");
            payload.put("body", recallBody);
            payload.put("notificationId", notification.getId());
            payload.put("recall", true);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<String> response = restTemplate.postForEntity(
                properties.getPush().getEndpoint(),
                new HttpEntity<>(payload, headers),
                String.class
            );
            if (response.getStatusCode().is2xxSuccessful()) {
                return ChannelDeliveryResult.success(NotificationChannel.PUSH, provider, String.valueOf(response.getStatusCode().value()), "Push recall accepted");
            }

            return ChannelDeliveryResult.failure(
                NotificationChannel.PUSH,
                provider,
                String.valueOf(response.getStatusCode().value()),
                "Push recall rejected",
                response.getBody()
            );
        } catch (Exception e) {
            logger.error("Failed to recall push notification {}", notification.getId(), e);
            return ChannelDeliveryResult.failure(NotificationChannel.PUSH, provider, "500", "Push recall failed", e.getMessage());
        }
    }

    private String defaultTitle(NotificationEntity notification) {
        if (notification.getNotificationType() != null) {
            return notification.getNotificationType().name() + " Notification";
        }
        return "Notification";
    }
}
