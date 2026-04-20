package com.meditrack.alert.service;

import com.meditrack.alert.entity.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SmsNotificationService implements NotificationChannelService {
    
    private static final Logger logger = LoggerFactory.getLogger(SmsNotificationService.class);
    
    @Autowired
    private SmsGatewayService smsGatewayService;
    
    @Override
    public boolean sendNotification(Notification notification) {
        try {
            logger.info("Sending SMS notification to: {}", notification.getRecipient());
            
            String smsMessage = formatSmsMessage(notification);
            boolean success = smsGatewayService.sendSms(notification.getRecipient(), smsMessage);
            
            if (success) {
                logger.info("SMS notification sent successfully to: {}", notification.getRecipient());
            } else {
                logger.error("Failed to send SMS notification to: {}", notification.getRecipient());
            }
            
            return success;
            
        } catch (Exception e) {
            logger.error("Error sending SMS notification to: {}", notification.getRecipient(), e);
            return false;
        }
    }
    
    @Override
    public boolean recallNotification(Notification notification) {
        try {
            logger.info("Recalling SMS notification: {}", notification.getId());
            
            // SMS cannot be recalled, but we can send a follow-up
            String recallMessage = "RECALL: Previous message has been recalled. ID: " + notification.getId();
            boolean success = smsGatewayService.sendSms(notification.getRecipient(), recallMessage);
            
            if (success) {
                logger.info("SMS recall notification sent to: {}", notification.getRecipient());
            } else {
                logger.error("Failed to send SMS recall notification to: {}", notification.getRecipient());
            }
            
            return success;
            
        } catch (Exception e) {
            logger.error("Error recalling SMS notification: {}", notification.getId(), e);
            return false;
        }
    }
    
    private String formatSmsMessage(Notification notification) {
        StringBuilder message = new StringBuilder();
        
        // Add priority indicator
        if (notification.getPriority() == NotificationService.NotificationPriority.CRITICAL) {
            message.append("CRITICAL: ");
        } else if (notification.getPriority() == NotificationService.NotificationPriority.HIGH) {
            message.append("HIGH: ");
        }
        
        // Add escalation level if present
        if (notification.getEscalationLevel() != null) {
            message.append("ESCALATED (").append(notification.getEscalationLevel()).append("): ");
        }
        
        // Add main message (truncated for SMS)
        String mainMessage = notification.getMessage();
        if (mainMessage.length() > 140) {
            mainMessage = mainMessage.substring(0, 137) + "...";
        }
        message.append(mainMessage);
        
        return message.toString();
    }
}
