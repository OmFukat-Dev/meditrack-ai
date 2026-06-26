-- =====================================================
-- PATIENT MANAGEMENT DATABASE SCHEMA
-- =====================================================
-- Database: meditrack_patient_management
-- Purpose: Store all patient-related data and medical records

-- Create database
CREATE DATABASE IF NOT EXISTS meditrack_patient_management 
CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE meditrack_patient_management;

-- =====================================================
-- PATIENTS TABLE
-- =====================================================
CREATE TABLE patients (
    id VARCHAR(50) PRIMARY KEY, -- UUID or custom ID like 'patient-1', 'patient-2', etc.
    patient_identifier VARCHAR(20) NOT NULL UNIQUE, -- PT-001, PT-002, etc.
    email VARCHAR(255) UNIQUE,
    password_hash VARCHAR(255),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    date_of_birth DATE,
    gender ENUM('MALE', 'FEMALE', 'OTHER'),
    phone VARCHAR(20),
    mobile_number VARCHAR(20),
    guardian_name VARCHAR(100),
    guardian_mobile VARCHAR(20),
    guardian_relationship VARCHAR(50),
    address TEXT,
    city VARCHAR(100),
    state VARCHAR(100),
    country VARCHAR(100),
    postal_code VARCHAR(20),
    emergency_contact_name VARCHAR(100),
    emergency_contact_phone VARCHAR(20),
    blood_group ENUM('A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'),
    allergies JSON, -- Store allergies as JSON array
    medical_conditions JSON, -- Store chronic conditions as JSON array
    insurance_provider VARCHAR(100),
    insurance_policy_number VARCHAR(100),
    room_number VARCHAR(20),
    bed_number VARCHAR(20),
    admission_date TIMESTAMP,
    discharge_date TIMESTAMP NULL,
    condition_status ENUM('STABLE', 'CRITICAL', 'EMERGENCY', 'RECOVERING', 'DISCHARGED') DEFAULT 'STABLE',
    doctor_id VARCHAR(50) NOT NULL, -- Foreign key reference to User Management DB
    primary_nurse_id VARCHAR(50), -- Foreign key reference to User Management DB
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_patient_identifier (patient_identifier),
    INDEX idx_email (email),
    INDEX idx_name (first_name, last_name),
    INDEX idx_doctor (doctor_id),
    INDEX idx_nurse (primary_nurse_id),
    INDEX idx_admission_date (admission_date),
    INDEX idx_condition (condition_status),
    INDEX idx_active (is_active),
    INDEX idx_room (room_number)
);

-- =====================================================
-- DOCTOR_PATIENT_ASSIGNMENT TABLE
-- =====================================================
CREATE TABLE doctor_patient_assignment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    doctor_id VARCHAR(50) NOT NULL, -- Foreign key reference to User Management DB
    patient_id VARCHAR(50) NOT NULL, -- Foreign key reference to patients table
    assignment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reassignment_date TIMESTAMP NULL,
    assignment_status ENUM('ACTIVE', 'TRANSFERRED', 'DISCHARGED') DEFAULT 'ACTIVE',
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    UNIQUE KEY unique_doctor_patient (doctor_id, patient_id, assignment_status),
    INDEX idx_doctor_assignments (doctor_id, assignment_status),
    INDEX idx_patient_assignments (patient_id),
    INDEX idx_assignment_date (assignment_date)
);

-- =====================================================
-- NURSE_PATIENT_ASSIGNMENT TABLE
-- =====================================================
CREATE TABLE nurse_patient_assignment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nurse_id VARCHAR(50) NOT NULL, -- Foreign key reference to User Management DB
    patient_id VARCHAR(50) NOT NULL, -- Foreign key reference to patients table
    assignment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reassignment_date TIMESTAMP NULL,
    shift_type ENUM('DAY', 'NIGHT', 'ROTATING') DEFAULT 'DAY',
    assignment_status ENUM('ACTIVE', 'TRANSFERRED', 'DISCHARGED') DEFAULT 'ACTIVE',
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    UNIQUE KEY unique_nurse_patient (nurse_id, patient_id, assignment_status),
    INDEX idx_nurse_assignments (nurse_id, assignment_status),
    INDEX idx_patient_nurse_assignments (patient_id),
    INDEX idx_shift (shift_type)
);

-- =====================================================
-- VITAL SIGNS TABLE
-- =====================================================
CREATE TABLE vital_signs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id VARCHAR(50) NOT NULL, -- Foreign key reference to patients table
    recorded_by VARCHAR(50) NOT NULL, -- Foreign key reference to User Management DB (doctor or nurse)
    heart_rate INT,
    blood_pressure_systolic INT,
    blood_pressure_diastolic INT,
    temperature DECIMAL(4,1),
    oxygen_saturation DECIMAL(5,2),
    respiratory_rate INT,
    blood_sugar DECIMAL(5,2),
    weight DECIMAL(5,2),
    height DECIMAL(5,2),
    bmi DECIMAL(4,1),
    notes TEXT,
    measurement_location VARCHAR(50), -- 'ICU', 'Ward', 'Emergency', etc.
    device_id VARCHAR(50), -- For IoT integration
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    INDEX idx_patient_vitals (patient_id, recorded_at),
    INDEX idx_recorded_by (recorded_by),
    INDEX idx_recorded_at (recorded_at),
    INDEX idx_device (device_id)
);

-- =====================================================
-- ALERTS TABLE
-- =====================================================
CREATE TABLE alerts (
    id VARCHAR(50) PRIMARY KEY, -- UUID or custom ID like 'alert-1', 'alert-2', etc.
    patient_id VARCHAR(50) NOT NULL, -- Foreign key reference to patients table
    alert_type ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') NOT NULL,
    alert_category ENUM('VITAL_SIGNS', 'MEDICATION', 'APPOINTMENT', 'SYSTEM', 'EMERGENCY') NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    alert_source ENUM('AI_PREDICTION', 'MANUAL', 'SYSTEM', 'DEVICE') DEFAULT 'SYSTEM',
    priority_score INT DEFAULT 1, -- 1-10 scale for AI prioritization
    assigned_to VARCHAR(50), -- Foreign key reference to User Management DB
    status ENUM('ACTIVE', 'ACKNOWLEDGED', 'RESOLVED', 'ESCALATED', 'FALSE_POSITIVE') DEFAULT 'ACTIVE',
    resolution_notes TEXT,
    resolved_by VARCHAR(50), -- Foreign key reference to User Management DB
    resolved_at TIMESTAMP NULL,
    acknowledged_at TIMESTAMP NULL,
    acknowledged_by VARCHAR(50), -- Foreign key reference to User Management DB
    escalation_level INT DEFAULT 1,
    auto_generated BOOLEAN DEFAULT FALSE,
    kafka_message_id VARCHAR(100), -- Track Kafka message ID
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    INDEX idx_patient_alerts (patient_id, status),
    INDEX idx_alert_type (alert_type, status),
    INDEX idx_assigned_to (assigned_to),
    INDEX idx_priority (priority_score),
    INDEX idx_created (created_at),
    INDEX idx_status (status)
);

-- =====================================================
-- MEDICAL RECORDS TABLE
-- =====================================================
CREATE TABLE medical_records (
    id VARCHAR(50) PRIMARY KEY, -- UUID or custom ID like 'record-1', 'record-2', etc.
    patient_id VARCHAR(50) NOT NULL, -- Foreign key reference to patients table
    doctor_id VARCHAR(50) NOT NULL, -- Foreign key reference to User Management DB
    record_type ENUM('CONSULTATION', 'DIAGNOSIS', 'PRESCRIPTION', 'LAB_RESULT', 'IMAGING', 'SURGERY', 'DISCHARGE_SUMMARY') NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    diagnosis TEXT,
    prescription JSON, -- Store medications as JSON array
    lab_results JSON, -- Store lab results as JSON object
    imaging_results JSON, -- Store imaging reports as JSON object
    treatment_plan TEXT,
    follow_up_date DATE,
    attachments JSON, -- Store file paths as JSON array
    is_confidential BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    INDEX idx_patient_records (patient_id, record_type),
    INDEX idx_doctor_records (doctor_id),
    INDEX idx_record_date (created_at),
    INDEX idx_follow_up (follow_up_date),
    INDEX idx_confidential (is_confidential)
);

-- =====================================================
-- MEDICATIONS TABLE
-- =====================================================
CREATE TABLE medications (
    id VARCHAR(50) PRIMARY KEY,
    patient_id VARCHAR(50) NOT NULL, -- Foreign key reference to patients table
    prescribed_by VARCHAR(50) NOT NULL, -- Foreign key reference to User Management DB
    medication_name VARCHAR(200) NOT NULL,
    dosage VARCHAR(100) NOT NULL,
    frequency VARCHAR(100) NOT NULL,
    route ENUM('ORAL', 'INTRAVENOUS', 'INTRAMUSCULAR', 'TOPICAL', 'INHALATION') NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NULL,
    is_active BOOLEAN DEFAULT TRUE,
    instructions TEXT,
    side_effects TEXT,
    contraindications TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    INDEX idx_patient_medications (patient_id, is_active),
    INDEX idx_prescribed_by (prescribed_by),
    INDEX idx_medication_dates (start_date, end_date)
);

-- =====================================================
-- APPOINTMENTS TABLE
-- =====================================================
CREATE TABLE appointments (
    id VARCHAR(50) PRIMARY KEY,
    patient_id VARCHAR(50) NOT NULL, -- Foreign key reference to patients table
    doctor_id VARCHAR(50) NOT NULL, -- Foreign key reference to User Management DB
    appointment_type ENUM('CONSULTATION', 'FOLLOW_UP', 'EMERGENCY', 'SURGERY', 'LAB_TEST', 'IMAGING') NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    appointment_date TIMESTAMP NOT NULL,
    duration_minutes INT DEFAULT 30,
    status ENUM('SCHEDULED', 'CONFIRMED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'NO_SHOW') DEFAULT 'SCHEDULED',
    location VARCHAR(100),
    notes TEXT,
    reminder_sent BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    INDEX idx_patient_appointments (patient_id, appointment_date),
    INDEX idx_doctor_appointments (doctor_id, appointment_date),
    INDEX idx_appointment_date (appointment_date),
    INDEX idx_status (status)
);

-- =====================================================
-- TRIGGERS FOR ALERT GENERATION
-- =====================================================
DELIMITER //

CREATE TRIGGER check_vital_signs_alert
AFTER INSERT ON vital_signs
FOR EACH ROW
BEGIN
    DECLARE alert_count INT DEFAULT 0;
    
    -- Check for critical heart rate
    IF NEW.heart_rate > 120 OR NEW.heart_rate < 50 THEN
        SELECT COUNT(*) INTO alert_count 
        FROM alerts 
        WHERE patient_id = NEW.patient_id 
        AND alert_category = 'VITAL_SIGNS'
        AND status = 'ACTIVE'
        AND created_at > DATE_SUB(NOW(), INTERVAL 1 HOUR);
        
        IF alert_count = 0 THEN
            INSERT INTO alerts (id, patient_id, alert_type, alert_category, title, message, alert_source, priority_score, created_at)
            VALUES (
                CONCAT('alert-', UUID()),
                NEW.patient_id,
                'HIGH',
                'VITAL_SIGNS',
                'Heart Rate Alert',
                CONCAT('Heart rate is ', NEW.heart_rate, ' BPM - outside normal range'),
                'SYSTEM',
                7,
                NOW()
            );
        END IF;
    END IF;
    
    -- Check for critical blood pressure
    IF NEW.blood_pressure_systolic > 140 OR NEW.blood_pressure_diastolic > 90 THEN
        SELECT COUNT(*) INTO alert_count 
        FROM alerts 
        WHERE patient_id = NEW.patient_id 
        AND alert_category = 'VITAL_SIGNS'
        AND status = 'ACTIVE'
        AND created_at > DATE_SUB(NOW(), INTERVAL 1 HOUR);
        
        IF alert_count = 0 THEN
            INSERT INTO alerts (id, patient_id, alert_type, alert_category, title, message, alert_source, priority_score, created_at)
            VALUES (
                CONCAT('alert-', UUID()),
                NEW.patient_id,
                'HIGH',
                'VITAL_SIGNS',
                'Blood Pressure Alert',
                CONCAT('Blood pressure is ', NEW.blood_pressure_systolic, '/', NEW.blood_pressure_diastolic, ' mmHg - elevated'),
                'SYSTEM',
                6,
                NOW()
            );
        END IF;
    END IF;
    
    -- Check for critical temperature
    IF NEW.temperature > 102 OR NEW.temperature < 95 THEN
        SELECT COUNT(*) INTO alert_count 
        FROM alerts 
        WHERE patient_id = NEW.patient_id 
        AND alert_category = 'VITAL_SIGNS'
        AND status = 'ACTIVE'
        AND created_at > DATE_SUB(NOW(), INTERVAL 1 HOUR);
        
        IF alert_count = 0 THEN
            INSERT INTO alerts (id, patient_id, alert_type, alert_category, title, message, alert_source, priority_score, created_at)
            VALUES (
                CONCAT('alert-', UUID()),
                NEW.patient_id,
                'MEDIUM',
                'VITAL_SIGNS',
                'Temperature Alert',
                CONCAT('Temperature is ', NEW.temperature, '°F - outside normal range'),
                'SYSTEM',
                5,
                NOW()
            );
        END IF;
    END IF;
    
    -- Check for low oxygen saturation
    IF NEW.oxygen_saturation < 90 THEN
        SELECT COUNT(*) INTO alert_count 
        FROM alerts 
        WHERE patient_id = NEW.patient_id 
        AND alert_category = 'VITAL_SIGNS'
        AND status = 'ACTIVE'
        AND created_at > DATE_SUB(NOW(), INTERVAL 30 MINUTE);
        
        IF alert_count = 0 THEN
            INSERT INTO alerts (id, patient_id, alert_type, alert_category, title, message, alert_source, priority_score, created_at)
            VALUES (
                CONCAT('alert-', UUID()),
                NEW.patient_id,
                'CRITICAL',
                'VITAL_SIGNS',
                'Oxygen Saturation Alert',
                CONCAT('Oxygen saturation is ', NEW.oxygen_saturation, '% - critically low'),
                'SYSTEM',
                9,
                NOW()
            );
        END IF;
    END IF;
END//

DELIMITER ;
