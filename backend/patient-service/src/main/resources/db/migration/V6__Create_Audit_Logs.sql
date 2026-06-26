-- MediTrack AI - Audit logs table required by the patient service

CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    details VARCHAR(255),
    assigned_by VARCHAR(100) NOT NULL,
    `timestamp` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_audit_logs_patient
        FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    INDEX idx_audit_logs_patient_id (patient_id),
    INDEX idx_audit_logs_timestamp (`timestamp`)
);
