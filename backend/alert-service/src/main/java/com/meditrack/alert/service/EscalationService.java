package com.meditrack.alert.service;

import com.meditrack.alert.entity.Alert;
import com.meditrack.alert.entity.EscalationRule;
import com.meditrack.alert.repository.EscalationRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EscalationService {
    
    private static final Logger logger = LoggerFactory.getLogger(EscalationService.class);
    
    @Autowired
    private EscalationRuleRepository escalationRuleRepository;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private AuditService auditService;
    
    // Active escalations tracking
    private final Map<String, EscalationContext> activeEscalations = new ConcurrentHashMap<>();
    
    // Check and execute escalation for alert
    public boolean checkAndEscalateAlert(Alert alert) {
        try {
            logger.info("Checking escalation for alert: {}", alert.getId());
            
            // Get applicable escalation rules
            List<EscalationRule> rules = getApplicableEscalationRules(alert);
            
            if (rules.isEmpty()) {
                logger.debug("No escalation rules found for alert: {}", alert.getId());
                return true;
            }
            
            boolean escalationExecuted = false;
            
            for (EscalationRule rule : rules) {
                if (shouldEscalate(alert, rule)) {
                    logger.info("Executing escalation for alert: {} with rule: {}", alert.getId(), rule.getId());
                    
                    boolean success = executeEscalation(rule, alert);
                    if (success) {
                        escalationExecuted = true;
                        trackEscalation(alert, rule);
                    } else {
                        logger.error("Failed to execute escalation for alert: {} with rule: {}", alert.getId(), rule.getId());
                    }
                }
            }
            
            return escalationExecuted;
            
        } catch (Exception e) {
            logger.error("Error checking escalation for alert: {}", alert.getId(), e);
            return false;
        }
    }
    
    // Execute escalation
    public boolean executeEscalation(EscalationRule rule) {
        try {
            logger.info("Executing escalation rule: {}", rule.getId());
            
            // Create escalation context
            EscalationContext context = new EscalationContext(rule, LocalDateTime.now());
            activeEscalations.put(rule.getId(), context);
            
            // Send escalation notifications
            boolean notificationSuccess = sendEscalationNotifications(rule);
            
            // Update escalation status
            context.setStatus(notificationSuccess ? EscalationStatus.EXECUTED : EscalationStatus.FAILED);
            
            // Log escalation for audit
            auditService.logEscalation(rule, notificationSuccess);
            
            return notificationSuccess;
            
        } catch (Exception e) {
            logger.error("Error executing escalation rule: {}", rule.getId(), e);
            return false;
        }
    }
    
    // Execute escalation with alert context
    private boolean executeEscalation(EscalationRule rule, Alert alert) {
        try {
            // Create escalation context
            EscalationContext context = new EscalationContext(rule, alert, LocalDateTime.now());
            activeEscalations.put(rule.getId() + "-" + alert.getId(), context);
            
            // Send escalation notifications
            boolean notificationSuccess = sendEscalationNotifications(rule, alert);
            
            // Update escalation status
            context.setStatus(notificationSuccess ? EscalationStatus.EXECUTED : EscalationStatus.FAILED);
            
            // Log escalation for audit
            auditService.logEscalation(rule, alert, notificationSuccess);
            
            return notificationSuccess;
            
        } catch (Exception e) {
            logger.error("Error executing escalation for alert: {} with rule: {}", alert.getId(), rule.getId(), e);
            return false;
        }
    }
    
    // Undo escalation
    public boolean undoEscalation(EscalationRule rule) {
        try {
            logger.info("Undoing escalation rule: {}", rule.getId());
            
            EscalationContext context = activeEscalations.get(rule.getId());
            if (context == null) {
                logger.warn("No active escalation found for rule: {}", rule.getId());
                return true;
            }
            
            // Send escalation recall notifications
            boolean recallSuccess = sendEscalationRecallNotifications(rule);
            
            // Update escalation status
            context.setStatus(recallSuccess ? EscalationStatus.RECALLED : EscalationStatus.RECALL_FAILED);
            
            // Remove from active escalations
            activeEscalations.remove(rule.getId());
            
            // Log recall for audit
            auditService.logEscalationRecall(rule, recallSuccess);
            
            return recallSuccess;
            
        } catch (Exception e) {
            logger.error("Error undoing escalation rule: {}", rule.getId(), e);
            return false;
        }
    }
    
    // Get applicable escalation rules for alert
    private List<EscalationRule> getApplicableEscalationRules(Alert alert) {
        try {
            return escalationRuleRepository.findByAlertTypeAndPriorityAndActive(
                alert.getAlertType(), alert.getPriority(), true);
        } catch (Exception e) {
            logger.error("Error getting escalation rules for alert: {}", alert.getId(), e);
            return List.of();
        }
    }
    
    // Check if alert should be escalated
    private boolean shouldEscalate(Alert alert, EscalationRule rule) {
        try {
            // Check time-based escalation
            if (rule.getTimeBasedEscalation() != null && rule.getTimeBasedEscalation()) {
                LocalDateTime escalationTime = alert.getCreatedAt().plusMinutes(rule.getEscalationDelayMinutes());
                if (LocalDateTime.now().isAfter(escalationTime)) {
                    return true;
                }
            }
            
            // Check condition-based escalation
            if (rule.getConditionBasedEscalation() != null && rule.getConditionBasedEscalation()) {
                return evaluateEscalationCondition(alert, rule);
            }
            
            // Check priority-based escalation
            if (rule.getPriorityBasedEscalation() != null && rule.getPriorityBasedEscalation()) {
                return alert.getPriority().ordinal() >= rule.getMinPriorityLevel().ordinal();
            }
            
            return false;
            
        } catch (Exception e) {
            logger.error("Error evaluating escalation condition for alert: {} with rule: {}", alert.getId(), rule.getId(), e);
            return false;
        }
    }
    
    // Evaluate escalation condition
    private boolean evaluateEscalationCondition(Alert alert, EscalationRule rule) {
        try {
            String condition = rule.getEscalationCondition();
            
            // Simple condition evaluation (in production, use a proper expression parser)
            if (condition.contains("priority")) {
                return condition.contains(alert.getPriority().toString());
            }
            
            if (condition.contains("age")) {
                long ageMinutes = java.time.Duration.between(alert.getCreatedAt(), LocalDateTime.now()).toMinutes();
                return condition.contains(">" + ageMinutes);
            }
            
            return false;
            
        } catch (Exception e) {
            logger.error("Error evaluating escalation condition: {}", rule.getEscalationCondition(), e);
            return false;
        }
    }
    
    // Send escalation notifications
    private boolean sendEscalationNotifications(EscalationRule rule) {
        try {
            boolean allSuccess = true;
            
            for (String recipient : rule.getEscalationRecipients()) {
                boolean success = notificationService.sendEscalationNotification(
                    recipient, rule.getEscalationMessage(), rule.getEscalationLevel());
                
                if (!success) {
                    allSuccess = false;
                    logger.error("Failed to send escalation notification to: {}", recipient);
                }
            }
            
            return allSuccess;
            
        } catch (Exception e) {
            logger.error("Error sending escalation notifications for rule: {}", rule.getId(), e);
            return false;
        }
    }
    
    // Send escalation notifications with alert context
    private boolean sendEscalationNotifications(EscalationRule rule, Alert alert) {
        try {
            boolean allSuccess = true;
            
            for (String recipient : rule.getEscalationRecipients()) {
                String message = String.format("%s\n\nAlert Details:\nType: %s\nPriority: %s\nMessage: %s\nCreated: %s",
                    rule.getEscalationMessage(),
                    alert.getAlertType(),
                    alert.getPriority(),
                    alert.getMessage(),
                    alert.getCreatedAt());
                
                boolean success = notificationService.sendEscalationNotification(
                    recipient, message, rule.getEscalationLevel());
                
                if (!success) {
                    allSuccess = false;
                    logger.error("Failed to send escalation notification to: {}", recipient);
                }
            }
            
            return allSuccess;
            
        } catch (Exception e) {
            logger.error("Error sending escalation notifications for alert: {} with rule: {}", alert.getId(), rule.getId(), e);
            return false;
        }
    }
    
    // Send escalation recall notifications
    private boolean sendEscalationRecallNotifications(EscalationRule rule) {
        try {
            boolean allSuccess = true;
            
            for (String recipient : rule.getEscalationRecipients()) {
                boolean success = notificationService.sendEscalationRecallNotification(
                    recipient, "Escalation has been recalled for rule: " + rule.getId());
                
                if (!success) {
                    allSuccess = false;
                    logger.error("Failed to send escalation recall notification to: {}", recipient);
                }
            }
            
            return allSuccess;
            
        } catch (Exception e) {
            logger.error("Error sending escalation recall notifications for rule: {}", rule.getId(), e);
            return false;
        }
    }
    
    // Track escalation
    private void trackEscalation(Alert alert, EscalationRule rule) {
        try {
            EscalationContext context = activeEscalations.get(rule.getId() + "-" + alert.getId());
            if (context != null) {
                context.setEscalatedAt(LocalDateTime.now());
                context.setStatus(EscalationStatus.EXECUTED);
            }
        } catch (Exception e) {
            logger.error("Error tracking escalation for alert: {} with rule: {}", alert.getId(), rule.getId(), e);
        }
    }
    
    // Get active escalations
    public Map<String, EscalationContext> getActiveEscalations() {
        return new ConcurrentHashMap<>(activeEscalations);
    }
    
    // Get escalation history
    public List<EscalationContext> getEscalationHistory(int limit) {
        // This would typically fetch from database
        return List.of();
    }
    
    // Inner classes
    public static class EscalationContext {
        private EscalationRule rule;
        private Alert alert;
        private LocalDateTime createdAt;
        private LocalDateTime escalatedAt;
        private EscalationStatus status;
        
        public EscalationContext(EscalationRule rule, LocalDateTime createdAt) {
            this.rule = rule;
            this.createdAt = createdAt;
            this.status = EscalationStatus.PENDING;
        }
        
        public EscalationContext(EscalationRule rule, Alert alert, LocalDateTime createdAt) {
            this.rule = rule;
            this.alert = alert;
            this.createdAt = createdAt;
            this.status = EscalationStatus.PENDING;
        }
        
        // Getters and setters
        public EscalationRule getRule() { return rule; }
        public Alert getAlert() { return alert; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getEscalatedAt() { return escalatedAt; }
        public EscalationStatus getStatus() { return status; }
        
        public void setEscalatedAt(LocalDateTime escalatedAt) { this.escalatedAt = escalatedAt; }
        public void setStatus(EscalationStatus status) { this.status = status; }
    }
    
    public enum EscalationStatus {
        PENDING, EXECUTED, FAILED, RECALLED, RECALL_FAILED
    }
}
