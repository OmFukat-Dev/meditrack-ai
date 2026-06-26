package com.meditrack.alert.health;

import com.meditrack.alert.repository.AlertRepository;
import com.meditrack.alert.repository.AuditLogRepository;
import com.meditrack.alert.repository.EscalationRuleRepository;
import com.meditrack.alert.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.LocalDateTime;

@Component
public class AlertServiceHealthIndicator implements HealthIndicator {

    private final AlertRepository alertRepository;
    private final AuditLogRepository auditLogRepository;
    private final EscalationRuleRepository escalationRuleRepository;
    private final NotificationRepository notificationRepository;
    private final String notificationServiceBaseUrl;

    public AlertServiceHealthIndicator(
            AlertRepository alertRepository,
            AuditLogRepository auditLogRepository,
            EscalationRuleRepository escalationRuleRepository,
            NotificationRepository notificationRepository,
            @Value("${meditrack.alert.notification.service-base-url:http://localhost:8086}") String notificationServiceBaseUrl) {
        this.alertRepository = alertRepository;
        this.auditLogRepository = auditLogRepository;
        this.escalationRuleRepository = escalationRuleRepository;
        this.notificationRepository = notificationRepository;
        this.notificationServiceBaseUrl = notificationServiceBaseUrl;
    }

    @Override
    public Health health() {
        try {
            URI notificationServiceUri = URI.create(notificationServiceBaseUrl);

            return Health.up()
                    .withDetail("timestamp", LocalDateTime.now())
                    .withDetail("notificationServiceBaseUrl", notificationServiceUri.toString())
                    .withDetail("alerts", alertRepository.count())
                    .withDetail("notifications", notificationRepository.count())
                    .withDetail("escalationRules", escalationRuleRepository.count())
                    .withDetail("auditLogs", auditLogRepository.count())
                    .build();
        } catch (Exception ex) {
            return Health.down(ex)
                    .withDetail("component", "alert-service-health")
                    .withDetail("notificationServiceBaseUrl", notificationServiceBaseUrl)
                    .build();
        }
    }
}
