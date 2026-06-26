package com.meditrack.notification.health;

import com.meditrack.notification.config.NotificationProperties;
import com.meditrack.notification.repository.NotificationDeliveryLogRepository;
import com.meditrack.notification.repository.NotificationRepository;
import com.meditrack.notification.repository.NotificationTemplateRepository;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
public class NotificationServiceHealthIndicator implements HealthIndicator {

    private final NotificationRepository notificationRepository;
    private final NotificationTemplateRepository notificationTemplateRepository;
    private final NotificationDeliveryLogRepository notificationDeliveryLogRepository;
    private final NotificationProperties notificationProperties;

    public NotificationServiceHealthIndicator(
            NotificationRepository notificationRepository,
            NotificationTemplateRepository notificationTemplateRepository,
            NotificationDeliveryLogRepository notificationDeliveryLogRepository,
            NotificationProperties notificationProperties) {
        this.notificationRepository = notificationRepository;
        this.notificationTemplateRepository = notificationTemplateRepository;
        this.notificationDeliveryLogRepository = notificationDeliveryLogRepository;
        this.notificationProperties = notificationProperties;
    }

    @Override
    public Health health() {
        try {
            Map<String, Object> channelSettings = Map.of(
                    "emailSimulated", notificationProperties.getEmail().isSimulated(),
                    "smsSimulated", notificationProperties.getSms().isSimulated(),
                    "pushSimulated", notificationProperties.getPush().isSimulated(),
                    "webhookSimulated", notificationProperties.getWebhook().isSimulated()
            );

            return Health.up()
                    .withDetail("timestamp", LocalDateTime.now())
                    .withDetail("kafkaTopic", notificationProperties.getKafka().getTopic())
                    .withDetail("retryMaxAttempts", notificationProperties.getRetry().getMaxAttempts())
                    .withDetail("rateLimitPerHour", notificationProperties.getRateLimiting().getMaxPerHour())
                    .withDetail("notifications", notificationRepository.count())
                    .withDetail("templates", notificationTemplateRepository.count())
                    .withDetail("deliveryLogs", notificationDeliveryLogRepository.count())
                    .withDetail("channels", channelSettings)
                    .build();
        } catch (Exception ex) {
            return Health.down(ex)
                    .withDetail("component", "notification-service-health")
                    .build();
        }
    }
}
