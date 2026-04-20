package com.meditrack.alert.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {
    
    @Id
    private String id;
    
    @Column(nullable = false)
    private String recipient;
    
    @Column(nullable = false)
    private String message;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType notificationType;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationPriority priority;
    
    @Column(name = "sent_at")
    private LocalDateTime sentAt;
    
    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;
    
    @Column(name = "read_at")
    private LocalDateTime readAt;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private NotificationStatus status;
    
    @Column(name = "escalation_level")
    private String escalationLevel;
    
    @Column(name = "recall")
    private Boolean recall = false;
    
    @Column(name = "recall_at")
    private LocalDateTime recallAt;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    // Constructors
    public Notification() {
        this.createdAt = LocalDateTime.now();
        this.status = NotificationStatus.PENDING;
        this.recall = false;
    }
    
    public Notification(String id, String recipient, String message, NotificationType notificationType) {
        this();
        this.id = id;
        this.recipient = recipient;
        this.message = message;
        this.notificationType = notificationType;
        this.priority = NotificationPriority.MEDIUM;
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public NotificationType getNotificationType() { return notificationType; }
    public void setNotificationType(NotificationType notificationType) { this.notificationType = notificationType; }
    
    public NotificationPriority getPriority() { return priority; }
    public void setPriority(NotificationPriority priority) { this.priority = priority; }
    
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
    
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }
    
    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
    
    public NotificationStatus getStatus() { return status; }
    public void setStatus(NotificationStatus status) { this.status = status; }
    
    public String getEscalationLevel() { return escalationLevel; }
    public void setEscalationLevel(String escalationLevel) { this.escalationLevel = escalationLevel; }
    
    public Boolean getRecall() { return recall; }
    public void setRecall(Boolean recall) { this.recall = recall; }
    
    public LocalDateTime getRecallAt() { return recallAt; }
    public void setRecallAt(LocalDateTime recallAt) { this.recallAt = recallAt; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    // Enums
    public enum NotificationType {
        EMAIL, SMS, PUSH, WEBHOOK
    }
    
    public enum NotificationPriority {
        LOW, MEDIUM, HIGH, CRITICAL
    }
    
    public enum NotificationStatus {
        PENDING, SENT, FAILED, DELIVERED, READ, RECALLED
    }
}
