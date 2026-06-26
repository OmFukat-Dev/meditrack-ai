-- =====================================================
-- USER MANAGEMENT DATABASE SEED DATA
-- =====================================================
-- Database: meditrack_user_management

USE meditrack_user_management;

-- =====================================================
-- INSERT ROLES
-- =====================================================
INSERT INTO roles (role_name, description, permissions) VALUES
('admin', 'System administrator with full access', JSON_OBJECT(
    'users', JSON_OBJECT('create', true, 'read', true, 'update', true, 'delete', true),
    'patients', JSON_OBJECT('create', true, 'read', true, 'update', true, 'delete', true),
    'alerts', JSON_OBJECT('create', true, 'read', true, 'update', true, 'delete', true),
    'system', JSON_OBJECT('configure', true, 'monitor', true, 'backup', true)
)),
('doctor', 'Medical doctor with patient management access', JSON_OBJECT(
    'patients', JSON_OBJECT('create', true, 'read', true, 'update', true, 'delete', false),
    'vitals', JSON_OBJECT('create', true, 'read', true, 'update', true, 'delete', false),
    'alerts', JSON_OBJECT('read', true, 'update', true, 'create', false, 'delete', false),
    'medical_records', JSON_OBJECT('create', true, 'read', true, 'update', true, 'delete', false)
)),
('nurse', 'Nurse with vitals input and patient care access', JSON_OBJECT(
    'vitals', JSON_OBJECT('create', true, 'read', true, 'update', true, 'delete', false),
    'patients', JSON_OBJECT('read', true, 'update', true, 'create', false, 'delete', false),
    'alerts', JSON_OBJECT('read', true, 'create', true, 'update', false, 'delete', false)
)),
('viewer', 'Read-only access for viewing purposes', JSON_OBJECT(
    'patients', JSON_OBJECT('read', true, 'create', false, 'update', false, 'delete', false),
    'vitals', JSON_OBJECT('read', true, 'create', false, 'update', false, 'delete', false),
    'alerts', JSON_OBJECT('read', true, 'create', false, 'update', false, 'delete', false)
));

-- =====================================================
-- INSERT DEPARTMENTS
-- =====================================================
INSERT INTO departments (department_name, description, is_active) VALUES
('Cardiology', 'Heart and cardiovascular system diagnosis and treatment', TRUE),
('Neurology', 'Brain and nervous system diagnosis and treatment', TRUE),
('Oncology', 'Cancer diagnosis and treatment', TRUE),
('Emergency', 'Emergency medical care and trauma treatment', TRUE),
('ICU', 'Intensive Care Unit for critical patients', TRUE),
('General Medicine', 'General medical care and primary treatment', TRUE),
('Pediatrics', 'Medical care for children and infants', TRUE),
('Orthopedics', 'Bone and joint treatment and surgery', TRUE);

-- =====================================================
-- INSERT USERS
-- =====================================================
-- Admin Users
INSERT INTO users (id, email, password_hash, first_name, last_name, role_id, department_id, is_active) VALUES
('admin-om', 'om@meditrackadmin.ai', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6ukx.Lr5O', 'Om', 'Fukat', 1, 1, TRUE),
('admin-sakshi', 'sakshi@meditrackadmin.ai', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6ukx.Lr5O', 'Sakshi', 'Admin', 1, 1, TRUE);

-- Admin Details
INSERT INTO admins (user_id, admin_level, access_permissions) VALUES
('admin-om', 'SUPER_ADMIN', JSON_OBJECT('all_access', true, 'system_config', true)),
('admin-sakshi', 'ADMIN', JSON_OBJECT('user_management', true, 'patient_management', true));

-- Doctor Users
INSERT INTO users (id, email, password_hash, first_name, last_name, role_id, department_id, is_active) VALUES
('doc-dipanshu', 'dipanshu@meditrackcardiology.ai', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6ukx.Lr5O', 'Dipanshu', 'Sharma', 2, 1, TRUE),
('doc-tanmay', 'tanmay@meditrackpediatrics.ai', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6ukx.Lr5O', 'Tanmay', 'Kumar', 2, 7, TRUE),
('doc-ayush', 'ayush@meditrackneurology.ai', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6ukx.Lr5O', 'Ayush', 'Singh', 2, 2, TRUE),
('doc-chetan', 'chetan@meditrackoncology.ai', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6ukx.Lr5O', 'Chetan', 'Patel', 2, 3, TRUE),
('doc-monir', 'monir@meditrackorthopedics.ai', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6ukx.Lr5O', 'Monir', 'Hossain', 2, 8, TRUE);

-- Doctor Details
INSERT INTO doctors (user_id, license_number, specialization, years_of_experience, education, certification, consultation_fee, is_available, max_patients) VALUES
('doc-dipanshu', 'MD-2023-001', 'Interventional Cardiology', 8, 'MD Cardiology, AIIMS Delhi', 
 JSON_ARRAY('Board Certified Cardiologist', 'Advanced Cardiac Life Support', 'Interventional Cardiology Certification'), 250.00, TRUE, 50),
('doc-tanmay', 'MD-2023-002', 'General Pediatrics', 6, 'MD Pediatrics, KGMU Lucknow', 
 JSON_ARRAY('Board Certified Pediatrician', 'Pediatric Advanced Life Support', 'Pediatric Endocrinology'), 200.00, TRUE, 45),
('doc-ayush', 'MD-2023-003', 'Neurology', 10, 'MD Neurology, PGIMER Chandigarh', 
 JSON_ARRAY('Board Certified Neurologist', 'EEG Interpretation', 'Stroke Management'), 300.00, TRUE, 40),
('doc-chetan', 'MD-2023-004', 'Medical Oncology', 5, 'MD Oncology, AIIMS Delhi', 
 JSON_ARRAY('Board Certified Oncologist', 'Chemotherapy Certification', 'Oncology Care'), 180.00, TRUE, 35),
('doc-monir', 'MD-2023-005', 'Orthopedic Surgery', 12, 'MS Orthopedics, Tata Memorial Hospital', 
 JSON_ARRAY('Board Certified Surgeon', 'Joint Replacement Certification', 'Trauma Care'), 400.00, TRUE, 30);

-- Nurse Users
INSERT INTO users (id, email, password_hash, first_name, last_name, role_id, department_id, is_active) VALUES
('nurse-sarah', 'sarah@meditrackcardiology.ai', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6ukx.Lr5O', 'Sarah', 'Johnson', 3, 1, TRUE),
('nurse-emily', 'emily@meditrackneurology.ai', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6ukx.Lr5O', 'Emily', 'Davis', 3, 2, TRUE),
('nurse-jessica', 'jessica@meditrackpediatrics.ai', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6ukx.Lr5O', 'Jessica', 'Brown', 3, 7, TRUE),
('nurse-monalisa', 'monalisa@meditrackoncology.ai', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6ukx.Lr5O', 'Monalisa', 'Khan', 3, 3, TRUE),
('nurse-lana', 'lana@meditrackorthopedics.ai', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6ukx.Lr5O', 'Lana', 'Wilson', 3, 8, TRUE);

-- Nurse Details
INSERT INTO nurses (user_id, license_number, certification, shift_type, is_available, max_patients) VALUES
('nurse-sarah', 'RN-2023-001', 
 JSON_ARRAY('Registered Nurse', 'Critical Care Nursing', 'BLS Certification'), 'DAY', TRUE, 20),
('nurse-emily', 'RN-2023-002', 
 JSON_ARRAY('Registered Nurse', 'Neurology Nursing', 'ACLS Certification'), 'ROTATING', TRUE, 18),
('nurse-jessica', 'RN-2023-003', 
 JSON_ARRAY('Registered Nurse', 'Oncology Nursing', 'Chemotherapy Certification'), 'NIGHT', TRUE, 15),
('nurse-monalisa', 'RN-2023-004', 
 JSON_ARRAY('Registered Nurse', 'Cardiac Nursing', 'ECG Interpretation'), 'DAY', TRUE, 22),
('nurse-lana', 'RN-2023-005', 
 JSON_ARRAY('Registered Nurse', 'Emergency Nursing', 'Trauma Care Certification'), 'ROTATING', TRUE, 25);

-- Sample Viewer User
INSERT INTO users (id, email, password_hash, first_name, last_name, role_id, department_id, is_active) VALUES
('viewer-1', 'viewer@meditrack.ai', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6ukx.Lr5O', 'Viewer', 'User', 4, 1, TRUE);

-- =====================================================
-- SAMPLE AUDIT LOGS
-- =====================================================
INSERT INTO audit_logs (user_id, action, entity_type, entity_id, new_values, ip_address, user_agent) VALUES
('admin-om', 'CREATE', 'user', 'doc-dipanshu', 
 JSON_OBJECT('email', 'dipanshu@meditrackcardiology.ai', 'role', 'doctor'), '192.168.1.100', 'Mozilla/5.0'),
('admin-om', 'CREATE', 'user', 'nurse-sarah', 
 JSON_OBJECT('email', 'sarah@meditrackcardiology.ai', 'role', 'nurse'), '192.168.1.100', 'Mozilla/5.0'),
('doc-dipanshu', 'LOGIN', 'user', 'doc-dipanshu', 
 JSON_OBJECT('login_time', NOW()), '192.168.1.101', 'Mozilla/5.0'),
('nurse-sarah', 'CREATE', 'vitals', 'patient-1', 
 JSON_OBJECT('heart_rate', 72, 'blood_pressure', '120/80'), '192.168.1.102', 'Mozilla/5.0');

-- =====================================================
-- UPDATE AUTO_INCREMENT VALUES
-- =====================================================
ALTER TABLE roles AUTO_INCREMENT = 100;
ALTER TABLE departments AUTO_INCREMENT = 100;
ALTER TABLE users AUTO_INCREMENT = 1000;
ALTER TABLE admins AUTO_INCREMENT = 100;
ALTER TABLE doctors AUTO_INCREMENT = 100;
ALTER TABLE nurses AUTO_INCREMENT = 100;
ALTER TABLE audit_logs AUTO_INCREMENT = 1000;
