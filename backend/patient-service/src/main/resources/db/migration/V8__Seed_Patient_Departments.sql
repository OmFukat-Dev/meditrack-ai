-- Seed department values using the stable string identifier. The primary key is BIGINT.
UPDATE patients SET department = 'Cardiology' WHERE patient_identifier IN ('PT-001', 'PT-002', 'PT-003', 'PT-004', 'PT-005');
UPDATE patients SET department = 'Pediatrics' WHERE patient_identifier IN ('PT-006', 'PT-007', 'PT-008', 'PT-009', 'PT-010');
UPDATE patients SET department = 'Neurology' WHERE patient_identifier IN ('PT-011', 'PT-012', 'PT-013', 'PT-014', 'PT-015');
UPDATE patients SET department = 'Oncology' WHERE patient_identifier IN ('PT-016', 'PT-017', 'PT-018', 'PT-019', 'PT-020');
UPDATE patients SET department = 'Orthopedics' WHERE patient_identifier IN ('PT-021', 'PT-022', 'PT-023', 'PT-024', 'PT-025');
