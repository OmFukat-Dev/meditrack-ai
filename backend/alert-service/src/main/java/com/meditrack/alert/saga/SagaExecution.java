package com.meditrack.alert.saga;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SagaExecution {
    
    private String sagaId;
    private List<SagaStep> steps;
    private List<SagaStep> completedSteps;
    private SagaStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    
    public SagaExecution(String sagaId, List<SagaStep> steps) {
        this.sagaId = sagaId;
        this.steps = new ArrayList<>(steps);
        this.completedSteps = new ArrayList<>();
        this.status = SagaStatus.RUNNING;
        this.startTime = LocalDateTime.now();
    }
    
    public void addCompletedStep(SagaStep step) {
        completedSteps.add(step);
    }
    
    // Getters
    public String getSagaId() { return sagaId; }
    public List<SagaStep> getSteps() { return steps; }
    public List<SagaStep> getCompletedSteps() { return completedSteps; }
    public SagaStatus getStatus() { return status; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    
    // Setters
    public void setStatus(SagaStatus status) { 
        this.status = status;
        if (status != SagaStatus.RUNNING && endTime == null) {
            this.endTime = LocalDateTime.now();
        }
    }
}
