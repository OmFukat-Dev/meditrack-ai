package com.meditrack.notification.service;

import com.meditrack.notification.config.NotificationProperties;
import com.meditrack.notification.domain.NotificationChannel;
import com.meditrack.notification.entity.NotificationEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationSender implements NotificationChannelSender {

    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationSender.class);

    private final NotificationProperties properties;
    private final JavaMailSender mailSender;

    public EmailNotificationSender(NotificationProperties properties, JavaMailSender mailSender) {
        this.properties = properties;
        this.mailSender = mailSender;
    }

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public ChannelDeliveryResult send(NotificationEntity notification, String subject, String body) {
        String provider = "SMTP";
        try {
            if (properties.getEmail().isSimulated()) {
                logger.info(
                    "Simulated email delivery to {} with subject '{}'",
                    notification.getRecipientId(),
                    subject
                );
                return ChannelDeliveryResult.success(NotificationChannel.EMAIL, "SIMULATED_EMAIL", "SIM-200", "Email simulated successfully");
            }

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(notification.getRecipientId());
            if (properties.getEmail().getDefaultFrom() != null) {
                message.setFrom(properties.getEmail().getDefaultFrom());
            }
            if (properties.getEmail().getDefaultReplyTo() != null) {
                message.setReplyTo(properties.getEmail().getDefaultReplyTo());
            }
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);

            return ChannelDeliveryResult.success(NotificationChannel.EMAIL, provider, "250", "Email accepted by mail server");
        } catch (Exception e) {
            logger.error("Failed to send email to {}", notification.getRecipientId(), e);
            return ChannelDeliveryResult.failure(NotificationChannel.EMAIL, provider, "500", "Email delivery failed", e.getMessage());
        }
    }

    @Override
    public ChannelDeliveryResult recall(NotificationEntity notification) {
        try {
            if (properties.getEmail().isSimulated()) {
                logger.info("Simulated email recall for notification {}", notification.getId());
                return ChannelDeliveryResult.success(NotificationChannel.EMAIL, "SIMULATED_EMAIL", "SIM-RECALL", "Email recall simulated");
            }

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(notification.getRecipientId());
            if (properties.getEmail().getDefaultFrom() != null) {
                message.setFrom(properties.getEmail().getDefaultFrom());
            }
            message.setSubject("Notification Recalled");
            message.setText("Previous notification has been recalled.\n\n" + notification.getMessage());
            mailSender.send(message);
            return ChannelDeliveryResult.success(NotificationChannel.EMAIL, "SMTP", "250", "Recall message sent");
        } catch (Exception e) {
            logger.error("Failed to recall email notification {}", notification.getId(), e);
            return ChannelDeliveryResult.failure(NotificationChannel.EMAIL, "SMTP", "500", "Recall failed", e.getMessage());
        }
    }
}
