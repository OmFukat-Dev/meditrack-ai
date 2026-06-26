package com.meditrack.vitals.dto;

import java.time.LocalDateTime;
import java.util.List;

public class VitalReadingEvent {
    private String eventId;
    private String eventType;
    private LocalDateTime timestamp;
    private String patientId;
    private String vitalType;
    private Object value;
    private String unit;
    private String systolic;
    private String diastolic;
    private String nurseId;
    private String department;
    private String createdBy;
    private String role;
    private String severity;
    private String message;
    private String confidence;
    private String riskLevel;
    private Integer newsScore;
    private List<String> recommendations;
    
    // Constructors
    public VitalReadingEvent() {}
    
    public VitalReadingEvent(String patientId, String vitalType, Object value) {
        this.patientId = patientId;
        this.vitalType = vitalType;
        this.value = value;
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
    
    public String getSystolic() { return systolic; }
    public void setSystolic(String systolic) { this.systolic = systolic; }
    
    public String getDiastolic() { return diastolic; }
    public void setDiastolic(String diastolic) { this.diastolic = diastolic; }
    
    public String getNurseId() { return nurseId; }
    public void setNurseId(String nurseId) { this.nurseId = nurseId; }
    
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }
    
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    
    public Integer getNewsScore() { return newsScore; }
    public void setNewsScore(Integer newsScore) { this.newsScore = newsScore; }
    
    public List<String> getRecommendations() { return recommendations; }
    public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
}
