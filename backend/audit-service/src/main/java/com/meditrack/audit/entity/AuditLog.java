package com.meditrack.audit.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_patient_id", columnList = "patient_id"),
    @Index(name = "idx_action", columnList = "action"),
    @Index(name = "idx_entity_type", columnList = "entity_type"),
    @Index(name = "idx_timestamp", columnList = "timestamp")
})
public class AuditLog {
    
    @Id
    private String id;
    
    @Column(name = "user_id")
    private String userId;
    
    @Column(name = "username")
    private String username;
    
    @Column(name = "action", nullable = false)
    private String action;
    
    @Column(name = "entity_type")
    private String entityType;
    
    @Column(name = "patient_id")
    private String patientId;
    
    @Column(name = "details", columnDefinition = "TEXT")
    private String details;
    
    @Column(name = "ip_address")
    private String ipAddress;
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    // Constructors
    public AuditLog() {
        this.timestamp = LocalDateTime.now();
    }
    
    public AuditLog(String id, String userId, String username, String action) {
        this();
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.action = action;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
