ALTER TABLE patients
    ADD COLUMN assigned_clinician_name VARCHAR(100) NULL AFTER updated_by,
    ADD COLUMN assigned_clinician_email VARCHAR(120) NULL AFTER assigned_clinician_name,
    ADD COLUMN viewer_email VARCHAR(120) NULL AFTER assigned_clinician_email;

CREATE INDEX idx_patients_assigned_clinician_email ON patients (assigned_clinician_email);
CREATE INDEX idx_patients_viewer_email ON patients (viewer_email);
CREATE INDEX idx_patients_assigned_clinician_active ON patients (assigned_clinician_email, is_active);
CREATE INDEX idx_patients_viewer_active ON patients (viewer_email, is_active);
