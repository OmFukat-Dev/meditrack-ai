package com.meditrack.alert.service;

import com.meditrack.alert.entity.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PushNotificationService implements NotificationChannelService {
    
    private static final Logger logger = LoggerFactory.getLogger(PushNotificationService.class);
    
    @Autowired
    private PushNotificationGatewayService pushGatewayService;
    
    @Override
    public boolean sendNotification(Notification notification) {
        try {
            logger.info("Sending push notification to: {}", notification.getRecipient());
            
            PushNotificationRequest request = createPushNotificationRequest(notification);
            boolean success = pushGatewayService.sendPushNotification(request);
            
            if (success) {
                logger.info("Push notification sent successfully to: {}", notification.getRecipient());
            } else {
                logger.error("Failed to send push notification to: {}", notification.getRecipient());
            }
            
            return success;
            
        } catch (Exception e) {
            logger.error("Error sending push notification to: {}", notification.getRecipient(), e);
            return false;
        }
    }
    
    @Override
    public boolean recallNotification(Notification notification) {
        try {
            logger.info("Recalling push notification: {}", notification.getId());
            
            // Send recall push notification
            PushNotificationRequest recallRequest = createRecallPushNotificationRequest(notification);
            boolean success = pushGatewayService.sendPushNotification(recallRequest);
            
            if (success) {
                logger.info("Push notification recalled successfully: {}", notification.getId());
            } else {
                logger.error("Failed to recall push notification: {}", notification.getId());
            }
            
            return success;
            
        } catch (Exception e) {
            logger.error("Error recalling push notification: {}", notification.getId(), e);
            return false;
        }
    }
    
    private PushNotificationRequest createPushNotificationRequest(Notification notification) {
        PushNotificationRequest request = new PushNotificationRequest();
        request.setDeviceToken(notification.getRecipient());
        request.setTitle(getTitle(notification));
        request.setBody(notification.getMessage());
        request.setData(createNotificationData(notification));
        request.setPriority(getPushPriority(notification));
        
        return request;
    }
    
    private PushNotificationRequest createRecallPushNotificationRequest(Notification notification) {
        PushNotificationRequest request = new PushNotificationRequest();
        request.setDeviceToken(notification.getRecipient());
        request.setTitle("Notification Recalled");
        request.setBody("Previous notification has been recalled");
        request.setData(Map.of(
            "originalNotificationId", notification.getId(),
            "recall", true
        ));
        request.setPriority(PushPriority.NORMAL);
        
        return request;
    }
    
    private String getTitle(Notification notification) {
        if (notification.getEscalationLevel() != null) {
            return "ESCALATION ALERT";
        }
        
        switch (notification.getPriority()) {
            case CRITICAL:
                return "CRITICAL ALERT";
            case HIGH:
                return "High Priority Alert";
            case MEDIUM:
                return "Alert";
            case LOW:
                return "Information";
            default:
                return "Notification";
        }
    }
    
    private PushPriority getPushPriority(Notification notification) {
        switch (notification.getPriority()) {
            case CRITICAL:
                return PushPriority.HIGH;
            case HIGH:
                return PushPriority.HIGH;
            case MEDIUM:
                return PushPriority.NORMAL;
            case LOW:
                return PushPriority.LOW;
            default:
                return PushPriority.NORMAL;
        }
    }
    
    private Map<String, Object> createNotificationData(Notification notification) {
        Map<String, Object> data = new HashMap<>();
        data.put("notificationId", notification.getId());
        data.put("priority", notification.getPriority().toString());
        data.put("createdAt", notification.getCreatedAt().toString());
        
        if (notification.getEscalationLevel() != null) {
            data.put("escalationLevel", notification.getEscalationLevel());
        }
        
        return data;
    }
    
    // Inner classes
    public static class PushNotificationRequest {
        private String deviceToken;
        private String title;
        private String body;
        private Map<String, Object> data;
        private PushPriority priority;
        
        // Getters and setters
        public String getDeviceToken() { return deviceToken; }
        public void setDeviceToken(String deviceToken) { this.deviceToken = deviceToken; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }
        
        public Map<String, Object> getData() { return data; }
        public void setData(Map<String, Object> data) { this.data = data; }
        
        public PushPriority getPriority() { return priority; }
        public void setPriority(PushPriority priority) { this.priority = priority; }
    }
    
    public enum PushPriority {
        LOW, NORMAL, HIGH
    }
}
