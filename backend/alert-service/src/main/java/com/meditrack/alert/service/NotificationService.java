package com.meditrack.alert.service;

import com.meditrack.alert.entity.Notification;
import com.meditrack.alert.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired
    private EmailNotificationService emailNotificationService;
    
    @Autowired
    private SmsNotificationService smsNotificationService;
    
    @Autowired
    private PushNotificationService pushNotificationService;
    
    @Autowired
    private WebhookNotificationService webhookNotificationService;
    
    @Autowired
    private AuditService auditService;
    
    // Notification channels
    private final Map<NotificationChannel, NotificationChannelService> channelServices = new ConcurrentHashMap<>();
    
    // Active notifications tracking
    private final Map<String, NotificationContext> activeNotifications = new ConcurrentHashMap<>();
    
    public NotificationService() {
        // Initialize channel services
        channelServices.put(NotificationChannel.EMAIL, emailNotificationService);
        channelServices.put(NotificationChannel.SMS, smsNotificationService);
        channelServices.put(NotificationChannel.PUSH, pushNotificationService);
        channelServices.put(NotificationChannel.WEBHOOK, webhookNotificationService);
    }
    
    // Send notification
    public boolean sendNotification(Notification notification) {
        try {
            logger.info("Sending notification: {} via channel: {}", 
                       notification.getId(), notification.getNotificationType());
            
            // Create notification context
            NotificationContext context = new NotificationContext(notification, LocalDateTime.now());
            activeNotifications.put(notification.getId(), context);
            
            // Get channel service
            NotificationChannelService channelService = channelServices.get(notification.getNotificationType());
            if (channelService == null) {
                logger.error("No channel service found for notification type: {}", notification.getNotificationType());
                context.setStatus(NotificationStatus.FAILED);
                return false;
            }
            
            // Send notification
            boolean success = channelService.sendNotification(notification);
            
            // Update context
            context.setStatus(success ? NotificationStatus.SENT : NotificationStatus.FAILED);
            context.setSentAt(success ? LocalDateTime.now() : null);
            
            // Log for audit
            auditService.logNotification(notification, success);
            
            // Save notification
            notification.setStatus(success ? NotificationStatus.SENT : NotificationStatus.FAILED);
            notification.setSentAt(success ? LocalDateTime.now() : null);
            notificationRepository.save(notification);
            
            logger.info("Notification {} {} via {}", 
                       notification.getId(), success ? "sent" : "failed", notification.getNotificationType());
            
            return success;
            
        } catch (Exception e) {
            logger.error("Error sending notification: {}", notification.getId(), e);
            
            // Update context
            NotificationContext context = activeNotifications.get(notification.getId());
            if (context != null) {
                context.setStatus(NotificationStatus.FAILED);
            }
            
            return false;
        }
    }
    
    // Send multi-channel notification
    public boolean sendMultiChannelNotification(Notification notification, List<NotificationChannel> channels) {
        try {
            logger.info("Sending multi-channel notification: {} via channels: {}", 
                       notification.getId(), channels);
            
            boolean allSuccess = true;
            
            for (NotificationChannel channel : channels) {
                // Create channel-specific notification
                Notification channelNotification = createChannelNotification(notification, channel);
                
                boolean success = sendNotification(channelNotification);
                if (!success) {
                    allSuccess = false;
                    logger.error("Failed to send notification via channel: {}", channel);
                }
            }
            
            return allSuccess;
            
        } catch (Exception e) {
            logger.error("Error sending multi-channel notification: {}", notification.getId(), e);
            return false;
        }
    }
    
    // Send escalation notification
    public boolean sendEscalationNotification(String recipient, String message, String escalationLevel) {
        try {
            logger.info("Sending escalation notification to: {} with level: {}", recipient, escalationLevel);
            
            // Create escalation notification
            Notification notification = new Notification();
            notification.setId("escalation-" + System.currentTimeMillis());
            notification.setRecipient(recipient);
            notification.setMessage(message);
            notification.setNotificationType(NotificationChannel.EMAIL); // Default to email for escalations
            notification.setPriority(NotificationPriority.HIGH);
            notification.setCreatedAt(LocalDateTime.now());
            notification.setEscalationLevel(escalationLevel);
            
            return sendNotification(notification);
            
        } catch (Exception e) {
            logger.error("Error sending escalation notification to: {}", recipient, e);
            return false;
        }
    }
    
    // Send escalation recall notification
    public boolean sendEscalationRecallNotification(String recipient, String message) {
        try {
            logger.info("Sending escalation recall notification to: {}", recipient);
            
            // Create recall notification
            Notification notification = new Notification();
            notification.setId("recall-" + System.currentTimeMillis());
            notification.setRecipient(recipient);
            notification.setMessage(message);
            notification.setNotificationType(NotificationChannel.EMAIL);
            notification.setPriority(NotificationPriority.MEDIUM);
            notification.setCreatedAt(LocalDateTime.now());
            notification.setRecall(true);
            
            return sendNotification(notification);
            
        } catch (Exception e) {
            logger.error("Error sending escalation recall notification to: {}", recipient, e);
            return false;
        }
    }
    
    // Recall notification
    public boolean recallNotification(Notification notification) {
        try {
            logger.info("Recalling notification: {}", notification.getId());
            
            // Get channel service
            NotificationChannelService channelService = channelServices.get(notification.getNotificationType());
            if (channelService == null) {
                logger.error("No channel service found for notification type: {}", notification.getNotificationType());
                return false;
            }
            
            // Recall notification
            boolean success = channelService.recallNotification(notification);
            
            // Update notification
            notification.setRecall(true);
            notification.setRecallAt(LocalDateTime.now());
            notificationRepository.save(notification);
            
            // Log for audit
            auditService.logNotificationRecall(notification, success);
            
            return success;
            
        } catch (Exception e) {
            logger.error("Error recalling notification: {}", notification.getId(), e);
            return false;
        }
    }
    
    // Get notification status
    public NotificationStatus getNotificationStatus(String notificationId) {
        NotificationContext context = activeNotifications.get(notificationId);
        if (context != null) {
            return context.getStatus();
        }
        
        // Check database
        Notification notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification != null) {
            return notification.getStatus();
        }
        
        return NotificationStatus.NOT_FOUND;
    }
    
    // Get active notifications
    public Map<String, NotificationContext> getActiveNotifications() {
        return new ConcurrentHashMap<>(activeNotifications);
    }
    
    // Get notification history
    public List<Notification> getNotificationHistory(int limit) {
        try {
            return notificationRepository.findTop100ByOrderByCreatedAtDesc();
        } catch (Exception e) {
            logger.error("Error getting notification history", e);
            return List.of();
        }
    }
    
    // Create channel-specific notification
    private Notification createChannelNotification(Notification original, NotificationChannel channel) {
        Notification channelNotification = new Notification();
        channelNotification.setId(original.getId() + "-" + channel.name().toLowerCase());
        channelNotification.setRecipient(original.getRecipient());
        channelNotification.setMessage(original.getMessage());
        channelNotification.setNotificationType(channel);
        channelNotification.setPriority(original.getPriority());
        channelNotification.setCreatedAt(LocalDateTime.now());
        channelNotification.setEscalationLevel(original.getEscalationLevel());
        
        return channelNotification;
    }
    
    // Inner classes
    public static class NotificationContext {
        private Notification notification;
        private LocalDateTime createdAt;
        private LocalDateTime sentAt;
        private NotificationStatus status;
        
        public NotificationContext(Notification notification, LocalDateTime createdAt) {
            this.notification = notification;
            this.createdAt = createdAt;
            this.status = NotificationStatus.PENDING;
        }
        
        // Getters and setters
        public Notification getNotification() { return notification; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getSentAt() { return sentAt; }
        public NotificationStatus getStatus() { return status; }
        
        public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
        public void setStatus(NotificationStatus status) { this.status = status; }
    }
    
    public enum NotificationChannel {
        EMAIL, SMS, PUSH, WEBHOOK
    }
    
    public enum NotificationStatus {
        PENDING, SENT, FAILED, RECALLED, NOT_FOUND
    }
    
    public enum NotificationPriority {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}
