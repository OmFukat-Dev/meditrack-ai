package com.meditrack.alert.service;

import com.meditrack.alert.entity.Alert;
import com.meditrack.alert.entity.EscalationRule;
import com.meditrack.alert.entity.Notification;
import com.meditrack.alert.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuditService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
    
    @Autowired
    private AuditLogRepository auditLogRepository;
    
    // Audit event tracking
    private final Map<String, AuditEvent> activeEvents = new ConcurrentHashMap<>();
    
    // Log alert creation
    public void logAlertCreation(Alert alert) {
        try {
            logger.info("Logging alert creation: {}", alert.getId());
            
            AuditLog auditLog = new AuditLog();
            auditLog.setId("audit-" + System.currentTimeMillis());
            auditLog.setEventType(AuditEventType.ALERT_CREATED);
            auditLog.setEntityId(alert.getId());
            auditLog.setEntityType("Alert");
            auditLog.setUserId(alert.getCreatedBy());
            auditLog.setTimestamp(LocalDateTime.now());
            auditLog.setDescription("Alert created: " + alert.getAlertType() + " - " + alert.getMessage());
            auditLog.setSeverity(alert.getPriority().toString());
            
            // Add details
            Map<String, Object> details = new ConcurrentHashMap<>();
            details.put("alertType", alert.getAlertType());
            details.put("priority", alert.getPriority());
            details.put("patientId", alert.getPatientId());
            details.put("vitalType", alert.getVitalType());
            details.put("vitalValue", alert.getVitalValue());
            auditLog.setDetails(details);
            
            auditLogRepository.save(auditLog);
            
        } catch (Exception e) {
            logger.error("Error logging alert creation: {}", alert.getId(), e);
        }
    }
    
    // Log alert processing
    public void logAlertProcessing(Alert alert, boolean success) {
        try {
            logger.info("Logging alert processing: {} - {}", alert.getId(), success);
            
            AuditLog auditLog = new AuditLog();
            auditLog.setId("audit-" + System.currentTimeMillis());
            auditLog.setEventType(success ? AuditEventType.ALERT_PROCESSED : AuditEventType.ALERT_PROCESSING_FAILED);
            auditLog.setEntityId(alert.getId());
            auditLog.setEntityType("Alert");
            auditLog.setUserId("system");
            auditLog.setTimestamp(LocalDateTime.now());
            auditLog.setDescription("Alert " + (success ? "processed successfully" : "processing failed"));
            auditLog.setSeverity(alert.getPriority().toString());
            
            // Add details
            Map<String, Object> details = new ConcurrentHashMap<>();
            details.put("alertType", alert.getAlertType());
            details.put("priority", alert.getPriority());
            details.put("success", success);
            details.put("processedAt", LocalDateTime.now());
            auditLog.setDetails(details);
            
            auditLogRepository.save(auditLog);
            
        } catch (Exception e) {
            logger.error("Error logging alert processing: {}", alert.getId(), e);
        }
    }
    
    // Log notification
    public void logNotification(Notification notification, boolean success) {
        try {
            logger.info("Logging notification: {} - {}", notification.getId(), success);
            
            AuditLog auditLog = new AuditLog();
            auditLog.setId("audit-" + System.currentTimeMillis());
            auditLog.setEventType(success ? AuditEventType.NOTIFICATION_SENT : AuditEventType.NOTIFICATION_FAILED);
            auditLog.setEntityId(notification.getId());
            auditLog.setEntityType("Notification");
            auditLog.setUserId("system");
            auditLog.setTimestamp(LocalDateTime.now());
            auditLog.setDescription("Notification " + (success ? "sent successfully" : "failed to send"));
            auditLog.setSeverity(notification.getPriority().toString());
            
            // Add details
            Map<String, Object> details = new ConcurrentHashMap<>();
            details.put("notificationType", notification.getNotificationType());
            details.put("recipient", notification.getRecipient());
            details.put("priority", notification.getPriority());
            details.put("success", success);
            if (notification.getEscalationLevel() != null) {
                details.put("escalationLevel", notification.getEscalationLevel());
            }
            auditLog.setDetails(details);
            
            auditLogRepository.save(auditLog);
            
        } catch (Exception e) {
            logger.error("Error logging notification: {}", notification.getId(), e);
        }
    }
    
    // Log notification recall
    public void logNotificationRecall(Notification notification, boolean success) {
        try {
            logger.info("Logging notification recall: {} - {}", notification.getId(), success);
            
            AuditLog auditLog = new AuditLog();
            auditLog.setId("audit-" + System.currentTimeMillis());
            auditLog.setEventType(success ? AuditEventType.NOTIFICATION_RECALLED : AuditEventType.NOTIFICATION_RECALL_FAILED);
            auditLog.setEntityId(notification.getId());
            auditLog.setEntityType("Notification");
            auditLog.setUserId("system");
            auditLog.setTimestamp(LocalDateTime.now());
            auditLog.setDescription("Notification " + (success ? "recalled successfully" : "recall failed"));
            auditLog.setSeverity(notification.getPriority().toString());
            
            // Add details
            Map<String, Object> details = new ConcurrentHashMap<>();
            details.put("notificationType", notification.getNotificationType());
            details.put("recipient", notification.getRecipient());
            details.put("success", success);
            details.put("recalledAt", LocalDateTime.now());
            auditLog.setDetails(details);
            
            auditLogRepository.save(auditLog);
            
        } catch (Exception e) {
            logger.error("Error logging notification recall: {}", notification.getId(), e);
        }
    }
    
    // Log escalation
    public void logEscalation(EscalationRule rule, boolean success) {
        try {
            logger.info("Logging escalation: {} - {}", rule.getId(), success);
            
            AuditLog auditLog = new AuditLog();
            auditLog.setId("audit-" + System.currentTimeMillis());
            auditLog.setEventType(success ? AuditEventType.ESCALATION_EXECUTED : AuditEventType.ESCALATION_FAILED);
            auditLog.setEntityId(rule.getId());
            auditLog.setEntityType("EscalationRule");
            auditLog.setUserId("system");
            auditLog.setTimestamp(LocalDateTime.now());
            auditLog.setDescription("Escalation " + (success ? "executed successfully" : "failed to execute"));
            auditLog.setSeverity("HIGH");
            
            // Add details
            Map<String, Object> details = new ConcurrentHashMap<>();
            details.put("escalationLevel", rule.getEscalationLevel());
            details.put("targetRole", rule.getTargetRole());
            details.put("recipients", rule.getEscalationRecipients());
            details.put("success", success);
            details.put("executedAt", LocalDateTime.now());
            auditLog.setDetails(details);
            
            auditLogRepository.save(auditLog);
            
        } catch (Exception e) {
            logger.error("Error logging escalation: {}", rule.getId(), e);
        }
    }
    
    // Log escalation with alert context
    public void logEscalation(EscalationRule rule, Alert alert, boolean success) {
        try {
            logger.info("Logging escalation: {} for alert: {} - {}", rule.getId(), alert.getId(), success);
            
            AuditLog auditLog = new AuditLog();
            auditLog.setId("audit-" + System.currentTimeMillis());
            auditLog.setEventType(success ? AuditEventType.ESCALATION_EXECUTED : AuditEventType.ESCALATION_FAILED);
            auditLog.setEntityId(rule.getId());
            auditLog.setEntityType("EscalationRule");
            auditLog.setUserId("system");
            auditLog.setTimestamp(LocalDateTime.now());
            auditLog.setDescription("Escalation " + (success ? "executed successfully" : "failed to execute") + 
                                 " for alert: " + alert.getId());
            auditLog.setSeverity("HIGH");
            
            // Add details
            Map<String, Object> details = new ConcurrentHashMap<>();
            details.put("escalationLevel", rule.getEscalationLevel());
            details.put("targetRole", rule.getTargetRole());
            details.put("recipients", rule.getEscalationRecipients());
            details.put("alertId", alert.getId());
            details.put("alertType", alert.getAlertType());
            details.put("alertPriority", alert.getPriority());
            details.put("success", success);
            details.put("executedAt", LocalDateTime.now());
            auditLog.setDetails(details);
            
            auditLogRepository.save(auditLog);
            
        } catch (Exception e) {
            logger.error("Error logging escalation: {} for alert: {}", rule.getId(), alert.getId(), e);
        }
    }
    
    // Log escalation recall
    public void logEscalationRecall(EscalationRule rule, boolean success) {
        try {
            logger.info("Logging escalation recall: {} - {}", rule.getId(), success);
            
            AuditLog auditLog = new AuditLog();
            auditLog.setId("audit-" + System.currentTimeMillis());
            auditLog.setEventType(success ? AuditEventType.ESCALATION_RECALLED : AuditEventType.ESCALATION_RECALL_FAILED);
            auditLog.setEntityId(rule.getId());
            auditLog.setEntityType("EscalationRule");
            auditLog.setUserId("system");
            auditLog.setTimestamp(LocalDateTime.now());
            auditLog.setDescription("Escalation " + (success ? "recalled successfully" : "recall failed"));
            auditLog.setSeverity("MEDIUM");
            
            // Add details
            Map<String, Object> details = new ConcurrentHashMap<>();
            details.put("escalationLevel", rule.getEscalationLevel());
            details.put("targetRole", rule.getTargetRole());
            details.put("recipients", rule.getEscalationRecipients());
            details.put("success", success);
            details.put("recalledAt", LocalDateTime.now());
            auditLog.setDetails(details);
            
            auditLogRepository.save(auditLog);
            
        } catch (Exception e) {
            logger.error("Error logging escalation recall: {}", rule.getId(), e);
        }
    }
    
    // Log saga execution
    public void logSagaExecution(String sagaId, List<String> steps, boolean success) {
        try {
            logger.info("Logging saga execution: {} - {}", sagaId, success);
            
            AuditLog auditLog = new AuditLog();
            auditLog.setId("audit-" + System.currentTimeMillis());
            auditLog.setEventType(success ? AuditEventType.SAGA_COMPLETED : AuditEventType.SAGA_FAILED);
            auditLog.setEntityId(sagaId);
            auditLog.setEntityType("Saga");
            auditLog.setUserId("system");
            auditLog.setTimestamp(LocalDateTime.now());
            auditLog.setDescription("Saga " + (success ? "completed successfully" : "failed"));
            auditLog.setSeverity("MEDIUM");
            
            // Add details
            Map<String, Object> details = new ConcurrentHashMap<>();
            details.put("sagaId", sagaId);
            details.put("steps", steps);
            details.put("success", success);
            details.put("completedAt", LocalDateTime.now());
            auditLog.setDetails(details);
            
            auditLogRepository.save(auditLog);
            
        } catch (Exception e) {
            logger.error("Error logging saga execution: {}", sagaId, e);
        }
    }
    
    // Log user action
    public void logUserAction(String userId, String action, String entityType, String entityId, String description) {
        try {
            logger.info("Logging user action: {} - {} on {} {}", userId, action, entityType, entityId);
            
            AuditLog auditLog = new AuditLog();
            auditLog.setId("audit-" + System.currentTimeMillis());
            auditLog.setEventType(AuditEventType.USER_ACTION);
            auditLog.setEntityId(entityId);
            auditLog.setEntityType(entityType);
            auditLog.setUserId(userId);
            auditLog.setTimestamp(LocalDateTime.now());
            auditLog.setDescription(description);
            auditLog.setSeverity("INFO");
            
            // Add details
            Map<String, Object> details = new ConcurrentHashMap<>();
            details.put("action", action);
            details.put("userId", userId);
            details.put("entityType", entityType);
            details.put("entityId", entityId);
            auditLog.setDetails(details);
            
            auditLogRepository.save(auditLog);
            
        } catch (Exception e) {
            logger.error("Error logging user action: {} - {}", userId, action, e);
        }
    }
    
    // Get audit logs
    public List<AuditLog> getAuditLogs(LocalDateTime startTime, LocalDateTime endTime, int limit) {
        try {
            return auditLogRepository.findByTimestampBetweenOrderByTimestampDesc(startTime, endTime);
        } catch (Exception e) {
            logger.error("Error getting audit logs", e);
            return List.of();
        }
    }
    
    // Get audit logs by entity
    public List<AuditLog> getAuditLogsByEntity(String entityType, String entityId, int limit) {
        try {
            return auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(entityType, entityId);
        } catch (Exception e) {
            logger.error("Error getting audit logs by entity: {} {}", entityType, entityId, e);
            return List.of();
        }
    }
    
    // Get audit logs by user
    public List<AuditLog> getAuditLogsByUser(String userId, int limit) {
        try {
            return auditLogRepository.findByUserIdOrderByTimestampDesc(userId);
        } catch (Exception e) {
            logger.error("Error getting audit logs by user: {}", userId, e);
            return List.of();
        }
    }
    
    // Generate compliance report
    public String generateComplianceReport(LocalDateTime startTime, LocalDateTime endTime) {
        try {
            List<AuditLog> logs = getAuditLogs(startTime, endTime, 1000);
            
            StringBuilder report = new StringBuilder();
            report.append("Compliance Audit Report\n");
            report.append("========================\n");
            report.append("Period: ").append(startTime).append(" to ").append(endTime).append("\n");
            report.append("Generated: ").append(LocalDateTime.now()).append("\n\n");
            
            // Summary statistics
            Map<AuditEventType, Long> eventCounts = new ConcurrentHashMap<>();
            for (AuditLog log : logs) {
                eventCounts.merge(log.getEventType(), 1L, Long::sum);
            }
            
            report.append("Event Summary:\n");
            for (Map.Entry<AuditEventType, Long> entry : eventCounts.entrySet()) {
                report.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            
            // Recent events
            report.append("\nRecent Events (Last 20):\n");
            for (int i = 0; i < Math.min(20, logs.size()); i++) {
                AuditLog log = logs.get(i);
                report.append(String.format("%s - %s - %s - %s\n", 
                    log.getTimestamp(), log.getEventType(), log.getEntityType(), log.getDescription()));
            }
            
            return report.toString();
            
        } catch (Exception e) {
            logger.error("Error generating compliance report", e);
            return "Error generating compliance report";
        }
    }
    
    // Inner classes
    public enum AuditEventType {
        ALERT_CREATED,
        ALERT_PROCESSED,
        ALERT_PROCESSING_FAILED,
        NOTIFICATION_SENT,
        NOTIFICATION_FAILED,
        NOTIFICATION_RECALLED,
        NOTIFICATION_RECALL_FAILED,
        ESCALATION_EXECUTED,
        ESCALATION_FAILED,
        ESCALATION_RECALLED,
        ESCALATION_RECALL_FAILED,
        SAGA_COMPLETED,
        SAGA_FAILED,
        USER_ACTION
    }
    
    public static class AuditEvent {
        private String eventId;
        private AuditEventType eventType;
        private String entityId;
        private String entityType;
        private String userId;
        private LocalDateTime timestamp;
        private String description;
        private String severity;
        private Map<String, Object> details;
        
        // Getters and setters
        public String getEventId() { return eventId; }
        public void setEventId(String eventId) { this.eventId = eventId; }
        
        public AuditEventType getEventType() { return eventType; }
        public void setEventType(AuditEventType eventType) { this.eventType = eventType; }
        
        public String getEntityId() { return entityId; }
        public void setEntityId(String entityId) { this.entityId = entityId; }
        
        public String getEntityType() { return entityType; }
        public void setEntityType(String entityType) { this.entityType = entityType; }
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        
        public Map<String, Object> getDetails() { return details; }
        public void setDetails(Map<String, Object> details) { this.details = details; }
    }
}
