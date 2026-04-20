package com.meditrack.alert.service;

import com.meditrack.alert.entity.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService implements NotificationChannelService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationService.class);
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Override
    public boolean sendNotification(Notification notification) {
        try {
            logger.info("Sending email notification to: {}", notification.getRecipient());
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(notification.getRecipient());
            message.setSubject(getSubject(notification));
            message.setText(formatEmailMessage(notification));
            
            mailSender.send(message);
            
            logger.info("Email notification sent successfully to: {}", notification.getRecipient());
            return true;
            
        } catch (Exception e) {
            logger.error("Error sending email notification to: {}", notification.getRecipient(), e);
            return false;
        }
    }
    
    @Override
    public boolean recallNotification(Notification notification) {
        try {
            logger.info("Recalling email notification: {}", notification.getId());
            
            // Send recall email
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(notification.getRecipient());
            message.setSubject("NOTIFICATION RECALLED");
            message.setText("This notification has been recalled:\n\n" + notification.getMessage());
            
            mailSender.send(message);
            
            logger.info("Email notification recalled successfully: {}", notification.getId());
            return true;
            
        } catch (Exception e) {
            logger.error("Error recalling email notification: {}", notification.getId(), e);
            return false;
        }
    }
    
    private String getSubject(Notification notification) {
        if (notification.getEscalationLevel() != null) {
            return "ESCALATION ALERT - " + notification.getEscalationLevel().toUpperCase();
        }
        
        switch (notification.getPriority()) {
            case CRITICAL:
                return "CRITICAL ALERT";
            case HIGH:
                return "HIGH PRIORITY ALERT";
            case MEDIUM:
                return "Alert Notification";
            case LOW:
                return "Information";
            default:
                return "Notification";
        }
    }
    
    private String formatEmailMessage(Notification notification) {
        StringBuilder message = new StringBuilder();
        
        message.append(notification.getMessage());
        
        if (notification.getEscalationLevel() != null) {
            message.append("\n\n--- ESCALATION LEVEL: ").append(notification.getEscalationLevel()).append(" ---");
        }
        
        message.append("\n\n--- Notification Details ---");
        message.append("\nID: ").append(notification.getId());
        message.append("\nPriority: ").append(notification.getPriority());
        message.append("\nCreated: ").append(notification.getCreatedAt());
        
        return message.toString();
    }
}
