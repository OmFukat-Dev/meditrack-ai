package com.meditrack.alert.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alerts")
public class Alert {
    
    @Id
    private String id;
    
    @Column(nullable = false)
    private String alertType;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertPriority priority;
    
    @Column(nullable = false)
    private String message;
    
    @Column(name = "patient_id")
    private String patientId;
    
    @Column(name = "vital_type")
    private String vitalType;
    
    @Column(name = "vital_value")
    private Double vitalValue;
    
    @Column(name = "threshold_value")
    private Double thresholdValue;
    
    @Column(name = "created_by")
    private String createdBy;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private AlertStatus status;
    
    @Column(name = "escalation_level")
    private String escalationLevel;
    
    @Column(name = "escalated_at")
    private LocalDateTime escalatedAt;
    
    // Constructors
    public Alert() {
        this.createdAt = LocalDateTime.now();
        this.status = AlertStatus.PENDING;
    }
    
    public Alert(String id, String alertType, AlertPriority priority, String message) {
        this();
        this.id = id;
        this.alertType = alertType;
        this.priority = priority;
        this.message = message;
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }
    
    public AlertPriority getPriority() { return priority; }
    public void setPriority(AlertPriority priority) { this.priority = priority; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    
    public String getVitalType() { return vitalType; }
    public void setVitalType(String vitalType) { this.vitalType = vitalType; }
    
    public Double getVitalValue() { return vitalValue; }
    public void setVitalValue(Double vitalValue) { this.vitalValue = vitalValue; }
    
    public Double getThresholdValue() { return thresholdValue; }
    public void setThresholdValue(Double thresholdValue) { this.thresholdValue = thresholdValue; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
    
    public AlertStatus getStatus() { return status; }
    public void setStatus(AlertStatus status) { this.status = status; }
    
    public String getEscalationLevel() { return escalationLevel; }
    public void setEscalationLevel(String escalationLevel) { this.escalationLevel = escalationLevel; }
    
    public LocalDateTime getEscalatedAt() { return escalatedAt; }
    public void setEscalatedAt(LocalDateTime escalatedAt) { this.escalatedAt = escalatedAt; }
    
    // Enums
    public enum AlertPriority {
        LOW, MEDIUM, HIGH, CRITICAL
    }
    
    public enum AlertStatus {
        PENDING, PROCESSING, PROCESSED, ESCALATED, RESOLVED, FAILED
    }
}
