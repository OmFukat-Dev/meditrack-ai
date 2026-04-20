package com.meditrack.alert.saga;

import java.time.LocalDateTime;

public class StepResult {
    
    private boolean success;
    private String message;
    private LocalDateTime timestamp;
    
    public StepResult(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public LocalDateTime getTimestamp() { return timestamp; }
    
    // Setters
    public void setSuccess(boolean success) { this.success = success; }
    public void setMessage(String message) { this.message = message; }
}
