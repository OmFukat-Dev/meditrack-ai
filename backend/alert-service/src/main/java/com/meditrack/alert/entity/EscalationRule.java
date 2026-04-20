package com.meditrack.alert.entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "escalation_rules")
public class EscalationRule {
    
    @Id
    private String id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(name = "alert_type")
    private String alertType;
    
    @Enumerated(EnumType.STRING)
    private AlertPriority priority;
    
    @Column(name = "escalation_level")
    private String escalationLevel;
    
    @Column(name = "target_role")
    private String targetRole;
    
    @ElementCollection
    @CollectionTable(name = "escalation_recipients", joinColumns = @JoinColumn(name = "rule_id"))
    @Column(name = "recipient")
    private List<String> escalationRecipients;
    
    @Column(name = "escalation_delay_minutes")
    private Integer escalationDelayMinutes;
    
    @Column(name = "time_based_escalation")
    private Boolean timeBasedEscalation;
    
    @Column(name = "condition_based_escalation")
    private Boolean conditionBasedEscalation;
    
    @Column(name = "priority_based_escalation")
    private Boolean priorityBasedEscalation;
    
    @Column(name = "escalation_condition")
    private String escalationCondition;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "min_priority_level")
    private AlertPriority minPriorityLevel;
    
    @Column(name = "escalation_message")
    private String escalationMessage;
    
    @Column(name = "active")
    private Boolean active = true;
    
    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;
    
    // Constructors
    public EscalationRule() {
        this.createdAt = java.time.LocalDateTime.now();
        this.updatedAt = java.time.LocalDateTime.now();
    }
    
    public EscalationRule(String id, String name, String escalationLevel) {
        this();
        this.id = id;
        this.name = name;
        this.escalationLevel = escalationLevel;
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }
    
    public AlertPriority getPriority() { return priority; }
    public void setPriority(AlertPriority priority) { this.priority = priority; }
    
    public String getEscalationLevel() { return escalationLevel; }
    public void setEscalationLevel(String escalationLevel) { this.escalationLevel = escalationLevel; }
    
    public String getTargetRole() { return targetRole; }
    public void setTargetRole(String targetRole) { this.targetRole = targetRole; }
    
    public List<String> getEscalationRecipients() { return escalationRecipients; }
    public void setEscalationRecipients(List<String> escalationRecipients) { this.escalationRecipients = escalationRecipients; }
    
    public Integer getEscalationDelayMinutes() { return escalationDelayMinutes; }
    public void setEscalationDelayMinutes(Integer escalationDelayMinutes) { this.escalationDelayMinutes = escalationDelayMinutes; }
    
    public Boolean getTimeBasedEscalation() { return timeBasedEscalation; }
    public void setTimeBasedEscalation(Boolean timeBasedEscalation) { this.timeBasedEscalation = timeBasedEscalation; }
    
    public Boolean getConditionBasedEscalation() { return conditionBasedEscalation; }
    public void setConditionBasedEscalation(Boolean conditionBasedEscalation) { this.conditionBasedEscalation = conditionBasedEscalation; }
    
    public Boolean getPriorityBasedEscalation() { return priorityBasedEscalation; }
    public void setPriorityBasedEscalation(Boolean priorityBasedEscalation) { this.priorityBasedEscalation = priorityBasedEscalation; }
    
    public String getEscalationCondition() { return escalationCondition; }
    public void setEscalationCondition(String escalationCondition) { this.escalationCondition = escalationCondition; }
    
    public AlertPriority getMinPriorityLevel() { return minPriorityLevel; }
    public void setMinPriorityLevel(AlertPriority minPriorityLevel) { this.minPriorityLevel = minPriorityLevel; }
    
    public String getEscalationMessage() { return escalationMessage; }
    public void setEscalationMessage(String escalationMessage) { this.escalationMessage = escalationMessage; }
    
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    
    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public java.time.LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(java.time.LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    // Enum
    public enum AlertPriority {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}
