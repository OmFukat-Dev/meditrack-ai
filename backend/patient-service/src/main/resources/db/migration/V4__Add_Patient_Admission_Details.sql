ALTER TABLE patients
    ADD COLUMN department VARCHAR(100) NULL AFTER viewer_email,
    ADD COLUMN ward_number VARCHAR(50) NULL AFTER department,
    ADD COLUMN bed_number VARCHAR(50) NULL AFTER ward_number;

CREATE INDEX idx_patients_department ON patients (department);
CREATE INDEX idx_patients_ward_number ON patients (ward_number);
CREATE INDEX idx_patients_bed_number ON patients (bed_number);
CREATE INDEX idx_patients_department_active ON patients (department, is_active);
