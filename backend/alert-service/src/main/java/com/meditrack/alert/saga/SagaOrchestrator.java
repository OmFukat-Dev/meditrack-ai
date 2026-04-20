package com.meditrack.alert.saga;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SagaOrchestrator {
    
    private static final Logger logger = LoggerFactory.getLogger(SagaOrchestrator.class);
    
    private final Map<String, SagaExecution> activeSagas = new ConcurrentHashMap<>();
    private final Map<String, List<SagaStep>> sagaSteps = new ConcurrentHashMap<>();
    
    // Execute saga with compensation
    public CompletableFuture<SagaResult> executeSaga(String sagaId, List<SagaStep> steps) {
        try {
            logger.info("Starting saga execution: {}", sagaId);
            
            SagaExecution execution = new SagaExecution(sagaId, steps);
            activeSagas.put(sagaId, execution);
            sagaSteps.put(sagaId, new ArrayList<>(steps));
            
            return executeStepsSequentially(execution, 0)
                .thenApply(result -> {
                    if (result.isSuccess()) {
                        logger.info("Saga {} completed successfully", sagaId);
                    } else {
                        logger.error("Saga {} failed, initiating compensation", sagaId);
                        executeCompensation(sagaId);
                    }
                    activeSagas.remove(sagaId);
                    return result;
                })
                .exceptionally(throwable -> {
                    logger.error("Saga {} failed with exception", sagaId, throwable);
                    executeCompensation(sagaId);
                    activeSagas.remove(sagaId);
                    return new SagaResult(false, throwable.getMessage());
                });
                
        } catch (Exception e) {
            logger.error("Error starting saga: {}", sagaId, e);
            return CompletableFuture.completedFuture(new SagaResult(false, e.getMessage()));
        }
    }
    
    // Execute steps sequentially
    private CompletableFuture<SagaResult> executeStepsSequentially(SagaExecution execution, int stepIndex) {
        if (stepIndex >= execution.getSteps().size()) {
            return CompletableFuture.completedFuture(new SagaResult(true, "All steps completed"));
        }
        
        SagaStep step = execution.getSteps().get(stepIndex);
        logger.debug("Executing step {} for saga {}", stepIndex, execution.getSagaId());
        
        return step.execute()
            .thenCompose(result -> {
                if (result.isSuccess()) {
                    execution.addCompletedStep(step);
                    return executeStepsSequentially(execution, stepIndex + 1);
                } else {
                    return CompletableFuture.completedFuture(result);
                }
            });
    }
    
    // Execute compensation for failed saga
    private void executeCompensation(String sagaId) {
        try {
            SagaExecution execution = activeSagas.get(sagaId);
            if (execution == null) return;
            
            logger.info("Starting compensation for saga: {}", sagaId);
            
            List<SagaStep> completedSteps = execution.getCompletedSteps();
            Collections.reverse(completedSteps);
            
            for (SagaStep step : completedSteps) {
                try {
                    step.compensate();
                    logger.debug("Compensated step: {}", step.getClass().getSimpleName());
                } catch (Exception e) {
                    logger.error("Error compensating step: {}", step.getClass().getSimpleName(), e);
                }
            }
            
            logger.info("Compensation completed for saga: {}", sagaId);
            
        } catch (Exception e) {
            logger.error("Error during compensation for saga: {}", sagaId, e);
        }
    }
    
    // Get saga status
    public SagaStatus getSagaStatus(String sagaId) {
        SagaExecution execution = activeSagas.get(sagaId);
        if (execution == null) {
            return SagaStatus.COMPLETED;
        }
        return execution.getStatus();
    }
}
