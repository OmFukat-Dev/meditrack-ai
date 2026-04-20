package com.meditrack.alert.controller;

import com.meditrack.alert.service.*;
import com.meditrack.alert.entity.Alert;
import com.meditrack.alert.entity.EscalationRule;
import com.meditrack.alert.entity.Notification;
import com.meditrack.alert.saga.SagaOrchestrator;
import com.meditrack.alert.saga.SagaStep;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alert-service")
@CrossOrigin(origins = "*")
public class AlertServiceController {
    
    private static final Logger logger = LoggerFactory.getLogger(AlertServiceController.class);
    
    @Autowired
    private AlertService alertService;
    
    @Autowired
    private EscalationService escalationService;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private AuditService auditService;
    
    @Autowired
    private SagaOrchestrator sagaOrchestrator;
    
    // Alert Management Endpoints
    @PostMapping("/alerts")
    @Counted(value = "alert.create", description = "Number of alerts created")
    @Timed(value = "alert.create.time", description = "Time taken to create alert")
    public ResponseEntity<?> createAlert(@Valid @RequestBody Alert alert) {
        try {
            logger.info("Creating alert: {}", alert.getId());
            
            Alert createdAlert = alertService.createAlert(alert);
            
            // Log alert creation
            auditService.logAlertCreation(createdAlert);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(createdAlert);
            
        } catch (Exception e) {
            logger.error("Error creating alert", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to create alert", "message", e.getMessage()));
        }
    }
    
    @PostMapping("/alerts/{alertId}/process")
    @Counted(value = "alert.process", description = "Number of alerts processed")
    @Timed(value = "alert.process.time", description = "Time taken to process alert")
    public ResponseEntity<?> processAlert(@PathVariable String alertId) {
        try {
            logger.info("Processing alert: {}", alertId);
            
            boolean success = alertService.processAlert(alertId);
            
            // Log alert processing
            Alert alert = alertService.getAlert(alertId);
            if (alert != null) {
                auditService.logAlertProcessing(alert, success);
            }
            
            if (success) {
                return ResponseEntity.ok(Map.of("message", "Alert processed successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to process alert"));
            }
            
        } catch (Exception e) {
            logger.error("Error processing alert: {}", alertId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to process alert", "message", e.getMessage()));
        }
    }
    
    @GetMapping("/alerts/{alertId}")
    @Counted(value = "alert.get", description = "Number of alert requests")
    @Timed(value = "alert.get.time", description = "Time taken to get alert")
    public ResponseEntity<?> getAlert(@PathVariable String alertId) {
        try {
            Alert alert = alertService.getAlert(alertId);
            
            if (alert == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Alert not found", "alertId", alertId));
            }
            
            return ResponseEntity.ok(alert);
            
        } catch (Exception e) {
            logger.error("Error getting alert: {}", alertId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get alert", "message", e.getMessage()));
        }
    }
    
    @GetMapping("/alerts")
    @Counted(value = "alert.list", description = "Number of alert list requests")
    @Timed(value = "alert.list.time", description = "Time taken to list alerts")
    public ResponseEntity<?> getAlerts(@RequestParam(defaultValue = "100") int limit) {
        try {
            List<Alert> alerts = alertService.getAlerts(limit);
            return ResponseEntity.ok(alerts);
            
        } catch (Exception e) {
            logger.error("Error getting alerts", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get alerts", "message", e.getMessage()));
        }
    }
    
    // Escalation Endpoints
    @PostMapping("/escalations/check/{alertId}")
    @Counted(value = "escalation.check", description = "Number of escalation checks")
    @Timed(value = "escalation.check.time", description = "Time taken to check escalation")
    public ResponseEntity<?> checkEscalation(@PathVariable String alertId) {
        try {
            logger.info("Checking escalation for alert: {}", alertId);
            
            Alert alert = alertService.getAlert(alertId);
            if (alert == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Alert not found", "alertId", alertId));
            }
            
            boolean escalationExecuted = escalationService.checkAndEscalateAlert(alert);
            
            return ResponseEntity.ok(Map.of(
                "message", escalationExecuted ? "Escalation executed" : "No escalation needed",
                "escalationExecuted", escalationExecuted
            ));
            
        } catch (Exception e) {
            logger.error("Error checking escalation for alert: {}", alertId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to check escalation", "message", e.getMessage()));
        }
    }
    
    @PostMapping("/escalations/execute/{ruleId}")
    @Counted(value = "escalation.execute", description = "Number of escalation executions")
    @Timed(value = "escalation.execute.time", description = "Time taken to execute escalation")
    public ResponseEntity<?> executeEscalation(@PathVariable String ruleId) {
        try {
            logger.info("Executing escalation rule: {}", ruleId);
            
            // Get escalation rule (simplified)
            EscalationRule rule = new EscalationRule();
            rule.setId(ruleId);
            
            boolean success = escalationService.executeEscalation(rule);
            
            if (success) {
                return ResponseEntity.ok(Map.of("message", "Escalation executed successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to execute escalation"));
            }
            
        } catch (Exception e) {
            logger.error("Error executing escalation: {}", ruleId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to execute escalation", "message", e.getMessage()));
        }
    }
    
    @GetMapping("/escalations/active")
    @Counted(value = "escalation.active", description = "Number of active escalation requests")
    @Timed(value = "escalation.active.time", description = "Time taken to get active escalations")
    public ResponseEntity<?> getActiveEscalations() {
        try {
            Map<String, EscalationService.EscalationContext> activeEscalations = 
                escalationService.getActiveEscalations();
            
            return ResponseEntity.ok(activeEscalations);
            
        } catch (Exception e) {
            logger.error("Error getting active escalations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get active escalations", "message", e.getMessage()));
        }
    }
    
    // Notification Endpoints
    @PostMapping("/notifications")
    @Counted(value = "notification.send", description = "Number of notifications sent")
    @Timed(value = "notification.send.time", description = "Time taken to send notification")
    public ResponseEntity<?> sendNotification(@Valid @RequestBody Notification notification) {
        try {
            logger.info("Sending notification: {}", notification.getId());
            
            boolean success = notificationService.sendNotification(notification);
            
            // Log notification
            auditService.logNotification(notification, success);
            
            if (success) {
                return ResponseEntity.ok(Map.of("message", "Notification sent successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to send notification"));
            }
            
        } catch (Exception e) {
            logger.error("Error sending notification", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to send notification", "message", e.getMessage()));
        }
    }
    
    @PostMapping("/notifications/multi-channel")
    @Counted(value = "notification.multi.channel", description = "Number of multi-channel notifications")
    @Timed(value = "notification.multi.channel.time", description = "Time taken to send multi-channel notification")
    public ResponseEntity<?> sendMultiChannelNotification(@Valid @RequestBody MultiChannelNotificationRequest request) {
        try {
            logger.info("Sending multi-channel notification: {}", request.getNotification().getId());
            
            boolean success = notificationService.sendMultiChannelNotification(
                request.getNotification(), request.getChannels());
            
            if (success) {
                return ResponseEntity.ok(Map.of("message", "Multi-channel notification sent successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to send multi-channel notification"));
            }
            
        } catch (Exception e) {
            logger.error("Error sending multi-channel notification", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to send multi-channel notification", "message", e.getMessage()));
        }
    }
    
    @PostMapping("/notifications/{notificationId}/recall")
    @Counted(value = "notification.recall", description = "Number of notification recalls")
    @Timed(value = "notification.recall.time", description = "Time taken to recall notification")
    public ResponseEntity<?> recallNotification(@PathVariable String notificationId) {
        try {
            logger.info("Recalling notification: {}", notificationId);
            
            Notification notification = new Notification();
            notification.setId(notificationId);
            
            boolean success = notificationService.recallNotification(notification);
            
            // Log recall
            auditService.logNotificationRecall(notification, success);
            
            if (success) {
                return ResponseEntity.ok(Map.of("message", "Notification recalled successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to recall notification"));
            }
            
        } catch (Exception e) {
            logger.error("Error recalling notification: {}", notificationId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to recall notification", "message", e.getMessage()));
        }
    }
    
    @GetMapping("/notifications/{notificationId}/status")
    @Counted(value = "notification.status", description = "Number of notification status requests")
    @Timed(value = "notification.status.time", description = "Time taken to get notification status")
    public ResponseEntity<?> getNotificationStatus(@PathVariable String notificationId) {
        try {
            NotificationService.NotificationStatus status = notificationService.getNotificationStatus(notificationId);
            
            return ResponseEntity.ok(Map.of(
                "notificationId", notificationId,
                "status", status
            ));
            
        } catch (Exception e) {
            logger.error("Error getting notification status: {}", notificationId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get notification status", "message", e.getMessage()));
        }
    }
    
    @GetMapping("/notifications/active")
    @Counted(value = "notification.active", description = "Number of active notification requests")
    @Timed(value = "notification.active.time", description = "Time taken to get active notifications")
    public ResponseEntity<?> getActiveNotifications() {
        try {
            Map<String, NotificationService.NotificationContext> activeNotifications = 
                notificationService.getActiveNotifications();
            
            return ResponseEntity.ok(activeNotifications);
            
        } catch (Exception e) {
            logger.error("Error getting active notifications", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get active notifications", "message", e.getMessage()));
        }
    }
    
    // Saga Endpoints
    @PostMapping("/saga/execute")
    @Counted(value = "saga.execute", description = "Number of saga executions")
    @Timed(value = "saga.execute.time", description = "Time taken to execute saga")
    public ResponseEntity<?> executeSaga(@Valid @RequestBody SagaExecutionRequest request) {
        try {
            logger.info("Executing saga: {}", request.getSagaId());
            
            // Create saga steps (simplified)
            List<SagaStep> steps = createSagaSteps(request);
            
            return sagaOrchestrator.executeSaga(request.getSagaId(), steps)
                .thenApply(result -> {
                    // Log saga execution
                    auditService.logSagaExecution(request.getSagaId(), 
                        steps.stream().map(step -> step.getStepName()).toList(), 
                        result.isSuccess());
                    
                    if (result.isSuccess()) {
                        return ResponseEntity.ok(result);
                    } else {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
                    }
                })
                .join();
            
        } catch (Exception e) {
            logger.error("Error executing saga: {}", request.getSagaId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to execute saga", "message", e.getMessage()));
        }
    }
    
    @GetMapping("/saga/{sagaId}/status")
    @Counted(value = "saga.status", description = "Number of saga status requests")
    @Timed(value = "saga.status.time", description = "Time taken to get saga status")
    public ResponseEntity<?> getSagaStatus(@PathVariable String sagaId) {
        try {
            SagaStatus status = sagaOrchestrator.getSagaStatus(sagaId);
            
            return ResponseEntity.ok(Map.of(
                "sagaId", sagaId,
                "status", status
            ));
            
        } catch (Exception e) {
            logger.error("Error getting saga status: {}", sagaId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get saga status", "message", e.getMessage()));
        }
    }
    
    // Audit Endpoints
    @GetMapping("/audit/logs")
    @Counted(value = "audit.logs", description = "Number of audit log requests")
    @Timed(value = "audit.logs.time", description = "Time taken to get audit logs")
    public ResponseEntity<?> getAuditLogs(@RequestParam(required = false) LocalDateTime startTime,
                                        @RequestParam(required = false) LocalDateTime endTime,
                                        @RequestParam(defaultValue = "100") int limit) {
        try {
            if (startTime == null) {
                startTime = LocalDateTime.now().minusHours(24);
            }
            if (endTime == null) {
                endTime = LocalDateTime.now();
            }
            
            List<AuditService.AuditLog> logs = auditService.getAuditLogs(startTime, endTime, limit);
            
            return ResponseEntity.ok(logs);
            
        } catch (Exception e) {
            logger.error("Error getting audit logs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get audit logs", "message", e.getMessage()));
        }
    }
    
    @GetMapping("/audit/logs/entity/{entityType}/{entityId}")
    @Counted(value = "audit.logs.entity", description = "Number of entity audit log requests")
    @Timed(value = "audit.logs.entity.time", description = "Time taken to get entity audit logs")
    public ResponseEntity<?> getAuditLogsByEntity(@PathVariable String entityType, 
                                                @PathVariable String entityId,
                                                @RequestParam(defaultValue = "50") int limit) {
        try {
            List<AuditService.AuditLog> logs = auditService.getAuditLogsByEntity(entityType, entityId, limit);
            
            return ResponseEntity.ok(logs);
            
        } catch (Exception e) {
            logger.error("Error getting audit logs by entity: {} {}", entityType, entityId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get audit logs", "message", e.getMessage()));
        }
    }
    
    @GetMapping("/audit/logs/user/{userId}")
    @Counted(value = "audit.logs.user", description = "Number of user audit log requests")
    @Timed(value = "audit.logs.user.time", description = "Time taken to get user audit logs")
    public ResponseEntity<?> getAuditLogsByUser(@PathVariable String userId,
                                              @RequestParam(defaultValue = "50") int limit) {
        try {
            List<AuditService.AuditLog> logs = auditService.getAuditLogsByUser(userId, limit);
            
            return ResponseEntity.ok(logs);
            
        } catch (Exception e) {
            logger.error("Error getting audit logs by user: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get audit logs", "message", e.getMessage()));
        }
    }
    
    @GetMapping("/audit/compliance-report")
    @Counted(value = "audit.compliance.report", description = "Number of compliance report requests")
    @Timed(value = "audit.compliance.report.time", description = "Time taken to generate compliance report")
    public ResponseEntity<?> generateComplianceReport(@RequestParam(required = false) LocalDateTime startTime,
                                                   @RequestParam(required = false) LocalDateTime endTime) {
        try {
            if (startTime == null) {
                startTime = LocalDateTime.now().minusDays(30);
            }
            if (endTime == null) {
                endTime = LocalDateTime.now();
            }
            
            String report = auditService.generateComplianceReport(startTime, endTime);
            
            return ResponseEntity.ok(Map.of(
                "startTime", startTime,
                "endTime", endTime,
                "report", report,
                "generatedAt", LocalDateTime.now()
            ));
            
        } catch (Exception e) {
            logger.error("Error generating compliance report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to generate compliance report", "message", e.getMessage()));
        }
    }
    
    // Health Check Endpoint
    @GetMapping("/health")
    @Counted(value = "alert.health", description = "Number of health checks")
    @Timed(value = "alert.health.time", description = "Time taken for health check")
    public ResponseEntity<?> healthCheck() {
        try {
            boolean isHealthy = true; // Simplified health check
            
            Map<String, Object> health = Map.of(
                "status", isHealthy ? "UP" : "DOWN",
                "timestamp", LocalDateTime.now(),
                "service", "alert-service",
                "components", Map.of(
                    "alertService", "UP",
                    "escalationService", "UP",
                    "notificationService", "UP",
                    "auditService", "UP",
                    "sagaOrchestrator", "UP"
                )
            );
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            logger.error("Error in health check: {}", e.getMessage(), e);
            
            Map<String, Object> health = Map.of(
                "status", "DOWN",
                "timestamp", LocalDateTime.now(),
                "service", "alert-service",
                "error", e.getMessage()
            );
            
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(health);
        }
    }
    
    // Helper methods
    private List<SagaStep> createSagaSteps(SagaExecutionRequest request) {
        List<SagaStep> steps = new ArrayList<>();
        
        // This would create actual saga steps based on the request
        // For now, return empty list as placeholder
        return steps;
    }
    
    // Request DTOs
    public static class MultiChannelNotificationRequest {
        private Notification notification;
        private List<NotificationService.NotificationChannel> channels;
        
        // Getters and setters
        public Notification getNotification() { return notification; }
        public void setNotification(Notification notification) { this.notification = notification; }
        
        public List<NotificationService.NotificationChannel> getChannels() { return channels; }
        public void setChannels(List<NotificationService.NotificationChannel> channels) { this.channels = channels; }
    }
    
    public static class SagaExecutionRequest {
        private String sagaId;
        private String sagaType;
        private Map<String, Object> parameters;
        
        // Getters and setters
        public String getSagaId() { return sagaId; }
        public void setSagaId(String sagaId) { this.sagaId = sagaId; }
        
        public String getSagaType() { return sagaType; }
        public void setSagaType(String sagaType) { this.sagaType = sagaType; }
        
        public Map<String, Object> getParameters() { return parameters; }
        public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
    }
}
