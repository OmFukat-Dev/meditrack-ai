-- MediTrack AI - Staff Members registry and initial access seed data

CREATE TABLE staff_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    role VARCHAR(20) NOT NULL,
    department VARCHAR(100),
    specialization VARCHAR(120),
    phone_number VARCHAR(30),
    license_number VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_staff_email (email),
    INDEX idx_staff_role (role),
    INDEX idx_staff_department (department)
);

INSERT INTO staff_members (full_name, email, role, department, specialization, phone_number, license_number, is_active) VALUES
('Dipanshu', 'dipanshu@meditrackcardiology.ai', 'DOCTOR', 'Cardiology', 'Cardiology', NULL, NULL, TRUE),
('Ayush', 'ayush@meditrackneurology.ai', 'DOCTOR', 'Neurology', 'Neurology', NULL, NULL, TRUE),
('Tanmay', 'tanmay@meditrackpediatrics.ai', 'DOCTOR', 'Pediatrics', 'Pediatrics', NULL, NULL, TRUE),
('Chetan', 'chetan@meditrackoncology.ai', 'DOCTOR', 'Oncology', 'Oncology', NULL, NULL, TRUE),
('Monir', 'monir@meditrackorthopedics.ai', 'DOCTOR', 'Orthopedics', 'Orthopedics', NULL, NULL, TRUE),
('Sarah', 'sarah@meditrackcardiology.ai', 'NURSE', 'Cardiology', NULL, NULL, NULL, TRUE),
('Emily', 'emily@meditrackneurology.ai', 'NURSE', 'Neurology', NULL, NULL, NULL, TRUE),
('Jessica', 'jessica@meditrackpediatrics.ai', 'NURSE', 'Pediatrics', NULL, NULL, NULL, TRUE),
('Monalisa', 'monalisa@meditrackoncology.ai', 'NURSE', 'Oncology', NULL, NULL, NULL, TRUE),
('Lana', 'lana@meditrackorthopedics.ai', 'NURSE', 'Orthopedics', NULL, NULL, NULL, TRUE),
('Om', 'om@meditrackadmin.ai', 'ADMIN', NULL, NULL, NULL, NULL, TRUE),
('Sakshi', 'sakshi@meditrackadmin.ai', 'ADMIN', NULL, NULL, NULL, NULL, TRUE);
