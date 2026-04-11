-- MediTrack AI - Alert Service Database Schema
-- Version 1.0

-- Alerts table
CREATE TABLE alerts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    alert_type VARCHAR(50) NOT NULL, -- VITAL_CRITICAL, AI_RISK, NEWS_HIGH, DEVICE_FAILURE
    alert_title VARCHAR(200) NOT NULL,
    alert_message TEXT NOT NULL,
    alert_severity VARCHAR(20) NOT NULL, -- LOW, MEDIUM, HIGH, CRITICAL
    alert_status VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE, ACKNOWLEDGED, RESOLVED, ESCALATED
    priority_level INT NOT NULL DEFAULT 3, -- 1=Highest, 5=Lowest
    source_system VARCHAR(50) NOT NULL, -- VITALS_SERVICE, AI_PREDICTION, MANUAL
    source_reference VARCHAR(100), -- Reference to source record ID
    trigger_data JSON, -- Data that triggered the alert
    escalation_level INT DEFAULT 1, -- 1=Initial, 2=Escalated, 3=Final
    assigned_to VARCHAR(50), -- User/role assigned to handle alert
    department VARCHAR(50), -- ICU, WARD, EMERGENCY
    location VARCHAR(100),
    is_recurring BOOLEAN DEFAULT FALSE,
    recurrence_pattern VARCHAR(100),
    first_occurrence TIMESTAMP NOT NULL,
    last_occurrence TIMESTAMP NOT NULL,
    occurrence_count INT DEFAULT 1,
    acknowledged_at TIMESTAMP,
    acknowledged_by VARCHAR(50),
    acknowledged_notes TEXT,
    resolved_at TIMESTAMP,
    resolved_by VARCHAR(50),
    resolution_notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    INDEX idx_patient_alerts (patient_id, alert_status),
    INDEX idx_alert_severity_status (alert_severity, alert_status),
    INDEX idx_alert_type_timestamp (alert_type, first_occurrence),
    INDEX idx_assigned_alerts (assigned_to, alert_status),
    INDEX idx_escalation_level (escalation_level, alert_status)
);

-- Alert escalation rules table
CREATE TABLE alert_escalation_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    alert_type VARCHAR(50) NOT NULL,
    escalation_level INT NOT NULL,
    time_threshold_minutes INT NOT NULL, -- Escalate after X minutes
    target_role VARCHAR(50) NOT NULL, -- DOCTOR, NURSE, HOD, ADMIN
    target_user VARCHAR(50), -- Specific user if assigned
    notification_channels JSON, -- ["EMAIL", "SMS", "PUSH", "IN_APP"]
    escalation_message_template TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    
    UNIQUE KEY uk_alert_type_escalation (alert_type, escalation_level),
    INDEX idx_escalation_rules (alert_type, is_active)
);

-- Alert escalation history table (Saga pattern implementation)
CREATE TABLE alert_escalation_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    alert_id BIGINT NOT NULL,
    escalation_level INT NOT NULL,
    escalation_action VARCHAR(50) NOT NULL, -- ESCALATE, NOTIFY, ASSIGN, RESOLVE
    target_role VARCHAR(50),
    target_user VARCHAR(50),
    notification_channels JSON,
    notification_status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, SENT, FAILED, DELIVERED
    notification_sent_at TIMESTAMP,
    notification_response_at TIMESTAMP,
    notification_response TEXT,
    error_message TEXT,
    saga_step VARCHAR(50), -- For saga pattern tracking
    saga_status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, COMPLETED, FAILED, COMPENSATED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (alert_id) REFERENCES alerts(id) ON DELETE CASCADE,
    INDEX idx_escalation_alert (alert_id, escalation_level),
    INDEX idx_saga_status (saga_status, created_at),
    INDEX idx_notification_status (notification_status)
);

-- Alert notification templates table
CREATE TABLE alert_notification_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_name VARCHAR(100) NOT NULL UNIQUE,
    alert_type VARCHAR(50) NOT NULL,
    alert_severity VARCHAR(20) NOT NULL,
    channel_type VARCHAR(20) NOT NULL, -- EMAIL, SMS, PUSH, IN_APP
    subject_template VARCHAR(200),
    body_template TEXT NOT NULL,
    template_variables JSON, -- Available variables for template
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    
    INDEX idx_template_type_severity (alert_type, alert_severity, channel_type),
    INDEX idx_template_active (is_active, template_name)
);

-- Alert suppression rules table
CREATE TABLE alert_suppression_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_name VARCHAR(100) NOT NULL UNIQUE,
    alert_type VARCHAR(50),
    patient_id BIGINT, -- NULL for global rules
    department VARCHAR(50),
    time_window_start TIME,
    time_window_end TIME,
    suppression_reason VARCHAR(200),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    INDEX idx_suppression_active (is_active),
    INDEX idx_suppression_patient (patient_id, is_active)
);

-- Alert metrics table (for monitoring and reporting)
CREATE TABLE alert_metrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    metric_date DATE NOT NULL,
    alert_type VARCHAR(50) NOT NULL,
    alert_severity VARCHAR(20) NOT NULL,
    total_alerts INT DEFAULT 0,
    acknowledged_alerts INT DEFAULT 0,
    resolved_alerts INT DEFAULT 0,
    escalated_alerts INT DEFAULT 0,
    avg_response_time_minutes DECIMAL(10,2),
    avg_resolution_time_minutes DECIMAL(10,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_alert_metrics_date (metric_date, alert_type, alert_severity),
    INDEX idx_metrics_date (metric_date),
    INDEX idx_metrics_type (alert_type)
);
