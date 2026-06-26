package com.meditrack.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meditrack.notification.dto.VitalReadingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class RealTimeNotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(RealTimeNotificationService.class);
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // Connected users by role (in production, use Redis)
    private final ConcurrentMap<String, String> connectedUsers = new ConcurrentHashMap<>();
    
    /**
     * Listen for patient vitals events
     */
    @KafkaListener(topics = "patient-vitals", groupId = "notification-group")
    public void handleVitalUpdate(String vitalJson) {
        try {
            VitalReadingEvent event = objectMapper.readValue(vitalJson, VitalReadingEvent.class);
            
            logger.info("Real-time vital update: Patient={}, Type={}, Value={}", 
                event.getPatientId(), event.getVitalType(), event.getValue());
            
            // Send to department-scoped dashboards
            sendToDepartmentDashboard(event, "doctor-vitals");
            sendToDepartmentDashboard(event, "nurse-vitals");
            sendToAdminDashboard(event);
            
        } catch (Exception e) {
            logger.error("Failed to process vital update: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Listen for AI prediction events
     */
    @KafkaListener(topics = "patient-predictions", groupId = "notification-group")
    public void handlePredictionUpdate(String predictionJson) {
        try {
            VitalReadingEvent event = objectMapper.readValue(predictionJson, VitalReadingEvent.class);
            
            if ("PREDICTION_GENERATED".equals(event.getEventType())) {
                logger.info("Real-time prediction update: Patient={}, Risk={}, Confidence={}%", 
                    event.getPatientId(), event.getRiskLevel(), event.getConfidence());
                
                // Send prediction updates to department-scoped dashboards
                sendToDepartmentDashboard(event, "doctor-predictions");
                sendPredictionToAdminDashboard(event);
            }
            
        } catch (Exception e) {
            logger.error("Failed to process prediction update: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Listen for alert events
     */
    @KafkaListener(topics = "patient-alerts", groupId = "notification-group")
    public void handleAlertUpdate(String alertJson) {
        try {
            VitalReadingEvent event = objectMapper.readValue(alertJson, VitalReadingEvent.class);
            
            if ("ALERT_GENERATED".equals(event.getEventType())) {
                logger.warn("Real-time alert: Patient={}, Severity={}", 
                    event.getPatientId(), event.getSeverity());
                
                // Send alert to department-scoped dashboards
                sendToDepartmentDashboard(event, "doctor-alerts");
                sendToDepartmentDashboard(event, "nurse-alerts");
                sendAlertToAdminDashboard(event);
            }
            
        } catch (Exception e) {
            logger.error("Failed to process alert update: {}", e.getMessage(), e);
        }
    }
    
    private void sendToDepartmentDashboard(VitalReadingEvent event, String topicPrefix) {
        try {
            if (event.getDepartment() == null || event.getDepartment().isBlank()) {
                logger.warn("Skipping {} update without department for patient {}", topicPrefix, event.getPatientId());
                return;
            }
            String message = objectMapper.writeValueAsString(event);
            messagingTemplate.convertAndSend("/topic/" + topicPrefix + "/department/" + normalizeTopicSegment(event.getDepartment()), message);
            
            logger.debug("Sent {} update to department dashboard: Department={}, Patient={}",
                topicPrefix, event.getDepartment(), event.getPatientId());
            
        } catch (Exception e) {
            logger.error("Failed to send department-scoped update: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Send vital updates to admin dashboard
     */
    private void sendToAdminDashboard(VitalReadingEvent event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            messagingTemplate.convertAndSend("/topic/admin-vitals", message);
            
            logger.debug("Sent vital update to admin dashboard: Patient={}", event.getPatientId());
            
        } catch (Exception e) {
            logger.error("Failed to send to admin dashboard: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Send prediction to admin dashboard
     */
    private void sendPredictionToAdminDashboard(VitalReadingEvent event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            messagingTemplate.convertAndSend("/topic/admin-predictions", message);
            
            logger.debug("Sent prediction to admin dashboard: Patient={}, Risk={}", 
                event.getPatientId(), event.getRiskLevel());
            
        } catch (Exception e) {
            logger.error("Failed to send prediction to admin: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Send alert to admin dashboard
     */
    private void sendAlertToAdminDashboard(VitalReadingEvent event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            messagingTemplate.convertAndSend("/topic/admin-alerts", message);
            
            logger.debug("Sent alert to admin dashboard: Patient={}, Severity={}", 
                event.getPatientId(), event.getSeverity());
            
        } catch (Exception e) {
            logger.error("Failed to send alert to admin: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Register user connection
     */
    public void registerUser(String userId, String role) {
        connectedUsers.put(userId, role);
        logger.info("User connected: {} ({})", userId, role);
    }
    
    /**
     * Unregister user disconnection
     */
    public void unregisterUser(String userId) {
        String role = connectedUsers.remove(userId);
        logger.info("User disconnected: {} ({})", userId, role);
    }
    
    /**
     * Get connected users count by role
     */
    public int getConnectedUsersByRole(String role) {
        return (int) connectedUsers.values().stream()
            .filter(userRole -> userRole.equals(role))
            .count();
    }

    private String normalizeTopicSegment(String value) {
        return value.trim().toLowerCase().replaceAll("[^a-z0-9_-]", "-");
    }
}
