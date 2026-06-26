-- Seed department values for the patients
UPDATE patients SET department = 'Cardiology' WHERE id IN ('patient-1', 'patient-2', 'patient-3', 'patient-4', 'patient-5');
UPDATE patients SET department = 'Pediatrics' WHERE id IN ('patient-6', 'patient-7', 'patient-8', 'patient-9', 'patient-10');
UPDATE patients SET department = 'Neurology' WHERE id IN ('patient-11', 'patient-12', 'patient-13', 'patient-14', 'patient-15');
UPDATE patients SET department = 'Oncology' WHERE id IN ('patient-16', 'patient-17', 'patient-18', 'patient-19', 'patient-20');
UPDATE patients SET department = 'Orthopedics' WHERE id IN ('patient-21', 'patient-22', 'patient-23', 'patient-24', 'patient-25');
