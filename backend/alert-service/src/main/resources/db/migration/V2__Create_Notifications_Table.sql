-- MediTrack AI - Alert Service Notifications table

CREATE TABLE notifications (
    id VARCHAR(255) PRIMARY KEY,
    recipient VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    notification_type VARCHAR(20) NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    sent_at TIMESTAMP NULL,
    delivered_at TIMESTAMP NULL,
    read_at TIMESTAMP NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    escalation_level VARCHAR(50) NULL,
    recall BOOLEAN DEFAULT FALSE,
    recall_at TIMESTAMP NULL,
    provider_reference TEXT,
    failure_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_notification_status (status),
    INDEX idx_notification_created_at (created_at),
    INDEX idx_notification_recipient (recipient, status)
);
