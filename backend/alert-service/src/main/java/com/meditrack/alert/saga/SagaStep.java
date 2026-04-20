package com.meditrack.alert.saga;

import java.util.concurrent.CompletableFuture;

public abstract class SagaStep {
    
    protected String stepId;
    protected String stepName;
    protected Map<String, Object> context;
    
    public SagaStep(String stepId, String stepName) {
        this.stepId = stepId;
        this.stepName = stepName;
        this.context = new HashMap<>();
    }
    
    // Execute the step
    public abstract CompletableFuture<StepResult> execute();
    
    // Compensate the step (undo the action)
    public abstract CompletableFuture<StepResult> compensate();
    
    // Getters
    public String getStepId() { return stepId; }
    public String getStepName() { return stepName; }
    public Map<String, Object> getContext() { return context; }
    
    // Set context
    public void setContext(Map<String, Object> context) { this.context = context; }
    
    // Add context value
    public void addContextValue(String key, Object value) { context.put(key, value); }
    
    // Get context value
    public Object getContextValue(String key) { return context.get(key); }
}
