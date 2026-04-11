-- MediTrack AI - Vitals Service Database Schema
-- Version 1.0

-- Vital readings table
CREATE TABLE vital_readings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    reading_timestamp TIMESTAMP NOT NULL,
    vital_type VARCHAR(50) NOT NULL, -- HEART_RATE, BLOOD_PRESSURE, TEMPERATURE, SPO2, RESPIRATORY_RATE
    value DECIMAL(10,2) NOT NULL,
    unit VARCHAR(20) NOT NULL, -- bpm, mmHg, °C, %, breaths/min
    systolic DECIMAL(10,2), -- For blood pressure systolic
    diastolic DECIMAL(10,2), -- For blood pressure diastolic
    source VARCHAR(50), -- DEVICE, MANUAL, SIMULATOR
    device_id VARCHAR(100),
    location VARCHAR(100), -- ICU, WARD, HOME
    quality_score DECIMAL(3,2), -- 0.00 to 1.00
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_patient_timestamp (patient_id, reading_timestamp),
    INDEX idx_vital_type_timestamp (vital_type, reading_timestamp),
    INDEX idx_device_readings (device_id, reading_timestamp)
);

-- Vital alerts thresholds table
CREATE TABLE vital_thresholds (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    vital_type VARCHAR(50) NOT NULL,
    min_normal DECIMAL(10,2),
    max_normal DECIMAL(10,2),
    min_warning DECIMAL(10,2),
    max_warning DECIMAL(10,2),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    UNIQUE KEY uk_patient_vital_threshold (patient_id, vital_type),
    INDEX idx_threshold_vital_type (vital_type)
);

-- Vital trends table (for rate-of-change calculations)
CREATE TABLE vital_trends (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    vital_type VARCHAR(50) NOT NULL,
    time_window_minutes INT NOT NULL, -- 15, 30, 60, 240 minutes
    slope DECIMAL(10,4), -- Rate of change per minute
    r_squared DECIMAL(5,4), -- Trend quality (0.0000 to 1.0000)
    last_reading_timestamp TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    UNIQUE KEY uk_patient_vital_trend (patient_id, vital_type, time_window_minutes),
    INDEX idx_trend_patient_vital (patient_id, vital_type),
    INDEX idx_trend_timestamp (last_reading_timestamp)
);

-- Latest vitals cache table (for quick access)
CREATE TABLE latest_vitals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    vital_type VARCHAR(50) NOT NULL,
    latest_value DECIMAL(10,2) NOT NULL,
    latest_unit VARCHAR(20) NOT NULL,
    latest_timestamp TIMESTAMP NOT NULL,
    systolic DECIMAL(10,2),
    diastolic DECIMAL(10,2),
    quality_score DECIMAL(3,2),
    source VARCHAR(50),
    device_id VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    UNIQUE KEY uk_patient_vital_latest (patient_id, vital_type),
    INDEX idx_latest_timestamp (latest_timestamp)
);

-- Vital reading batches (for Kafka processing)
CREATE TABLE vital_reading_batches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id VARCHAR(100) NOT NULL UNIQUE,
    patient_count INT NOT NULL,
    reading_count INT NOT NULL,
    start_timestamp TIMESTAMP NOT NULL,
    end_timestamp TIMESTAMP NOT NULL,
    processing_status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, PROCESSING, COMPLETED, FAILED
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    
    INDEX idx_batch_status (processing_status),
    INDEX idx_batch_timestamp (start_timestamp)
);

-- Vital reading batch items
CREATE TABLE vital_batch_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id VARCHAR(100) NOT NULL,
    vital_reading_id BIGINT NOT NULL,
    processing_status VARCHAR(20) DEFAULT 'PENDING',
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    
    FOREIGN KEY (vital_reading_id) REFERENCES vital_readings(id) ON DELETE CASCADE,
    INDEX idx_batch_item_status (batch_id, processing_status)
);
