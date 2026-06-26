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
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class WebhookNotificationSender implements NotificationChannelSender {

    private static final Logger logger = LoggerFactory.getLogger(WebhookNotificationSender.class);

    private final NotificationProperties properties;
    private final RestTemplate restTemplate;

    public WebhookNotificationSender(NotificationProperties properties) {
        this.properties = properties;
        this.restTemplate = createRestTemplate(properties.getWebhook().getTimeoutSeconds());
    }

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.WEBHOOK;
    }

    @Override
    public ChannelDeliveryResult send(NotificationEntity notification, String subject, String body) {
        String provider = properties.getWebhook().getProvider();

        try {
            if (properties.getWebhook().isSimulated()) {
                logger.info(
                    "Simulated webhook delivery to {} with subject '{}'",
                    notification.getRecipientId(),
                    subject
                );
                ChannelDeliveryResult result = ChannelDeliveryResult.success(
                    NotificationChannel.WEBHOOK,
                    "SIMULATED_WEBHOOK",
                    "SIM-200",
                    "Webhook simulated successfully"
                );
                result.getMetadata().put("target", notification.getRecipientId());
                return result;
            }

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("notificationId", notification.getId());
            payload.put("recipientId", notification.getRecipientId());
            payload.put("subject", subject);
            payload.put("message", body);
            payload.put("channel", notification.getChannelType() == null ? NotificationChannel.WEBHOOK.name() : notification.getChannelType().name());
            payload.put("sourceService", notification.getSourceService());
            payload.put("sourceReference", notification.getSourceReference());
            payload.put("priorityLevel", notification.getPriorityLevel());
            payload.put("templateUsed", notification.getTemplateUsed());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (properties.getWebhook().getSignatureHeader() != null
                && !properties.getWebhook().getSignatureHeader().isBlank()
                && properties.getWebhook().getSecretKey() != null
                && !properties.getWebhook().getSecretKey().isBlank()) {
                headers.set(properties.getWebhook().getSignatureHeader(), properties.getWebhook().getSecretKey());
            }

            ResponseEntity<String> response = restTemplate.postForEntity(
                notification.getRecipientId(),
                new HttpEntity<>(payload, headers),
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                ChannelDeliveryResult result = ChannelDeliveryResult.success(
                    NotificationChannel.WEBHOOK,
                    provider,
                    String.valueOf(response.getStatusCode().value()),
                    "Webhook accepted by target"
                );
                result.getMetadata().put("target", notification.getRecipientId());
                return result;
            }

            return ChannelDeliveryResult.failure(
                NotificationChannel.WEBHOOK,
                provider,
                String.valueOf(response.getStatusCode().value()),
                "Webhook target returned non-success status",
                response.getBody()
            );
        } catch (Exception e) {
            logger.error("Failed to send webhook notification to {}", notification.getRecipientId(), e);
            return ChannelDeliveryResult.failure(
                NotificationChannel.WEBHOOK,
                provider,
                "500",
                "Webhook delivery failed",
                e.getMessage()
            );
        }
    }

    @Override
    public ChannelDeliveryResult recall(NotificationEntity notification) {
        String provider = properties.getWebhook().getProvider();

        try {
            if (properties.getWebhook().isSimulated()) {
                logger.info("Simulated webhook recall for notification {}", notification.getId());
                return ChannelDeliveryResult.success(
                    NotificationChannel.WEBHOOK,
                    "SIMULATED_WEBHOOK",
                    "SIM-RECALL",
                    "Webhook recall simulated"
                );
            }

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("event", "notification.recall");
            payload.put("notificationId", notification.getId());
            payload.put("recipientId", notification.getRecipientId());
            payload.put("message", "Notification has been recalled");
            payload.put("recall", true);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (properties.getWebhook().getSignatureHeader() != null
                && !properties.getWebhook().getSignatureHeader().isBlank()
                && properties.getWebhook().getSecretKey() != null
                && !properties.getWebhook().getSecretKey().isBlank()) {
                headers.set(properties.getWebhook().getSignatureHeader(), properties.getWebhook().getSecretKey());
            }

            ResponseEntity<String> response = restTemplate.postForEntity(
                notification.getRecipientId(),
                new HttpEntity<>(payload, headers),
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                return ChannelDeliveryResult.success(
                    NotificationChannel.WEBHOOK,
                    provider,
                    String.valueOf(response.getStatusCode().value()),
                    "Webhook recall accepted"
                );
            }

            return ChannelDeliveryResult.failure(
                NotificationChannel.WEBHOOK,
                provider,
                String.valueOf(response.getStatusCode().value()),
                "Webhook recall rejected",
                response.getBody()
            );
        } catch (Exception e) {
            logger.error("Failed to recall webhook notification {}", notification.getId(), e);
            return ChannelDeliveryResult.failure(
                NotificationChannel.WEBHOOK,
                provider,
                "500",
                "Webhook recall failed",
                e.getMessage()
            );
        }
    }

    private RestTemplate createRestTemplate(int timeoutSeconds) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int timeoutMillis = Math.max(1, timeoutSeconds) * 1000;
        factory.setConnectTimeout(timeoutMillis);
        factory.setReadTimeout(timeoutMillis);
        return new RestTemplate(factory);
    }
}
