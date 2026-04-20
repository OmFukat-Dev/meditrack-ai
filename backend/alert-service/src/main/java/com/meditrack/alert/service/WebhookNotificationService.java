package com.meditrack.alert.service;

import com.meditrack.alert.entity.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class WebhookNotificationService implements NotificationChannelService {
    
    private static final Logger logger = LoggerFactory.getLogger(WebhookNotificationService.class);
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Override
    public boolean sendNotification(Notification notification) {
        try {
            logger.info("Sending webhook notification to: {}", notification.getRecipient());
            
            WebhookRequest request = createWebhookRequest(notification);
            ResponseEntity<String> response = restTemplate.exchange(
                notification.getRecipient(),
                HttpMethod.POST,
                new HttpEntity<>(request, createHeaders()),
                String.class
            );
            
            boolean success = response.getStatusCode().is2xxSuccessful();
            
            if (success) {
                logger.info("Webhook notification sent successfully to: {}", notification.getRecipient());
            } else {
                logger.error("Failed to send webhook notification to: {}. Status: {}", 
                           notification.getRecipient(), response.getStatusCode());
            }
            
            return success;
            
        } catch (Exception e) {
            logger.error("Error sending webhook notification to: {}", notification.getRecipient(), e);
            return false;
        }
    }
    
    @Override
    public boolean recallNotification(Notification notification) {
        try {
            logger.info("Recalling webhook notification: {}", notification.getId());
            
            WebhookRequest recallRequest = createRecallWebhookRequest(notification);
            ResponseEntity<String> response = restTemplate.exchange(
                notification.getRecipient(),
                HttpMethod.POST,
                new HttpEntity<>(recallRequest, createHeaders()),
                String.class
            );
            
            boolean success = response.getStatusCode().is2xxSuccessful();
            
            if (success) {
                logger.info("Webhook notification recalled successfully: {}", notification.getId());
            } else {
                logger.error("Failed to recall webhook notification: {}. Status: {}", 
                           notification.getId(), response.getStatusCode());
            }
            
            return success;
            
        } catch (Exception e) {
            logger.error("Error recalling webhook notification: {}", notification.getId(), e);
            return false;
        }
    }
    
    private WebhookRequest createWebhookRequest(Notification notification) {
        WebhookRequest request = new WebhookRequest();
        request.setEvent("alert.notification");
        request.setNotificationId(notification.getId());
        request.setRecipient(notification.getRecipient());
        request.setMessage(notification.getMessage());
        request.setPriority(notification.getPriority().toString());
        request.setTimestamp(notification.getCreatedAt());
        
        if (notification.getEscalationLevel() != null) {
            request.setEscalationLevel(notification.getEscalationLevel());
        }
        
        return request;
    }
    
    private WebhookRequest createRecallWebhookRequest(Notification notification) {
        WebhookRequest request = new WebhookRequest();
        request.setEvent("alert.recall");
        request.setNotificationId(notification.getId());
        request.setRecipient(notification.getRecipient());
        request.setMessage("Notification has been recalled");
        request.setTimestamp(java.time.LocalDateTime.now());
        request.setRecall(true);
        
        return request;
    }
    
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("User-Agent", "MediTrack-AlertService/1.0");
        return headers;
    }
    
    // Inner classes
    public static class WebhookRequest {
        private String event;
        private String notificationId;
        private String recipient;
        private String message;
        private String priority;
        private String escalationLevel;
        private java.time.LocalDateTime timestamp;
        private boolean recall;
        
        // Getters and setters
        public String getEvent() { return event; }
        public void setEvent(String event) { this.event = event; }
        
        public String getNotificationId() { return notificationId; }
        public void setNotificationId(String notificationId) { this.notificationId = notificationId; }
        
        public String getRecipient() { return recipient; }
        public void setRecipient(String recipient) { this.recipient = recipient; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
        
        public String getEscalationLevel() { return escalationLevel; }
        public void setEscalationLevel(String escalationLevel) { this.escalationLevel = escalationLevel; }
        
        public java.time.LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(java.time.LocalDateTime timestamp) { this.timestamp = timestamp; }
        
        public boolean isRecall() { return recall; }
        public void setRecall(boolean recall) { this.recall = recall; }
    }
}
