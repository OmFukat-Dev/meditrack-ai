package com.meditrack.alert.saga;

import com.meditrack.alert.service.AlertService;
import com.meditrack.alert.entity.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class AlertSagaStep extends SagaStep {
    
    private static final Logger logger = LoggerFactory.getLogger(AlertSagaStep.class);
    
    private final AlertService alertService;
    private final Alert alert;
    
    public AlertSagaStep(AlertService alertService, Alert alert) {
        super("alert-" + alert.getId(), "Alert Processing");
        this.alertService = alertService;
        this.alert = alert;
        this.addContextValue("alertId", alert.getId());
        this.addContextValue("alertType", alert.getAlertType());
        this.addContextValue("priority", alert.getPriority());
    }
    
    @Override
    public CompletableFuture<StepResult> execute() {
        try {
            logger.info("Executing alert step for alert: {}", alert.getId());
            
            // Process the alert
            boolean success = alertService.processAlert(alert);
            
            if (success) {
                addContextValue("processedAt", java.time.LocalDateTime.now());
                return CompletableFuture.completedFuture(
                    new StepResult(true, "Alert processed successfully")
                );
            } else {
                return CompletableFuture.completedFuture(
                    new StepResult(false, "Failed to process alert")
                );
            }
            
        } catch (Exception e) {
            logger.error("Error executing alert step for alert: {}", alert.getId(), e);
            return CompletableFuture.completedFuture(
                new StepResult(false, "Error processing alert: " + e.getMessage())
            );
        }
    }
    
    @Override
    public CompletableFuture<StepResult> compensate() {
        try {
            logger.info("Compensating alert step for alert: {}", alert.getId());
            
            // Undo alert processing
            boolean success = alertService.undoAlertProcessing(alert);
            
            if (success) {
                return CompletableFuture.completedFuture(
                    new StepResult(true, "Alert compensation successful")
                );
            } else {
                return CompletableFuture.completedFuture(
                    new StepResult(false, "Failed to compensate alert")
                );
            }
            
        } catch (Exception e) {
            logger.error("Error compensating alert step for alert: {}", alert.getId(), e);
            return CompletableFuture.completedFuture(
                new StepResult(false, "Error compensating alert: " + e.getMessage())
            );
        }
    }
}
