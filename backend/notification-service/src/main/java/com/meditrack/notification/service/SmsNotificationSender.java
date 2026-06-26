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
public class SmsNotificationSender implements NotificationChannelSender {

    private static final Logger logger = LoggerFactory.getLogger(SmsNotificationSender.class);

    private final NotificationProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();

    public SmsNotificationSender(NotificationProperties properties) {
        this.properties = properties;
    }

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.SMS;
    }

    @Override
    public ChannelDeliveryResult send(NotificationEntity notification, String subject, String body) {
        String provider = properties.getSms().getProvider();
        String message = formatSmsMessage(notification, body);

        try {
            if (properties.getSms().isSimulated() || properties.getSms().getEndpoint() == null || properties.getSms().getEndpoint().isBlank()) {
                logger.info(
                    "Simulated SMS delivery to {} using provider {}: {}",
                    notification.getRecipientId(),
                    provider,
                    message
                );
                ChannelDeliveryResult result = ChannelDeliveryResult.success(NotificationChannel.SMS, "SIMULATED_SMS", "SIM-200", "SMS simulated successfully");
                result.getMetadata().put("messageLength", message.length());
                return result;
            }

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("to", notification.getRecipientId());
            payload.put("from", properties.getSms().getFromNumber());
            payload.put("message", message);
            payload.put("notificationId", notification.getId());
            payload.put("priorityLevel", notification.getPriorityLevel());
            payload.put("sourceService", notification.getSourceService());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(properties.getSms().getEndpoint(), entity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                return ChannelDeliveryResult.success(NotificationChannel.SMS, provider, String.valueOf(response.getStatusCode().value()), "SMS accepted by provider");
            }

            return ChannelDeliveryResult.failure(
                NotificationChannel.SMS,
                provider,
                String.valueOf(response.getStatusCode().value()),
                "SMS provider returned non-success status",
                response.getBody()
            );
        } catch (Exception e) {
            logger.error("Failed to send SMS to {}", notification.getRecipientId(), e);
            return ChannelDeliveryResult.failure(NotificationChannel.SMS, provider, "500", "SMS delivery failed", e.getMessage());
        }
    }

    @Override
    public ChannelDeliveryResult recall(NotificationEntity notification) {
        String provider = properties.getSms().getProvider();
        try {
            String recallMessage = "RECALL: Previous message has been recalled. ID: " + notification.getId();
            if (properties.getSms().isSimulated() || properties.getSms().getEndpoint() == null || properties.getSms().getEndpoint().isBlank()) {
                logger.info("Simulated SMS recall to {}: {}", notification.getRecipientId(), recallMessage);
                return ChannelDeliveryResult.success(NotificationChannel.SMS, "SIMULATED_SMS", "SIM-RECALL", "SMS recall simulated");
            }

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("to", notification.getRecipientId());
            payload.put("from", properties.getSms().getFromNumber());
            payload.put("message", recallMessage);
            payload.put("notificationId", notification.getId());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<String> response = restTemplate.postForEntity(
                properties.getSms().getEndpoint(),
                new HttpEntity<>(payload, headers),
                String.class
            );
            if (response.getStatusCode().is2xxSuccessful()) {
                return ChannelDeliveryResult.success(NotificationChannel.SMS, provider, String.valueOf(response.getStatusCode().value()), "SMS recall accepted");
            }

            return ChannelDeliveryResult.failure(
                NotificationChannel.SMS,
                provider,
                String.valueOf(response.getStatusCode().value()),
                "SMS recall rejected",
                response.getBody()
            );
        } catch (Exception e) {
            logger.error("Failed to recall SMS notification {}", notification.getId(), e);
            return ChannelDeliveryResult.failure(NotificationChannel.SMS, provider, "500", "SMS recall failed", e.getMessage());
        }
    }

    private String formatSmsMessage(NotificationEntity notification, String body) {
        StringBuilder message = new StringBuilder();

        if (notification.getPriorityLevel() != null && notification.getPriorityLevel() <= 2) {
            message.append("HIGH: ");
        }

        if (body != null) {
            message.append(body);
        }

        String text = message.toString();
        if (text.length() > 160) {
            return text.substring(0, 157) + "...";
        }
        return text;
    }
}
