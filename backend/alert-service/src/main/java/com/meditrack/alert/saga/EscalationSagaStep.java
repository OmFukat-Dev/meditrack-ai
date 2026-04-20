package com.meditrack.alert.saga;

import com.meditrack.alert.service.EscalationService;
import com.meditrack.alert.entity.EscalationRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class EscalationSagaStep extends SagaStep {
    
    private static final Logger logger = LoggerFactory.getLogger(EscalationSagaStep.class);
    
    private final EscalationService escalationService;
    private final EscalationRule escalationRule;
    
    public EscalationSagaStep(EscalationService escalationService, EscalationRule escalationRule) {
        super("escalation-" + escalationRule.getId(), "Alert Escalation");
        this.escalationService = escalationService;
        this.escalationRule = escalationRule;
        this.addContextValue("escalationRuleId", escalationRule.getId());
        this.addContextValue("escalationLevel", escalationRule.getEscalationLevel());
        this.addContextValue("targetRole", escalationRule.getTargetRole());
    }
    
    @Override
    public CompletableFuture<StepResult> execute() {
        try {
            logger.info("Executing escalation step for rule: {}", escalationRule.getId());
            
            // Execute escalation
            boolean success = escalationService.executeEscalation(escalationRule);
            
            if (success) {
                addContextValue("escalatedAt", java.time.LocalDateTime.now());
                return CompletableFuture.completedFuture(
                    new StepResult(true, "Escalation executed successfully")
                );
            } else {
                return CompletableFuture.completedFuture(
                    new StepResult(false, "Failed to execute escalation")
                );
            }
            
        } catch (Exception e) {
            logger.error("Error executing escalation step for rule: {}", escalationRule.getId(), e);
            return CompletableFuture.completedFuture(
                new StepResult(false, "Error executing escalation: " + e.getMessage())
            );
        }
    }
    
    @Override
    public CompletableFuture<StepResult> compensate() {
        try {
            logger.info("Compensating escalation step for rule: {}", escalationRule.getId());
            
            // Undo escalation
            boolean success = escalationService.undoEscalation(escalationRule);
            
            if (success) {
                return CompletableFuture.completedFuture(
                    new StepResult(true, "Escalation compensation successful")
                );
            } else {
                return CompletableFuture.completedFuture(
                    new StepResult(false, "Failed to compensate escalation")
                );
            }
            
        } catch (Exception e) {
            logger.error("Error compensating escalation step for rule: {}", escalationRule.getId(), e);
            return CompletableFuture.completedFuture(
                new StepResult(false, "Error compensating escalation: " + e.getMessage())
            );
        }
    }
}
