package com.meditrack.alert.dto;

import java.time.LocalDateTime;

public class AlertEvent {
    private String eventId;
    private String eventType;
    private LocalDateTime timestamp;
    private String patientId;
    private String severity;
    private String message;
    private String vitalType;
    private Object value;
    private String department;
    private String createdBy;
    private String role;
    
    // Constructors
    public AlertEvent() {}
    
    public AlertEvent(String patientId, String severity, String message) {
        this.patientId = patientId;
        this.severity = severity;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getVitalType() { return vitalType; }
    public void setVitalType(String vitalType) { this.vitalType = vitalType; }
    
    public Object getValue() { return value; }
    public void setValue(Object value) { this.value = value; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
