package com.meditrack.alert.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
    
    @Id
    private String id;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private AuditEventType eventType;
    
    @Column(name = "entity_id")
    private String entityId;
    
    @Column(name = "entity_type")
    private String entityType;
    
    @Column(name = "user_id")
    private String userId;
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @Column(nullable = false)
    private String description;
    
    @Column
    private String severity;
    
    @ElementCollection
    @CollectionTable(name = "audit_log_details", joinColumns = @JoinColumn(name = "audit_log_id"))
    @MapKeyColumn(name = "detail_key")
    @Column(name = "detail_value")
    private Map<String, Object> details;
    
    // Constructors
    public AuditLog() {
        this.timestamp = LocalDateTime.now();
    }
    
    public AuditLog(String id, AuditEventType eventType, String description) {
        this();
        this.id = id;
        this.eventType = eventType;
        this.description = description;
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public AuditEventType getEventType() { return eventType; }
    public void setEventType(AuditEventType eventType) { this.eventType = eventType; }
    
    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }
    
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    
    public Map<String, Object> getDetails() { return details; }
    public void setDetails(Map<String, Object> details) { this.details = details; }
    
    // Enum
    public enum AuditEventType {
        ALERT_CREATED,
        ALERT_PROCESSED,
        ALERT_PROCESSING_FAILED,
        NOTIFICATION_SENT,
        NOTIFICATION_FAILED,
        NOTIFICATION_RECALLED,
        NOTIFICATION_RECALL_FAILED,
        ESCALATION_EXECUTED,
        ESCALATION_FAILED,
        ESCALATION_RECALLED,
        ESCALATION_RECALL_FAILED,
        SAGA_COMPLETED,
        SAGA_FAILED,
        USER_ACTION
    }
}
