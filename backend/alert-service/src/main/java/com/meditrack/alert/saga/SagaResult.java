package com.meditrack.alert.saga;

import java.time.LocalDateTime;
import java.util.Map;

public class SagaResult {
    
    private boolean success;
    private String message;
    private LocalDateTime timestamp;
    private Map<String, Object> details;
    
    public SagaResult(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }
    
    public SagaResult(boolean success, String message, Map<String, Object> details) {
        this.success = success;
        this.message = message;
        this.details = details;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public Map<String, Object> getDetails() { return details; }
    
    // Setters
    public void setSuccess(boolean success) { this.success = success; }
    public void setMessage(String message) { this.message = message; }
    public void setDetails(Map<String, Object> details) { this.details = details; }
}
