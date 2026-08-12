-- MediTrack AI - Alert Service Database Schema
-- Version 1.0

-- Alerts table
CREATE TABLE alerts (
    id VARCHAR(255) PRIMARY KEY,
    patient_id VARCHAR(255),
    alert_type VARCHAR(255) NOT NULL,
    priority VARCHAR(50) NOT NULL,
    message VARCHAR(255) NOT NULL,
    department VARCHAR(255),
    vital_type VARCHAR(255),
    vital_value DOUBLE,
    threshold_value DOUBLE,
    created_by VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP NULL,
    status VARCHAR(50),
    escalation_level VARCHAR(255),
    escalated_at TIMESTAMP NULL
);

-- Escalation rules table
CREATE TABLE escalation_rules (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    alert_type VARCHAR(255),
    priority VARCHAR(50),
    escalation_level VARCHAR(255),
    target_role VARCHAR(255),
    escalation_delay_minutes INT,
    time_based_escalation BOOLEAN,
    condition_based_escalation BOOLEAN,
    priority_based_escalation BOOLEAN,
    escalation_condition VARCHAR(255),
    min_priority_level VARCHAR(50),
    escalation_message VARCHAR(255),
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- Escalation recipients table
CREATE TABLE escalation_recipients (
    rule_id VARCHAR(255) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    FOREIGN KEY (rule_id) REFERENCES escalation_rules(id) ON DELETE CASCADE
);

-- Audit logs table
CREATE TABLE audit_logs (
    id VARCHAR(255) PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(255),
    entity_type VARCHAR(255),
    user_id VARCHAR(255),
    timestamp TIMESTAMP NOT NULL,
    description VARCHAR(255) NOT NULL,
    severity VARCHAR(50)
);

-- Audit log details table
CREATE TABLE audit_log_details (
    audit_log_id VARCHAR(255) NOT NULL,
    detail_key VARCHAR(255) NOT NULL,
    detail_value BLOB,
    FOREIGN KEY (audit_log_id) REFERENCES audit_logs(id) ON DELETE CASCADE
);
