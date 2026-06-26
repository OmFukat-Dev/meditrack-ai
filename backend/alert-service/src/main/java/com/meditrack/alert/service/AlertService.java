package com.meditrack.alert.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meditrack.alert.dto.AlertEvent;
import com.meditrack.alert.entity.Alert;
import com.meditrack.alert.repository.AlertRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@Transactional
public class AlertService {

    private static final Logger logger = LoggerFactory.getLogger(AlertService.class);

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    // In-memory storage for active alerts (in production, use database)
    private final ConcurrentMap<String, Alert> activeAlerts = new ConcurrentHashMap<>();
    
    private static final String NOTIFICATION_TOPIC = "patient-notifications";

    public Alert createAlert(Alert alert) {
        ensureDefaults(alert);
        Alert savedAlert = alertRepository.save(alert);
        publishAlertUpdate(savedAlert);
        return savedAlert;
    }

    /**
     * Listen for alert events from AI prediction service
     */
    @KafkaListener(topics = "patient-alerts", groupId = "alert-service-group")
    public void handleAlert(String alertJson) {
        try {
            AlertEvent event = objectMapper.readValue(alertJson, AlertEvent.class);

            if (event.getDepartment() == null || event.getDepartment().isBlank()) {
                logger.warn("Rejecting alert event without department: patientId={}", event.getPatientId());
                return;
            }
            if (event.getPatientId() == null || event.getPatientId().isBlank()) {
                logger.warn("Rejecting alert event without patientId");
                return;
            }
            
            if ("ALERT_GENERATED".equals(event.getEventType())) {
                logger.warn("ALERT RECEIVED: Patient={}, Severity={}, Message={}", 
                    event.getPatientId(), event.getSeverity(), event.getMessage());
                
                // Create alert
                Alert alert = new Alert();
                alert.setId(java.util.UUID.randomUUID().toString());
                alert.setPatientId(event.getPatientId());
                alert.setPriority(Alert.AlertPriority.valueOf(event.getSeverity()));
                alert.setMessage(event.getMessage());
                alert.setCreatedAt(LocalDateTime.now());
                alert.setStatus(Alert.AlertStatus.ACTIVE);
                alert.setVitalType(event.getVitalType());
                alert.setDepartment(event.getDepartment());
                alert.setCreatedBy(event.getCreatedBy());
                
                // Store alert
                activeAlerts.put(event.getPatientId(), alert);
                
                // Route alert based on severity
                routeAlert(alert);
                
                // Send notifications
                sendNotifications(alert);
            }
            
        } catch (Exception e) {
            logger.error("Failed to process alert: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Route alerts based on severity level
     */
    private void routeAlert(Alert alert) {
        switch (alert.getPriority()) {
            case LOW:
                logger.info("LOW ALERT: Patient={}, Message={}", 
                    alert.getPatientId(), alert.getMessage());
                break;
                
            case MEDIUM:
                logger.warn("MEDIUM ALERT: Patient={}, Message={}", 
                    alert.getPatientId(), alert.getMessage());
                notifyDepartmentCareTeam(alert);
                break;
                
            case HIGH:
                logger.error("HIGH ALERT: Patient={}, Message={}", 
                    alert.getPatientId(), alert.getMessage());
                notifyDepartmentCareTeam(alert);
                notifyAdmin(alert);
                break;
                
            case CRITICAL:
                logger.error("CRITICAL ALERT: Patient={}, Message={}", 
                    alert.getPatientId(), alert.getMessage());
                // Emergency notification to all
                notifyDepartmentCareTeam(alert);
                notifyAdmin(alert);
                notifyEmergency(alert);
                break;
        }
    }
    
    /**
     * Send notifications to relevant parties
     */
    private void sendNotifications(Alert alert) {
        try {
            String notificationJson = objectMapper.writeValueAsString(alert);
            kafkaTemplate.send(NOTIFICATION_TOPIC, alert.getPatientId(), notificationJson);
            
            logger.info("Notification sent for patient: {}, Severity: {}", 
                alert.getPatientId(), alert.getPriority());
                
        } catch (Exception e) {
            logger.error("Failed to send notification: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Notify the department care team
     */
    private void notifyDepartmentCareTeam(Alert alert) {
        try {
            logger.info("DEPARTMENT CARE TEAM NOTIFICATION: Department={}, Patient={}, Alert={}",
                alert.getDepartment(), alert.getPatientId(), alert.getMessage());
            
        } catch (Exception e) {
            logger.error("Failed to notify department care team: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Notify admin
     */
    private void notifyAdmin(Alert alert) {
        try {
            logger.warn("ADMIN NOTIFICATION: Patient={}, Alert={}", 
                alert.getPatientId(), alert.getMessage());
            
        } catch (Exception e) {
            logger.error("Failed to notify admin: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Emergency notification
     */
    private void notifyEmergency(Alert alert) {
        try {
            logger.error("EMERGENCY NOTIFICATION: Patient={}, Alert={}", 
                alert.getPatientId(), alert.getMessage());
            
            // In production, this could trigger:
            // - SMS alerts
            // - Email notifications
            // - Pager systems
            // - Emergency response teams
            
        } catch (Exception e) {
            logger.error("Failed to send emergency notification: {}", e.getMessage(), e);
        }
    }
    
    public boolean processAlert(String alertId) {
        Alert alert = getAlert(alertId);
        if (alert == null) {
            return false;
        }
        return processAlert(alert);
    }

    public boolean processAlert(Alert alert) {
        try {
            ensureDefaults(alert);
            alert.setStatus(Alert.AlertStatus.IN_PROGRESS);
            alertRepository.save(alert);

            alert.setStatus(Alert.AlertStatus.RESOLVED);
            alert.setProcessedAt(LocalDateTime.now());
            alertRepository.save(alert);
            publishAlertUpdate(alert);
            return true;
        } catch (Exception e) {
            alert.setStatus(Alert.AlertStatus.ACTIVE);
            alertRepository.save(alert);
            return false;
        }
    }

    public boolean undoAlertProcessing(Alert alert) {
        try {
            ensureDefaults(alert);
            alert.setStatus(Alert.AlertStatus.ACTIVE);
            alert.setProcessedAt(null);
            alert.setEscalatedAt(null);
            alertRepository.save(alert);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Alert getAlert(String alertId) {
        return alertRepository.findById(alertId).orElse(null);
    }

    public List<Alert> getAlerts(int limit) {
        int pageSize = Math.max(1, Math.min(limit, 100));
        return alertRepository.findAll(
            PageRequest.of(0, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();
    }

    public List<Alert> getAlertsByPatient(String patientId) {
        return alertRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
    }

    public List<Alert> getAlertsByDepartment(String department, int limit) {
        int pageSize = Math.max(1, Math.min(limit, 100));
        return alertRepository.findByDepartmentIgnoreCase(
            department,
            PageRequest.of(0, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();
    }

    private void publishAlertUpdate(Alert alert) {
        if (messagingTemplate == null) {
            logger.warn("SimpMessagingTemplate is not available; skipping alert websocket publish for alert={}", alert.getId());
            return;
        }

        if (alert.getDepartment() != null && !alert.getDepartment().isBlank()) {
            messagingTemplate.convertAndSend("/topic/alerts/department/" + normalizeTopicSegment(alert.getDepartment()), alert);
        }
        messagingTemplate.convertAndSend("/topic/admin-alerts", alert);
    }

    private String normalizeTopicSegment(String value) {
        return value.trim().toLowerCase().replaceAll("[^a-z0-9_-]", "-");
    }

    private void ensureDefaults(Alert alert) {
        if (alert.getCreatedAt() == null) {
            alert.setCreatedAt(LocalDateTime.now());
        }
        if (alert.getStatus() == null) {
            alert.setStatus(Alert.AlertStatus.ACTIVE);
        }
        if (alert.getPriority() == null) {
            alert.setPriority(Alert.AlertPriority.MEDIUM);
        }
    }
}
