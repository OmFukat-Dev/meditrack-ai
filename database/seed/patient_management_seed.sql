-- =====================================================
-- PATIENT MANAGEMENT DATABASE SEED DATA
-- =====================================================
-- Database: meditrack_patient_management

USE meditrack_patient_management;

-- =====================================================
-- INSERT 25 PATIENTS (5 per department)
-- =====================================================
INSERT INTO patients (
    id, patient_identifier, email, password_hash, first_name, last_name, 
    date_of_birth, gender, phone, mobile_number, guardian_name, guardian_mobile,
    guardian_relationship, address, city, state, country, postal_code,
    emergency_contact_name, emergency_contact_phone, blood_group, allergies,
    medical_conditions, insurance_provider, insurance_policy_number, room_number,
    bed_number, admission_date, condition_status, doctor_id, primary_nurse_id,
    is_active
) VALUES
-- Cardiology (doc-dipanshu / nurse-sarah)
('patient-1', 'PT-001', 'john.doe@email.com', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6ukx.Lr5O', 
 'John', 'Doe', '1980-05-15', 'MALE', '555-0101', '+1-555-0101',
 'Jane Doe', '+1-555-0102', 'Spouse', '123 Main St', 'New York', 'NY', 'USA', '10001',
 'Jane Doe', '+1-555-0102', 'A+', 
 JSON_ARRAY('Penicillin', 'Peanuts'), 
 JSON_ARRAY('Hypertension', 'Diabetes Type 2'), 
 'Blue Cross', 'BC-101', 'A-101', '1', 
 '2024-04-15 09:00:00', 'STABLE', 'doc-dipanshu', 'nurse-sarah', TRUE),

('patient-2', 'PT-002', 'jane.smith@email.com', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6ukx.Lr5O', 
 'Jane', 'Smith', '1992-08-22', 'FEMALE', '555-0103', '+1-555-0103',
 'Robert Smith', '+1-555-0104', 'Father', '456 Oak Ave', 'Los Angeles', 'CA', 'USA', '90001',
 'Robert Smith', '+1-555-0104', 'B+', 
 JSON_ARRAY('Shellfish'), 
 JSON_ARRAY('Asthma'), 
 'Aetna', 'AE-102', 'A-102', '1', 
 '2024-04-18 14:30:00', 'CRITICAL', 'doc-dipanshu', 'nurse-sarah', TRUE),

('patient-3', 'PT-003', 'alice.cooper@email.com', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6ukx.Lr5O', 
 'Alice', 'Cooper', '1968-12-10', 'FEMALE', '555-0105', '+1-555-0105',
 'Bob Cooper', '+1-555-0106', 'Spouse', '789 Pine Rd', 'Chicago', 'IL', 'USA', '60001',
 'Bob Cooper', '+1-555-0106', 'O+', 
 JSON_ARRAY('None'), 
 JSON_ARRAY('None'), 
 'UnitedHealth', 'UH-103', 'A-103', '1', 
 '2024-04-20 11:15:00', 'STABLE', 'doc-dipanshu', 'nurse-sarah', TRUE),

('patient-4', 'PT-004', 'henry.ford@email.com', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6ukx.Lr5O', 
 'Henry', 'Ford', '1962-03-25', 'MALE', '555-0107', '+1-555-0107',
 'Mary Ford', '+1-555-0108', 'Spouse', '321 Elm St', 'Houston', 'TX', 'USA', '77001',
 'Mary Ford', '+1-555-0108', 'A-', 
 JSON_ARRAY('Latex'), 
 JSON_ARRAY('Allergic Rhinitis'), 
 'Cigna', 'CI-104', 'A-104', '1', 
 '2024-04-22 16:45:00', 'EMERGENCY', 'doc-dipanshu', 'nurse-sarah', TRUE),

('patient-5', 'PT-005', 'linda.evans@email.com', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6ukx.Lr5O', 
 'Linda', 'Evans', '1975-11-08', 'FEMALE', '555-0109', '+1-555-0109',
 'Tom Evans', '+1-555-0110', 'Spouse', '654 Maple Dr', 'Phoenix', 'AZ', 'USA', '85001',
 'Tom Evans', '+1-555-0110', 'AB+', 
 JSON_ARRAY('Sulfa'), 
 JSON_ARRAY('Coronary Artery Disease'), 
 'Humana', 'HU-105', 'A-105', '1', 
 '2024-04-25 10:30:00', 'STABLE', 'doc-dipanshu', 'nurse-sarah', TRUE),

-- Pediatrics (doc-tanmay / nurse-jessica)
('patient-6', 'PT-006', 'emily.brown@email.com', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6ukx.Lr5O', 
 'Emily', 'Brown', '2016-04-27', 'FEMALE', '555-0111', '+1-555-0111',
 'David Brown', '+1-555-0112', 'Father', '987 Cedar Ln', 'Philadelphia', 'PA', 'USA', '19101',
 'David Brown', '+1-555-0112', 'O+', 
 JSON_ARRAY('None'), 
 JSON_ARRAY('None'), 
 'Blue Cross', 'BC-106', 'B-101', '1', 
 '2024-04-27 13:20:00', 'STABLE', 'doc-tanmay', 'nurse-jessica', TRUE),

('patient-7', 'PT-007', 'liam.jones@email.com', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6ukx.Lr5O', 
 'Liam', 'Jones', '2012-02-14', 'MALE', '555-0113', '+1-555-0113',
 'Sarah Jones', '+1-555-0114', 'Mother', '147 Birch Rd', 'San Antonio', 'TX', 'USA', '78201',
 'Sarah Jones', '+1-555-0114', 'B+', 
 JSON_ARRAY('Pollen'), 
 JSON_ARRAY('Asthma'), 
 'Aetna', 'AE-107', 'B-102', '1', 
 '2024-04-29 08:45:00', 'CRITICAL', 'doc-tanmay', 'nurse-jessica', TRUE),

('patient-8', 'PT-008', 'mia.clark@email.com', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6ukx.Lr5O', 
 'Mia', 'Clark', '2018-09-18', 'FEMALE', '555-0115', '+1-555-0115',
 'Thomas Clark', '+1-555-0116', 'Father', '258 Spruce St', 'San Diego', 'CA', 'USA', '92101',
 'Thomas Clark', '+1-555-0116', 'A+', 
 JSON_ARRAY('None'), 
 JSON_ARRAY('None'), 
 'UnitedHealth', 'UH-108', 'B-103', '1', 
 '2024-05-01 15:30:00', 'STABLE', 'doc-tanmay', 'nurse-jessica', TRUE),

('patient-9', 'PT-009', 'noah.white@email.com', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6ukx.Lr5O', 
 'Noah', 'White', '2014-07-22', 'MALE', '555-0117', '+1-555-0117',
 'Anna White', '+1-555-0118', 'Mother', '369 Willow Ave', 'Dallas', 'TX', 'USA', '75201',
 'Anna White', '+1-555-0118', 'O-', 
 JSON_ARRAY('Penicillin'), 
 JSON_ARRAY('None'), 
 'Cigna', 'CI-109', 'B-104', '1', 
 '2024-05-03 11:00:00', 'EMERGENCY', 'doc-tanmay', 'nurse-jessica', TRUE),

('patient-10', 'PT-010', 'sofia.harris@email.com', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6ukx.Lr5O', 
 'Sofia', 'Harris', '2010-04-05', 'FEMALE', '555-0119', '+1-555-0119',
 'Eric Harris', '+1-555-0120', 'Father', '741 Poplar Dr', 'San Jose', 'CA', 'USA', '95101',
 'Eric Harris', '+1-555-0120', 'B+', 
 JSON_ARRAY('None'), 
 JSON_ARRAY('None'), 
 'Humana', 'HU-110', 'B-105', '1', 
 '2024-05-05 14:15:00', 'STABLE', 'doc-tanmay', 'nurse-jessica', TRUE),

-- Neurology (doc-ayush / nurse-emily)
('patient-11', 'PT-011', 'robert.johnson@email.com', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6ukx.Lr5O', 
 'Robert', 'Johnson', '1969-10-12', 'MALE', '555-0121', '+1-555-0121',
 'Mary Johnson', '+1-555-0122', 'Spouse', '852 Oak St', 'Austin', 'TX', 'USA', '78701',
 'Mary Johnson', '+1-555-0122', 'A+', 
 JSON_ARRAY('Shellfish'), 
 JSON_ARRAY('Stroke history'), 
 'Medicare', 'MC-111', 'C-101', '1', 
 '2024-04-20 09:30:00', 'EMERGENCY', 'doc-ayush', 'nurse-emily', TRUE),

('patient-12', 'PT-012', 'patricia.thomas@email.com', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6ukx.Lr5O', 
 'Patricia', 'Thomas', '1963-01-28', 'FEMALE', '555-0123', '+1-555-0123',
 'Robert Thomas', '+1-555-0124', 'Spouse', '963 Pine Rd', 'Jacksonville', 'FL', 'USA', '32201',
 'Robert Thomas', '+1-555-0124', 'B+', 
 JSON_ARRAY('Latex'), 
 JSON_ARRAY('Osteoporosis'), 
 'Blue Cross', 'BC-112', 'C-102', '1', 
 '2024-05-05 16:45:00', 'STABLE', 'doc-ayush', 'nurse-emily', TRUE),

('patient-13', 'PT-013', 'paul.walker@email.com', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6ukx.Lr5O', 
 'Paul', 'Walker', '1981-06-15', 'MALE', '555-0125', '+1-555-0125',
 'Linda Walker', '+1-555-0126', 'Mother', '147 Birch Rd', 'Indianapolis', 'IN', 'USA', '46201',
 'Linda Walker', '+1-555-0126', 'B-', 
 JSON_ARRAY('None'), 
 JSON_ARRAY('None'), 
 'Cigna', 'CI-113', 'C-103', '1', 
 '2024-05-10 12:00:00', 'CRITICAL', 'doc-ayush', 'nurse-emily', TRUE),

('patient-14', 'PT-014', 'donna.king@email.com', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6ukx.Lr5O', 
 'Donna', 'King', '1988-08-03', 'FEMALE', '555-0127', '+1-555-0127',
 'Richard King', '+1-555-0128', 'Spouse', '258 Spruce St', 'Columbus', 'OH', 'USA', '43201',
 'Richard King', '+1-555-0128', 'AB+', 
 JSON_ARRAY('Pollen'), 
 JSON_ARRAY('Migraines'), 
 'Medicare', 'MC-114', 'C-104', '1', 
 '2024-05-15 10:15:00', 'STABLE', 'doc-ayush', 'nurse-emily', TRUE),

('patient-15', 'PT-015', 'james.taylor@email.com', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6ukx.Lr5O', 
 'James', 'Taylor', '1977-11-20', 'MALE', '555-0129', '+1-555-0129',
 'Patricia Taylor', '+1-555-0130', 'Spouse', '369 Willow Ave', 'Charlotte', 'NC', 'USA', '28201',
 'Patricia Taylor', '+1-555-0130', 'O+', 
 JSON_ARRAY('None'), 
 JSON_ARRAY('None'), 
 'UnitedHealth', 'UH-115', 'C-105', '1', 
 '2024-05-18 14:30:00', 'EMERGENCY', 'doc-ayush', 'nurse-emily', TRUE),

-- Oncology (doc-chetan / nurse-monalisa)
('patient-16', 'PT-016', 'sarah.davis@email.com', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6ukx.Lr5O', 
 'Sarah', 'Davis', '1974-04-27', 'FEMALE', '555-0131', '+1-555-0131',
 'James Davis', '+1-555-0132', 'Father', '987 Cedar Ln', 'Philadelphia', 'PA', 'USA', '19101',
 'James Davis', '+1-555-0132', 'O+', 
 JSON_ARRAY('None'), 
 JSON_ARRAY('None'), 
 'Blue Cross', 'BC-116', 'D-101', '1', 
 '2024-04-27 13:20:00', 'CRITICAL', 'doc-chetan', 'nurse-monalisa', TRUE),

('patient-17', 'PT-017', 'mark.allen@email.com', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6ukx.Lr5O', 
 'Mark', 'Allen', '1966-05-25', 'MALE', '555-0133', '+1-555-0133',
 'Diane Allen', '+1-555-0134', 'Wife', '852 Oak St', 'Seattle', 'WA', 'USA', '98101',
 'Diane Allen', '+1-555-0134', 'B+', 
 JSON_ARRAY('None'), 
 JSON_ARRAY('None'), 
 'Aetna', 'AE-117', 'D-102', '1', 
 '2024-05-05 13:20:00', 'EMERGENCY', 'doc-chetan', 'nurse-monalisa', TRUE),

('patient-18', 'PT-018', 'sandra.young@email.com', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6ukx.Lr5O', 
 'Sandra', 'Young', '1982-09-08', 'FEMALE', '555-0135', '+1-555-0135',
 'Robert Young', '+1-555-0136', 'Father', '963 Pine Rd', 'Portland', 'OR', 'USA', '97201',
 'Robert Young', '+1-555-0136', 'O-', 
 JSON_ARRAY('Dust'), 
 JSON_ARRAY('Chronic Bronchitis'), 
 'Blue Cross', 'BC-118', 'D-103', '1', 
 '2024-05-10 11:45:00', 'STABLE', 'doc-chetan', 'nurse-monalisa', TRUE),

('patient-19', 'PT-019', 'kevin.hernandez@email.com', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6ukx.Lr5O', 
 'Kevin', 'Hernandez', '1970-12-03', 'MALE', '555-0137', '+1-555-0137',
 'Maria Hernandez', '+1-555-0138', 'Wife', '147 Birch Rd', 'Las Vegas', 'NV', 'USA', '89101',
 'Maria Hernandez', '+1-555-0138', 'AB+', 
 JSON_ARRAY('None'), 
 JSON_ARRAY('Type 1 Diabetes'), 
 'Humana', 'HU-119', 'D-104', '1', 
 '2024-05-15 16:10:00', 'CRITICAL', 'doc-chetan', 'nurse-monalisa', TRUE),

('patient-20', 'PT-020', 'jennifer.robinson@email.com', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6ukx.Lr5O', 
 'Jennifer', 'Robinson', '1978-07-15', 'FEMALE', '555-0139', '+1-555-0139',
 'William Robinson', '+1-555-0140', 'Husband', '258 Spruce St', 'Miami', 'FL', 'USA', '33101',
 'William Robinson', '+1-555-0140', 'A+', 
 JSON_ARRAY('Shellfish'), 
 JSON_ARRAY('None'), 
 'UnitedHealth', 'UH-120', 'D-105', '1', 
 '2024-05-20 09:00:00', 'STABLE', 'doc-chetan', 'nurse-monalisa', TRUE),

-- Orthopedics (doc-monir / nurse-lana)
('patient-21', 'PT-021', 'david.miller@email.com', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6ukx.Lr5O', 
 'David', 'Miller', '1976-02-14', 'MALE', '555-0141', '+1-555-0141',
 'Susan Miller', '+1-555-0142', 'Wife', '147 Birch Rd', 'San Antonio', 'TX', 'USA', '78201',
 'Susan Miller', '+1-555-0142', 'B+', 
 JSON_ARRAY('Pollen'), 
 JSON_ARRAY('COPD'), 
 'Medicare', 'MC-121', 'E-101', '1', 
 '2024-04-29 08:45:00', 'STABLE', 'doc-monir', 'nurse-lana', TRUE),

('patient-22', 'PT-022', 'karen.hall@email.com', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6ukx.Lr5O', 
 'Karen', 'Hall', '1986-02-10', 'FEMALE', '555-0143', '+1-555-0143',
 'Steven Hall', '+1-555-0144', 'Husband', '741 Poplar Dr', 'Denver', 'CO', 'USA', '80201',
 'Steven Hall', '+1-555-0144', 'A+', 
 JSON_ARRAY('Penicillin'), 
 JSON_ARRAY('Hypothyroidism'), 
 'Kaiser', 'KP-122', 'E-102', '1', 
 '2024-05-03 08:45:00', 'STABLE', 'doc-monir', 'nurse-lana', TRUE),

('patient-23', 'PT-023', 'christopher.martinez@email.com', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6ukx.Lr5O', 
 'Christopher', 'Martinez', '1961-10-12', 'MALE', '555-0145', '+1-555-0145',
 'Maria Martinez', '+1-555-0146', 'Wife', '852 Oak St', 'Austin', 'TX', 'USA', '78701',
 'Maria Martinez', '+1-555-0146', 'A+', 
 JSON_ARRAY('Shellfish'), 
 JSON_ARRAY('Type 2 Diabetes'), 
 'Humana', 'HU-123', 'E-103', '1', 
 '2024-05-07 09:30:00', 'EMERGENCY', 'doc-monir', 'nurse-lana', TRUE),

('patient-24', 'PT-024', 'nancy.lewis@email.com', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6ukx.Lr5O', 
 'Nancy', 'Lewis', '1969-08-03', 'FEMALE', '555-0147', '+1-555-0147',
 'George Lewis', '+1-555-0148', 'Husband', '258 Spruce St', 'Columbus', 'OH', 'USA', '43201',
 'George Lewis', '+1-555-0148', 'AB+', 
 JSON_ARRAY('Pollen'), 
 JSON_ARRAY('Seasonal Allergies'), 
 'Medicare', 'MC-124', 'E-104', '1', 
 '2024-05-12 10:15:00', 'CRITICAL', 'doc-monir', 'nurse-lana', TRUE),

('patient-25', 'PT-025', 'lisa.anderson@email.com', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6ukx.Lr5O', 
 'Lisa', 'Anderson', '1991-09-18', 'FEMALE', '555-0149', '+1-555-0149',
 'Thomas Anderson', '+1-555-0150', 'Father', '258 Spruce St', 'San Diego', 'CA', 'USA', '92101',
 'Thomas Anderson', '+1-555-0150', 'A+', 
 JSON_ARRAY('None'), 
 JSON_ARRAY('None'), 
 'Kaiser', 'KP-125', 'E-105', '1', 
 '2024-05-17 15:30:00', 'STABLE', 'doc-monir', 'nurse-lana', TRUE);

-- =====================================================
-- INSERT DOCTOR-PATIENT ASSIGNMENTS (25 assignments)
-- =====================================================
INSERT INTO doctor_patient_assignment (doctor_id, patient_id, assignment_status, notes) VALUES
('doc-dipanshu', 'patient-1', 'ACTIVE', 'Primary cardiology care'),
('doc-dipanshu', 'patient-2', 'ACTIVE', 'Cardiac monitoring'),
('doc-dipanshu', 'patient-3', 'ACTIVE', 'Cardiology support'),
('doc-dipanshu', 'patient-4', 'ACTIVE', 'Cardiology follow-up'),
('doc-dipanshu', 'patient-5', 'ACTIVE', 'Cardiology general'),

('doc-tanmay', 'patient-6', 'ACTIVE', 'Pediatrics support'),
('doc-tanmay', 'patient-7', 'ACTIVE', 'Pediatrics critical care'),
('doc-tanmay', 'patient-8', 'ACTIVE', 'Pediatrics checkup'),
('doc-tanmay', 'patient-9', 'ACTIVE', 'Pediatrics emergency'),
('doc-tanmay', 'patient-10', 'ACTIVE', 'Pediatrics routine'),

('doc-ayush', 'patient-11', 'ACTIVE', 'Neurology monitoring'),
('doc-ayush', 'patient-12', 'ACTIVE', 'Neurology routine'),
('doc-ayush', 'patient-13', 'ACTIVE', 'Neurology critical'),
('doc-ayush', 'patient-14', 'ACTIVE', 'Neurology checkup'),
('doc-ayush', 'patient-15', 'ACTIVE', 'Neurology emergency'),

('doc-chetan', 'patient-16', 'ACTIVE', 'Oncology critical'),
('doc-chetan', 'patient-17', 'ACTIVE', 'Oncology emergency'),
('doc-chetan', 'patient-18', 'ACTIVE', 'Oncology support'),
('doc-chetan', 'patient-19', 'ACTIVE', 'Oncology checkup'),
('doc-chetan', 'patient-20', 'ACTIVE', 'Oncology routine'),

('doc-monir', 'patient-21', 'ACTIVE', 'Orthopedics checkup'),
('doc-monir', 'patient-22', 'ACTIVE', 'Orthopedics general'),
('doc-monir', 'patient-23', 'ACTIVE', 'Orthopedics emergency'),
('doc-monir', 'patient-24', 'ACTIVE', 'Orthopedics critical'),
('doc-monir', 'patient-25', 'ACTIVE', 'Orthopedics routine');

-- =====================================================
-- INSERT NURSE-PATIENT ASSIGNMENTS (25 assignments)
-- =====================================================
INSERT INTO nurse_patient_assignment (nurse_id, patient_id, shift_type, assignment_status, notes) VALUES
('nurse-sarah', 'patient-1', 'DAY', 'ACTIVE', 'Primary care nurse'),
('nurse-sarah', 'patient-2', 'DAY', 'ACTIVE', 'Primary care nurse'),
('nurse-sarah', 'patient-3', 'DAY', 'ACTIVE', 'Primary care nurse'),
('nurse-sarah', 'patient-4', 'DAY', 'ACTIVE', 'Primary care nurse'),
('nurse-sarah', 'patient-5', 'DAY', 'ACTIVE', 'Primary care nurse'),

('nurse-jessica', 'patient-6', 'DAY', 'ACTIVE', 'Pediatrics nurse'),
('nurse-jessica', 'patient-7', 'DAY', 'ACTIVE', 'Pediatrics nurse'),
('nurse-jessica', 'patient-8', 'DAY', 'ACTIVE', 'Pediatrics nurse'),
('nurse-jessica', 'patient-9', 'DAY', 'ACTIVE', 'Pediatrics nurse'),
('nurse-jessica', 'patient-10', 'DAY', 'ACTIVE', 'Pediatrics nurse'),

('nurse-emily', 'patient-11', 'NIGHT', 'ACTIVE', 'Neurology nurse'),
('nurse-emily', 'patient-12', 'NIGHT', 'ACTIVE', 'Neurology nurse'),
('nurse-emily', 'patient-13', 'NIGHT', 'ACTIVE', 'Neurology nurse'),
('nurse-emily', 'patient-14', 'NIGHT', 'ACTIVE', 'Neurology nurse'),
('nurse-emily', 'patient-15', 'NIGHT', 'ACTIVE', 'Neurology nurse'),

('nurse-monalisa', 'patient-16', 'DAY', 'ACTIVE', 'Oncology nurse'),
('nurse-monalisa', 'patient-17', 'DAY', 'ACTIVE', 'Oncology nurse'),
('nurse-monalisa', 'patient-18', 'DAY', 'ACTIVE', 'Oncology nurse'),
('nurse-monalisa', 'patient-19', 'DAY', 'ACTIVE', 'Oncology nurse'),
('nurse-monalisa', 'patient-20', 'DAY', 'ACTIVE', 'Oncology nurse'),

('nurse-lana', 'patient-21', 'ROTATING', 'ACTIVE', 'Orthopedics nurse'),
('nurse-lana', 'patient-22', 'ROTATING', 'ACTIVE', 'Orthopedics nurse'),
('nurse-lana', 'patient-23', 'ROTATING', 'ACTIVE', 'Orthopedics nurse'),
('nurse-lana', 'patient-24', 'ROTATING', 'ACTIVE', 'Orthopedics nurse'),
('nurse-lana', 'patient-25', 'ROTATING', 'ACTIVE', 'Orthopedics nurse');

-- =====================================================
-- INSERT SAMPLE VITAL SIGNS
-- =====================================================
INSERT INTO vital_signs (patient_id, recorded_by, heart_rate, blood_pressure_systolic, blood_pressure_diastolic, temperature, oxygen_saturation, respiratory_rate, notes, recorded_at) VALUES
('patient-1', 'nurse-sarah', 72, 120, 80, 98.6, 98, 16, 'Normal vitals', '2024-05-20 08:00:00'),
('patient-1', 'nurse-sarah', 75, 118, 78, 98.4, 97, 15, 'Slightly elevated heart rate', '2024-05-20 12:00:00'),
('patient-1', 'nurse-sarah', 70, 122, 82, 98.8, 98, 16, 'Post-lunch vitals', '2024-05-20 16:00:00'),
('patient-2', 'nurse-sarah', 145, 160, 100, 101.2, 88, 24, 'CRITICAL - Elevated vitals', '2024-05-20 09:30:00'),
('patient-2', 'nurse-sarah', 138, 155, 95, 100.8, 90, 22, 'Still elevated but improving', '2024-05-20 13:30:00'),
('patient-11', 'nurse-emily', 92, 140, 90, 99.5, 85, 28, 'Emergency - High respiratory rate', '2024-05-20 02:00:00'),
('patient-11', 'nurse-emily', 85, 135, 88, 99.1, 89, 25, 'Improving after treatment', '2024-05-20 06:00:00'),
('patient-6', 'nurse-jessica', 68, 115, 75, 98.2, 99, 14, 'Normal vitals', '2024-05-20 10:00:00'),
('patient-16', 'nurse-monalisa', 88, 145, 92, 99.8, 91, 20, 'Slightly elevated BP', '2024-05-20 11:00:00'),
('patient-21', 'nurse-lana', 65, 110, 70, 98.0, 100, 12, 'Normal orthopedics vitals', '2024-05-20 09:00:00');

-- =====================================================
-- INSERT SAMPLE ALERTS
-- =====================================================
INSERT INTO alerts (id, patient_id, alert_type, alert_category, title, message, alert_source, priority_score, assigned_to, status, created_at) VALUES
('alert-1', 'patient-2', 'CRITICAL', 'VITAL_SIGNS', 'Heart Rate Dangerously High', 
 'Heart rate is 145 BPM - requires immediate attention', 'SYSTEM', 9, 'doc-dipanshu', 'ACTIVE', '2024-05-20 09:30:00'),
('alert-2', 'patient-2', 'HIGH', 'VITAL_SIGNS', 'Blood Pressure Elevated', 
 'Blood pressure is 160/100 mmHg - above normal range', 'SYSTEM', 7, 'doc-dipanshu', 'ACTIVE', '2024-05-20 09:30:00'),
('alert-3', 'patient-11', 'CRITICAL', 'VITAL_SIGNS', 'Respiratory Distress', 
 'Respiratory rate is 28 breaths/min - critically high', 'SYSTEM', 8, 'doc-ayush', 'ACTIVE', '2024-05-20 02:00:00');

-- =====================================================
-- INSERT SAMPLE MEDICAL RECORDS
-- =====================================================
INSERT INTO medical_records (id, patient_id, doctor_id, record_type, title, description, diagnosis, prescription, treatment_plan, follow_up_date, created_at) VALUES
('record-1', 'patient-1', 'doc-dipanshu', 'CONSULTATION', 'Initial Cardiology Consultation', 
 'Patient presents with chest pain and shortness of breath. History of hypertension.', 
 'Hypertension, Possible Angina', 
 JSON_ARRAY('Lisinopril 10mg daily', 'Metoprolol 25mg twice daily', 'Aspirin 81mg daily'), 
 'Continue current medications, lifestyle modifications, follow-up in 2 weeks', '2024-05-28', '2024-05-20 09:00:00'),

('record-2', 'patient-2', 'doc-dipanshu', 'DIAGNOSIS', 'Acute Myocardial Infarction', 
 'Patient admitted with severe chest pain, ECG shows ST elevation. Troponin levels elevated.', 
 'Acute ST-Elevation Myocardial Infarction', 
 JSON_ARRAY('Aspirin 325mg loading dose', 'Heparin IV infusion', 'Morphine for pain'), 
 'Emergency cardiac catheterization, possible stenting, ICU monitoring', '2024-05-25', '2024-05-20 10:00:00');

-- =====================================================
-- INSERT SAMPLE MEDICATIONS
-- =====================================================
INSERT INTO medications (id, patient_id, prescribed_by, medication_name, dosage, frequency, route, start_date, end_date, is_active, instructions) VALUES
('med-1', 'patient-1', 'doc-dipanshu', 'Lisinopril', '10mg', 'Once daily', 'ORAL', '2024-05-20', NULL, TRUE, 'Take in the morning with food'),
('med-2', 'patient-1', 'doc-dipanshu', 'Metoprolol', '25mg', 'Twice daily', 'ORAL', '2024-05-20', NULL, TRUE, 'Take every 12 hours'),
('med-3', 'patient-2', 'doc-dipanshu', 'Aspirin', '81mg', 'Once daily', 'ORAL', '2024-05-20', NULL, TRUE, 'Take with food to prevent stomach upset');

-- =====================================================
-- INSERT SAMPLE APPOINTMENTS
-- =====================================================
INSERT INTO appointments (id, patient_id, doctor_id, appointment_type, title, description, appointment_date, duration_minutes, status, location, notes) VALUES
('appt-1', 'patient-1', 'doc-dipanshu', 'FOLLOW_UP', 'Cardiology Follow-up', 
 'Routine follow-up for hypertension management', '2024-05-28 10:00:00', 30, 'SCHEDULED', 'Cardiology Clinic', 'Bring medication list'),
('appt-2', 'patient-2', 'doc-dipanshu', 'CONSULTATION', 'Post-Discharge Consultation', 
 'Follow-up after cardiac catheterization', '2024-05-25 14:00:00', 45, 'SCHEDULED', 'Cardiology Clinic', 'Review procedure results');

-- =====================================================
-- UPDATE AUTO_INCREMENT VALUES
-- =====================================================
ALTER TABLE patients AUTO_INCREMENT = 1000;
ALTER TABLE doctor_patient_assignment AUTO_INCREMENT = 1000;
ALTER TABLE nurse_patient_assignment AUTO_INCREMENT = 1000;
ALTER TABLE vital_signs AUTO_INCREMENT = 1000;
ALTER TABLE alerts AUTO_INCREMENT = 1000;
ALTER TABLE medical_records AUTO_INCREMENT = 1000;
ALTER TABLE medications AUTO_INCREMENT = 1000;
ALTER TABLE appointments AUTO_INCREMENT = 1000;
