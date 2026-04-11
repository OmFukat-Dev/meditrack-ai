-- MediTrack AI - Report Service Database Schema
-- Version 1.0

-- Report requests table
CREATE TABLE report_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT,
    report_type VARCHAR(50) NOT NULL, -- DISCHARGE_SUMMARY, VITAL_TRENDS, AI_ANALYSIS, COMPREHENSIVE
    report_title VARCHAR(200) NOT NULL,
    report_description TEXT,
    requested_by VARCHAR(50) NOT NULL,
    request_date TIMESTAMP NOT NULL,
    start_date DATE, -- For time-based reports
    end_date DATE, -- For time-based reports
    report_parameters JSON, -- Additional report parameters
    generation_status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, GENERATING, COMPLETED, FAILED
    file_path VARCHAR(500),
    file_name VARCHAR(255),
    file_size_bytes BIGINT,
    file_format VARCHAR(10) DEFAULT 'PDF', -- PDF, HTML, CSV
    download_count INT DEFAULT 0,
    expires_at TIMESTAMP,
    generated_at TIMESTAMP,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    INDEX idx_report_patient (patient_id, generation_status),
    INDEX idx_report_status (generation_status, request_date),
    INDEX idx_report_requested_by (requested_by, request_date),
    INDEX idx_report_expires (expires_at)
);

-- Report templates table
CREATE TABLE report_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_name VARCHAR(100) NOT NULL UNIQUE,
    template_type VARCHAR(50) NOT NULL, -- DISCHARGE_SUMMARY, VITAL_TRENDS, AI_ANALYSIS, COMPREHENSIVE
    template_description TEXT,
    html_template TEXT NOT NULL,
    css_styles TEXT,
    template_variables JSON, -- Available variables for template
    is_default BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    version VARCHAR(20) DEFAULT '1.0',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    
    INDEX idx_template_type (template_type, is_active),
    INDEX idx_template_default (is_default, template_type)
);

-- Report sections table (for modular report building)
CREATE TABLE report_sections (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    section_name VARCHAR(100) NOT NULL,
    section_type VARCHAR(50) NOT NULL, -- PATIENT_INFO, VITAL_CHARTS, AI_PREDICTIONS, ALERTS
    section_order INT NOT NULL,
    html_template TEXT NOT NULL,
    css_classes VARCHAR(200),
    data_source VARCHAR(100), -- API endpoint or data source
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_section_order (section_order, is_active),
    INDEX idx_section_type (section_type, is_active)
);

-- Report generation logs table
CREATE TABLE report_generation_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_request_id BIGINT NOT NULL,
    generation_step VARCHAR(50) NOT NULL, -- DATA_COLLECTION, TEMPLATE_PROCESSING, PDF_GENERATION
    step_status VARCHAR(20) NOT NULL, -- STARTED, COMPLETED, FAILED
    step_start_time TIMESTAMP NOT NULL,
    step_end_time TIMESTAMP,
    step_duration_ms BIGINT,
    input_data JSON,
    output_data JSON,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (report_request_id) REFERENCES report_requests(id) ON DELETE CASCADE,
    INDEX idx_generation_report (report_request_id, generation_step),
    INDEX idx_generation_status (step_status, step_start_time),
    INDEX idx_generation_duration (step_duration_ms)
);

-- Scheduled reports table
CREATE TABLE scheduled_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_name VARCHAR(100) NOT NULL,
    report_type VARCHAR(50) NOT NULL,
    patient_id BIGINT, -- NULL for system-wide reports
    recipient_email VARCHAR(255) NOT NULL,
    schedule_type VARCHAR(20) NOT NULL, -- DAILY, WEEKLY, MONTHLY
    schedule_cron VARCHAR(100), -- Cron expression for complex schedules
    schedule_time TIME,
    schedule_day_of_week INT, -- 1-7 (Monday-Sunday)
    schedule_day_of_month INT, -- 1-31
    report_parameters JSON,
    is_active BOOLEAN DEFAULT TRUE,
    last_generated_at TIMESTAMP,
    next_generation_at TIMESTAMP,
    generation_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    INDEX idx_scheduled_active (is_active, next_generation_at),
    INDEX idx_scheduled_patient (patient_id, is_active),
    INDEX idx_scheduled_type (schedule_type, is_active)
);

-- Report access logs table (for audit and compliance)
CREATE TABLE report_access_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_request_id BIGINT NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    access_type VARCHAR(20) NOT NULL, -- VIEW, DOWNLOAD, EMAIL, PRINT
    access_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent TEXT,
    access_granted BOOLEAN NOT NULL,
    denial_reason VARCHAR(200),
    
    FOREIGN KEY (report_request_id) REFERENCES report_requests(id) ON DELETE CASCADE,
    INDEX idx_access_report_user (report_request_id, user_id),
    INDEX idx_access_timestamp (access_timestamp),
    INDEX idx_access_type (access_type, access_granted)
);

-- Report metrics table
CREATE TABLE report_metrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    metric_date DATE NOT NULL,
    report_type VARCHAR(50) NOT NULL,
    total_requests INT DEFAULT 0,
    completed_reports INT DEFAULT 0,
    failed_reports INT DEFAULT 0,
    avg_generation_time_seconds DECIMAL(10,2),
    avg_file_size_bytes BIGINT,
    total_downloads INT DEFAULT 0,
    unique_users INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_report_metrics_date (metric_date, report_type),
    INDEX idx_metrics_date (metric_date),
    INDEX idx_metrics_type (report_type)
);
