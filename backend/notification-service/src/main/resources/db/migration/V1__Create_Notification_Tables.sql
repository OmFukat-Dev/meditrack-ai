-- MediTrack AI - Notification Service Database Schema
-- Version 1.0

-- Notifications table
CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_id VARCHAR(50) NOT NULL, -- User ID, email, phone number
    recipient_type VARCHAR(20) NOT NULL, -- USER, EMAIL, PHONE, ROLE
    notification_type VARCHAR(50) NOT NULL, -- ALERT, REMINDER, SYSTEM, REPORT
    channel_type VARCHAR(20) NOT NULL, -- EMAIL, SMS, PUSH, IN_APP
    subject VARCHAR(200),
    message TEXT NOT NULL,
    message_data JSON, -- Structured message data
    priority_level INT DEFAULT 3, -- 1=Highest, 5=Lowest
    delivery_status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, SENT, DELIVERED, FAILED, READ
    scheduled_at TIMESTAMP,
    sent_at TIMESTAMP,
    delivered_at TIMESTAMP,
    read_at TIMESTAMP,
    failure_reason TEXT,
    retry_count INT DEFAULT 0,
    max_retries INT DEFAULT 3,
    next_retry_at TIMESTAMP,
    source_service VARCHAR(50) NOT NULL, -- ALERT_SERVICE, AI_PREDICTION, etc.
    source_reference VARCHAR(100), -- Reference to source record
    template_used VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_recipient_status (recipient_id, delivery_status),
    INDEX idx_notification_type (notification_type, delivery_status),
    INDEX idx_scheduled_notifications (scheduled_at, delivery_status),
    INDEX idx_retry_notifications (next_retry_at, delivery_status),
    INDEX idx_source_reference (source_service, source_reference)
);

-- Notification templates table
CREATE TABLE notification_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_name VARCHAR(100) NOT NULL UNIQUE,
    template_type VARCHAR(50) NOT NULL, -- ALERT, REMINDER, SYSTEM, REPORT
    channel_type VARCHAR(20) NOT NULL, -- EMAIL, SMS, PUSH, IN_APP
    subject_template VARCHAR(200),
    body_template TEXT NOT NULL,
    template_variables JSON, -- Available variables for template
    css_styles TEXT, -- For email templates
    is_html BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    
    INDEX idx_template_type_channel (template_type, channel_type),
    INDEX idx_template_active (is_active, template_name)
);

-- Notification preferences table
CREATE TABLE notification_preferences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    channel_type VARCHAR(20) NOT NULL,
    is_enabled BOOLEAN DEFAULT TRUE,
    quiet_hours_start TIME,
    quiet_hours_end TIME,
    max_notifications_per_hour INT DEFAULT 10,
    max_notifications_per_day INT DEFAULT 100,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_user_notification_channel (user_id, notification_type, channel_type),
    INDEX idx_user_preferences (user_id, is_enabled)
);

-- Notification delivery logs table
CREATE TABLE notification_delivery_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    notification_id BIGINT NOT NULL,
    delivery_attempt INT NOT NULL,
    channel_type VARCHAR(20) NOT NULL,
    provider VARCHAR(50) NOT NULL, -- SMTP, TWILIO, FCM, etc.
    provider_response_code VARCHAR(20),
    provider_response_message TEXT,
    delivery_status VARCHAR(20) NOT NULL, -- ATTEMPTED, SENT, FAILED, DELIVERED
    delivery_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    error_details TEXT,
    metadata JSON, -- Additional delivery metadata
    
    FOREIGN KEY (notification_id) REFERENCES notifications(id) ON DELETE CASCADE,
    INDEX idx_delivery_notification (notification_id, delivery_attempt),
    INDEX idx_delivery_status_time (delivery_status, delivery_time),
    INDEX idx_delivery_provider (provider, delivery_status)
);

-- Email configurations table
CREATE TABLE email_configurations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_name VARCHAR(100) NOT NULL UNIQUE,
    smtp_host VARCHAR(255) NOT NULL,
    smtp_port INT NOT NULL,
    smtp_username VARCHAR(255),
    smtp_password_encrypted TEXT,
    smtp_use_ssl BOOLEAN DEFAULT TRUE,
    smtp_use_tls BOOLEAN DEFAULT TRUE,
    from_email VARCHAR(255) NOT NULL,
    from_name VARCHAR(100),
    reply_to_email VARCHAR(255),
    is_default BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    
    INDEX idx_email_config_active (is_active),
    INDEX idx_email_config_default (is_default)
);

-- SMS configurations table
CREATE TABLE sms_configurations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_name VARCHAR(100) NOT NULL UNIQUE,
    provider VARCHAR(50) NOT NULL, -- TWILIO, AWS_SNS, etc.
    api_key_encrypted TEXT,
    api_secret_encrypted TEXT,
    from_number VARCHAR(20),
    webhook_url VARCHAR(255),
    is_default BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    
    INDEX idx_sms_config_active (is_active),
    INDEX idx_sms_config_default (is_default)
);

-- Push notification configurations table
CREATE TABLE push_configurations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_name VARCHAR(100) NOT NULL UNIQUE,
    provider VARCHAR(50) NOT NULL, -- FCM, APNS, etc.
    api_key_encrypted TEXT,
    certificate_path VARCHAR(255),
    bundle_id VARCHAR(100), -- For APNS
    team_id VARCHAR(50), -- For APNS
    is_default BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    
    INDEX idx_push_config_active (is_active),
    INDEX idx_push_config_default (is_default)
);

-- Notification metrics table
CREATE TABLE notification_metrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    metric_date DATE NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    channel_type VARCHAR(20) NOT NULL,
    total_sent INT DEFAULT 0,
    total_delivered INT DEFAULT 0,
    total_failed INT DEFAULT 0,
    total_read INT DEFAULT 0,
    avg_delivery_time_seconds DECIMAL(10,2),
    delivery_rate DECIMAL(5,2), -- Percentage
    read_rate DECIMAL(5,2), -- Percentage
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_notification_metrics_date (metric_date, notification_type, channel_type),
    INDEX idx_metrics_date (metric_date),
    INDEX idx_metrics_type (notification_type)
);
