package com.meditrack.patient.dto;

import java.time.LocalDateTime;

public class PatientTimelineEvent {
    private String eventId;
    private String eventType;
    private LocalDateTime timestamp;
    private String patientId;
    private String vitalType;
    private Object value;
    private String unit;
    private String severity;
    private String message;
    private String riskLevel;
    private String confidence;
    private String userId;
    private String nurseId;
    private String department;
    
    // Constructors
    public PatientTimelineEvent() {}
    
    public PatientTimelineEvent(String patientId, String eventType, String message) {
        this.patientId = patientId;
        this.eventType = eventType;
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
    
    public String getVitalType() { return vitalType; }
    public void setVitalType(String vitalType) { this.vitalType = vitalType; }
    
    public Object getValue() { return value; }
    public void setValue(Object value) { this.value = value; }
    
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    
    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getNurseId() { return nurseId; }
    public void setNurseId(String nurseId) { this.nurseId = nurseId; }
    
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
}
