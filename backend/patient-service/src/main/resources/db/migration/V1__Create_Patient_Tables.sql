-- MediTrack AI - Patient Service Database Schema
-- Version 1.0

-- Patients table
CREATE TABLE patients (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_identifier VARCHAR(50) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    date_of_birth DATE NOT NULL,
    gender VARCHAR(20) NOT NULL,
    blood_type VARCHAR(10),
    phone_number VARCHAR(20),
    email VARCHAR(100),
    address TEXT,
    emergency_contact_name VARCHAR(100),
    emergency_contact_phone VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE,
    
    INDEX idx_patient_identifier (patient_identifier),
    INDEX idx_name (last_name, first_name),
    INDEX idx_date_of_birth (date_of_birth)
);

-- Medical history table
CREATE TABLE medical_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    condition_name VARCHAR(200) NOT NULL,
    diagnosis_date DATE,
    diagnosis_code VARCHAR(20), -- ICD-10 code
    severity VARCHAR(20) NOT NULL, -- MILD, MODERATE, SEVERE
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, RESOLVED, CHRONIC
    notes TEXT,
    doctor_name VARCHAR(100),
    hospital_name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    INDEX idx_patient_condition (patient_id, condition_name),
    INDEX idx_diagnosis_date (diagnosis_date)
);

-- Allergies table
CREATE TABLE allergies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    allergen VARCHAR(100) NOT NULL,
    allergy_type VARCHAR(50) NOT NULL, -- FOOD, MEDICATION, ENVIRONMENTAL
    severity VARCHAR(20) NOT NULL, -- MILD, MODERATE, SEVERE
    reaction TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    INDEX idx_patient_allergen (patient_id, allergen)
);

-- Medications table
CREATE TABLE medications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    medication_name VARCHAR(200) NOT NULL,
    dosage VARCHAR(100) NOT NULL,
    frequency VARCHAR(100) NOT NULL,
    route VARCHAR(50) NOT NULL, -- ORAL, IV, TOPICAL, etc.
    start_date DATE NOT NULL,
    end_date DATE,
    prescribed_by VARCHAR(100) NOT NULL,
    purpose TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    INDEX idx_patient_medication (patient_id, is_active),
    INDEX idx_medication_name (medication_name)
);

-- FHIR Resource mapping table (for FHIR compliance)
CREATE TABLE fhir_resources (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    resource_type VARCHAR(50) NOT NULL, -- Patient, Observation, Condition, etc.
    resource_id VARCHAR(100) NOT NULL, -- FHIR resource ID
    resource_version VARCHAR(20) DEFAULT '1',
    resource_data JSON NOT NULL, -- FHIR resource JSON
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    UNIQUE KEY uk_fhir_resource (resource_type, resource_id, resource_version),
    INDEX idx_fhir_patient_resource (patient_id, resource_type)
);

-- Audit log table for compliance
CREATE TABLE patient_audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT,
    action VARCHAR(50) NOT NULL, -- CREATE, UPDATE, DELETE, VIEW
    table_name VARCHAR(50) NOT NULL,
    record_id BIGINT,
    old_data JSON,
    new_data JSON,
    performed_by VARCHAR(50) NOT NULL,
    performed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent TEXT,
    
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE SET NULL,
    INDEX idx_audit_patient_action (patient_id, action),
    INDEX idx_audit_performed_at (performed_at)
);
