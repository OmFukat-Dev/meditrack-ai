-- =====================================================
-- SAMPLE QUERIES FOR MEDITRACK AI SYSTEM
-- =====================================================
-- This file contains essential queries for cross-database operations
-- and data validation for the Hospital Management System

-- =====================================================
-- 1. GET PATIENTS BY DOCTOR (CROSS-DATABASE QUERY)
-- =====================================================
-- This query joins User Management DB with Patient Management DB
-- Usage: Doctor dashboard - show only assigned patients

-- Step 1: Get doctor's assigned patients from Patient Management DB
SELECT 
    p.id,
    p.patient_identifier,
    p.first_name,
    p.last_name,
    p.email,
    p.mobile_number,
    p.guardian_name,
    p.room_number,
    p.bed_number,
    p.admission_date,
    p.condition_status,
    p.is_active,
    dpa.assignment_date,
    dpa.assignment_status
FROM meditrack_patient_management.patients p
INNER JOIN meditrack_patient_management.doctor_patient_assignment dpa 
    ON p.id = dpa.patient_id
WHERE dpa.doctor_id = ? -- Doctor ID from User Management DB
    AND dpa.assignment_status = 'ACTIVE'
    AND p.is_active = TRUE
ORDER BY p.admission_date DESC;

-- =====================================================
-- 2. GET ALL PATIENTS (ADMIN VIEW)
-- =====================================================
-- Usage: Admin dashboard - complete patient overview

SELECT 
    p.id,
    p.patient_identifier,
    p.first_name,
    p.last_name,
    p.email,
    p.mobile_number,
    p.guardian_name,
    p.room_number,
    p.bed_number,
    p.admission_date,
    p.condition_status,
    p.is_active,
    u.first_name as doctor_first_name,
    u.last_name as doctor_last_name,
    u.email as doctor_email,
    d.department_name as department,
    dpa.assignment_date as doctor_assignment_date
FROM meditrack_patient_management.patients p
LEFT JOIN meditrack_patient_management.doctor_patient_assignment dpa 
    ON p.id = dpa.patient_id
    AND dpa.assignment_status = 'ACTIVE'
LEFT JOIN meditrack_user_management.users u 
    ON dpa.doctor_id = u.id
LEFT JOIN meditrack_user_management.departments d 
    ON u.department_id = d.id
WHERE p.is_active = TRUE
ORDER BY p.admission_date DESC;

-- =====================================================
-- 3. GET VITALS PER PATIENT
-- =====================================================
-- Usage: Doctor/Nurse dashboard - patient vital signs history

SELECT 
    vs.id,
    vs.heart_rate,
    vs.blood_pressure_systolic,
    vs.blood_pressure_diastolic,
    vs.temperature,
    vs.oxygen_saturation,
    vs.respiratory_rate,
    vs.blood_sugar,
    vs.weight,
    vs.height,
    vs.bmi,
    vs.notes,
    vs.measurement_location,
    vs.recorded_at,
    recorder.first_name as recorded_by_first_name,
    recorder.last_name as recorded_by_last_name,
    recorder.role_id as recorded_by_role
FROM meditrack_patient_management.vital_signs vs
INNER JOIN meditrack_user_management.users recorder 
    ON vs.recorded_by = recorder.id
WHERE vs.patient_id = ?
    AND vs.recorded_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
ORDER BY vs.recorded_at DESC
LIMIT 50;

-- =====================================================
-- 4. GET PATIENTS BY NURSE
-- =====================================================
-- Usage: Nurse dashboard - show assigned patients

SELECT 
    p.id,
    p.patient_identifier,
    p.first_name,
    p.last_name,
    p.email,
    p.mobile_number,
    p.room_number,
    p.bed_number,
    p.condition_status,
    p.is_active,
    npa.assignment_date,
    npa.shift_type,
    npa.assignment_status,
    doctor.first_name as doctor_first_name,
    doctor.last_name as doctor_last_name
FROM meditrack_patient_management.patients p
INNER JOIN meditrack_patient_management.nurse_patient_assignment npa 
    ON p.id = npa.patient_id
LEFT JOIN meditrack_user_management.users doctor 
    ON p.doctor_id = doctor.id
WHERE npa.nurse_id = ? -- Nurse ID from User Management DB
    AND npa.assignment_status = 'ACTIVE'
    AND p.is_active = TRUE
ORDER BY npa.assignment_date DESC;

-- =====================================================
-- 5. GET ACTIVE ALERTS FOR DOCTOR
-- =====================================================
-- Usage: Doctor dashboard - show assigned patient alerts

SELECT 
    a.id,
    a.alert_type,
    a.alert_category,
    a.title,
    a.message,
    a.status,
    a.priority_score,
    a.created_at,
    p.patient_identifier,
    p.first_name as patient_first_name,
    p.last_name as patient_last_name,
    p.room_number,
    p.condition_status
FROM meditrack_patient_management.alerts a
INNER JOIN meditrack_patient_management.patients p 
    ON a.patient_id = p.id
WHERE a.assigned_to = ? -- Doctor ID from User Management DB
    AND a.status IN ('ACTIVE', 'ACKNOWLEDGED')
ORDER BY a.priority_score DESC, a.created_at DESC;

-- =====================================================
-- 6. GET ALL ACTIVE ALERTS (ADMIN VIEW)
-- =====================================================
-- Usage: Admin dashboard - complete alert overview

SELECT 
    a.id,
    a.alert_type,
    a.alert_category,
    a.title,
    a.message,
    a.status,
    a.priority_score,
    a.created_at,
    p.patient_identifier,
    p.first_name as patient_first_name,
    p.last_name as patient_last_name,
    p.room_number,
    p.condition_status,
    assigned_doctor.first_name as assigned_doctor_first_name,
    assigned_doctor.last_name as assigned_doctor_last_name,
    d.department_name as department
FROM meditrack_patient_management.alerts a
INNER JOIN meditrack_patient_management.patients p 
    ON a.patient_id = p.id
LEFT JOIN meditrack_user_management.users assigned_doctor 
    ON a.assigned_to = assigned_doctor.id
LEFT JOIN meditrack_user_management.departments d 
    ON assigned_doctor.department_id = d.id
WHERE a.status IN ('ACTIVE', 'ACKNOWLEDGED')
ORDER BY a.priority_score DESC, a.created_at DESC;

-- =====================================================
-- 7. GET DOCTOR WORKLOAD STATISTICS
-- =====================================================
-- Usage: Admin dashboard - workload monitoring

SELECT 
    u.id as doctor_id,
    u.first_name,
    u.last_name,
    u.email,
    d.department_name,
    COUNT(DISTINCT dpa.patient_id) as total_patients,
    COUNT(DISTINCT CASE WHEN p.condition_status IN ('CRITICAL', 'EMERGENCY') THEN dpa.patient_id END) as critical_patients,
    COUNT(DISTINCT CASE WHEN p.condition_status = 'STABLE' THEN dpa.patient_id END) as stable_patients,
    MAX(dpa.assignment_date) as last_assignment_date,
    doc.specialization,
    doc.max_patients,
    CASE 
        WHEN COUNT(DISTINCT dpa.patient_id) >= doc.max_patients * 0.9 THEN 'HIGH'
        WHEN COUNT(DISTINCT dpa.patient_id) >= doc.max_patients * 0.7 THEN 'MEDIUM'
        ELSE 'LOW'
    END as workload_level
FROM meditrack_user_management.users u
INNER JOIN meditrack_user_management.departments d 
    ON u.department_id = d.id
LEFT JOIN meditrack_user_management.doctors doc 
    ON u.id = doc.user_id
LEFT JOIN meditrack_patient_management.doctor_patient_assignment dpa 
    ON u.id = dpa.doctor_id
    AND dpa.assignment_status = 'ACTIVE'
LEFT JOIN meditrack_patient_management.patients p 
    ON dpa.patient_id = p.id
    AND p.is_active = TRUE
WHERE u.role_id = 2 -- Doctor role
    AND u.is_active = TRUE
GROUP BY u.id, u.first_name, u.last_name, u.email, d.department_name, doc.specialization, doc.max_patients
ORDER BY total_patients DESC;

-- =====================================================
-- 8. GET DEPARTMENT STATISTICS
-- =====================================================
-- Usage: Admin dashboard - department performance

SELECT 
    d.id as department_id,
    d.department_name,
    COUNT(DISTINCT u.id) as total_doctors,
    COUNT(DISTINCT CASE WHEN u.role_id = 3 THEN u.id END) as total_nurses,
    COUNT(DISTINCT dpa.patient_id) as total_patients,
    COUNT(DISTINCT CASE WHEN p.condition_status IN ('CRITICAL', 'EMERGENCY') THEN dpa.patient_id END) as critical_cases,
    COUNT(DISTINCT CASE WHEN p.condition_status = 'STABLE' THEN dpa.patient_id END) as stable_cases,
    AVG(CASE WHEN p.admission_date >= DATE_SUB(NOW(), INTERVAL 30 DAY) THEN 1 ELSE 0 END) * 100 as admission_rate_30d
FROM meditrack_user_management.departments d
LEFT JOIN meditrack_user_management.users u 
    ON d.id = u.department_id
    AND u.is_active = TRUE
LEFT JOIN meditrack_patient_management.doctor_patient_assignment dpa 
    ON u.id = dpa.doctor_id
    AND dpa.assignment_status = 'ACTIVE'
LEFT JOIN meditrack_patient_management.patients p 
    ON dpa.patient_id = p.id
    AND p.is_active = TRUE
GROUP BY d.id, d.department_name
ORDER BY total_patients DESC;

-- =====================================================
-- 9. VALIDATE CROSS-DATABASE RELATIONSHIPS
-- =====================================================
-- Usage: Data integrity check - ensure all doctor_ids exist

-- Check for orphaned patient records (doctor_id doesn't exist in User DB)
SELECT 
    p.id as patient_id,
    p.patient_identifier,
    p.first_name,
    p.last_name,
    p.doctor_id as invalid_doctor_id,
    'Doctor not found in User Management DB' as issue
FROM meditrack_patient_management.patients p
LEFT JOIN meditrack_user_management.users u 
    ON p.doctor_id = u.id
WHERE p.doctor_id IS NOT NULL 
    AND u.id IS NULL
    AND p.is_active = TRUE;

-- Check for orphaned doctor assignments (patient doesn't exist)
SELECT 
    dpa.doctor_id,
    dpa.patient_id as invalid_patient_id,
    'Patient not found in Patient Management DB' as issue
FROM meditrack_patient_management.doctor_patient_assignment dpa
LEFT JOIN meditrack_patient_management.patients p 
    ON dpa.patient_id = p.id
WHERE dpa.assignment_status = 'ACTIVE'
    AND p.id IS NULL;

-- =====================================================
-- 10. GET PATIENT MEDICAL HISTORY
-- =====================================================
-- Usage: Doctor dashboard - complete patient medical record

SELECT 
    mr.id,
    mr.record_type,
    mr.title,
    mr.description,
    mr.diagnosis,
    mr.treatment_plan,
    mr.follow_up_date,
    mr.created_at,
    doctor.first_name as doctor_first_name,
    doctor.last_name as doctor_last_name,
    doctor.specialization
FROM meditrack_patient_management.medical_records mr
INNER JOIN meditrack_user_management.users doctor 
    ON mr.doctor_id = doctor.id
WHERE mr.patient_id = ?
    AND mr.is_confidential = FALSE -- Only show non-confidential records to assigned doctor
ORDER BY mr.created_at DESC;

-- =====================================================
-- 11. GET ACTIVE MEDICATIONS FOR PATIENT
-- =====================================================
-- Usage: Doctor/Nurse dashboard - current medications

SELECT 
    m.id,
    m.medication_name,
    m.dosage,
    m.frequency,
    m.route,
    m.start_date,
    m.end_date,
    m.is_active,
    m.instructions,
    prescriber.first_name as prescribed_by_first_name,
    prescriber.last_name as prescribed_by_last_name
FROM meditrack_patient_management.medications m
INNER JOIN meditrack_user_management.users prescriber 
    ON m.prescribed_by = prescriber.id
WHERE m.patient_id = ?
    AND m.is_active = TRUE
ORDER BY m.start_date DESC;

-- =====================================================
-- 12. GET UPCOMING APPOINTMENTS
-- =====================================================
-- Usage: Doctor/Nurse dashboard - scheduled appointments

SELECT 
    a.id,
    a.appointment_type,
    a.title,
    a.description,
    a.appointment_date,
    a.duration_minutes,
    a.status,
    a.location,
    a.notes,
    p.patient_identifier,
    p.first_name as patient_first_name,
    p.last_name as patient_last_name,
    p.mobile_number,
    p.room_number
FROM meditrack_patient_management.appointments a
INNER JOIN meditrack_patient_management.patients p 
    ON a.patient_id = p.id
WHERE a.doctor_id = ? -- Doctor ID from User Management DB
    AND a.appointment_date >= NOW()
    AND a.status IN ('SCHEDULED', 'CONFIRMED')
ORDER BY a.appointment_date ASC
LIMIT 20;

-- =====================================================
-- 13. GET AUDIT LOGS FOR USER
-- =====================================================
-- Usage: Admin dashboard - user activity tracking

SELECT 
    al.id,
    al.action,
    al.entity_type,
    al.entity_id,
    al.old_values,
    al.new_values,
    al.ip_address,
    al.user_agent,
    al.created_at,
    u.first_name,
    u.last_name,
    u.email
FROM meditrack_user_management.audit_logs al
INNER JOIN meditrack_user_management.users u 
    ON al.user_id = u.id
WHERE al.user_id = ?
    AND al.created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
ORDER BY al.created_at DESC
LIMIT 100;

-- =====================================================
-- 14. GET SYSTEM STATISTICS
-- =====================================================
-- Usage: Admin dashboard - overall system metrics

SELECT 
    'Total Users' as metric_name,
    COUNT(*) as value
FROM meditrack_user_management.users
WHERE is_active = TRUE

UNION ALL

SELECT 
    'Total Admins' as metric_name,
    COUNT(*) as value
FROM meditrack_user_management.admins

UNION ALL

SELECT 
    'Total Doctors' as metric_name,
    COUNT(*) as value
FROM meditrack_user_management.doctors

UNION ALL

SELECT 
    'Total Nurses' as metric_name,
    COUNT(*) as value
FROM meditrack_user_management.nurses

UNION ALL

SELECT 
    'Total Patients' as metric_name,
    COUNT(*) as value
FROM meditrack_patient_management.patients
WHERE is_active = TRUE

UNION ALL

SELECT 
    'Active Cases' as metric_name,
    COUNT(*) as value
FROM meditrack_patient_management.patients
WHERE is_active = TRUE
    AND condition_status IN ('CRITICAL', 'EMERGENCY')

UNION ALL

SELECT 
    'Stable Cases' as metric_name,
    COUNT(*) as value
FROM meditrack_patient_management.patients
WHERE is_active = TRUE
    AND condition_status = 'STABLE'

UNION ALL

SELECT 
    'Active Alerts' as metric_name,
    COUNT(*) as value
FROM meditrack_patient_management.alerts
WHERE status IN ('ACTIVE', 'ACKNOWLEDGED');

-- =====================================================
-- 15. GET AI MODEL PERFORMANCE METRICS
-- =====================================================
-- Usage: AI Model monitoring dashboard

SELECT 
    COUNT(*) as total_predictions,
    COUNT(CASE WHEN a.alert_source = 'AI_PREDICTION' THEN 1 END) as ai_predictions,
    COUNT(CASE WHEN a.status = 'FALSE_POSITIVE' THEN 1 END) as false_positives,
    ROUND(
        (COUNT(CASE WHEN a.status != 'FALSE_POSITIVE' AND a.alert_source = 'AI_PREDICTION' THEN 1 END) * 100.0) / 
        NULLIF(COUNT(CASE WHEN a.alert_source = 'AI_PREDICTION' THEN 1 END), 0), 
        2
    ) as accuracy_percentage,
    DATE_FORMAT(a.created_at, '%Y-%m') as month
FROM meditrack_patient_management.alerts a
WHERE a.created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
GROUP BY DATE_FORMAT(a.created_at, '%Y-%m')
ORDER BY month DESC;
