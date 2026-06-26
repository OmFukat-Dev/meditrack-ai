package com.meditrack.notification.entity;

import com.meditrack.notification.domain.NotificationChannel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_delivery_logs")
public class NotificationDeliveryLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "notification_id", nullable = false)
    private Long notificationId;

    @Column(name = "delivery_attempt", nullable = false)
    private Integer deliveryAttempt;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false, length = 20)
    private NotificationChannel channelType;

    @Column(nullable = false, length = 50)
    private String provider;

    @Column(name = "provider_response_code", length = 20)
    private String providerResponseCode;

    @Column(name = "provider_response_message", columnDefinition = "TEXT")
    private String providerResponseMessage;

    @Column(name = "delivery_status", nullable = false, length = 20)
    private String deliveryStatus;

    @Column(name = "delivery_time")
    private LocalDateTime deliveryTime;

    @Column(name = "error_details", columnDefinition = "TEXT")
    private String errorDetails;

    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadataJson;

    @PrePersist
    public void prePersist() {
        if (deliveryTime == null) {
            deliveryTime = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Long notificationId) {
        this.notificationId = notificationId;
    }

    public Integer getDeliveryAttempt() {
        return deliveryAttempt;
    }

    public void setDeliveryAttempt(Integer deliveryAttempt) {
        this.deliveryAttempt = deliveryAttempt;
    }

    public NotificationChannel getChannelType() {
        return channelType;
    }

    public void setChannelType(NotificationChannel channelType) {
        this.channelType = channelType;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getProviderResponseCode() {
        return providerResponseCode;
    }

    public void setProviderResponseCode(String providerResponseCode) {
        this.providerResponseCode = providerResponseCode;
    }

    public String getProviderResponseMessage() {
        return providerResponseMessage;
    }

    public void setProviderResponseMessage(String providerResponseMessage) {
        this.providerResponseMessage = providerResponseMessage;
    }

    public String getDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(String deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    public LocalDateTime getDeliveryTime() {
        return deliveryTime;
    }

    public void setDeliveryTime(LocalDateTime deliveryTime) {
        this.deliveryTime = deliveryTime;
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    public void setErrorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }
}
