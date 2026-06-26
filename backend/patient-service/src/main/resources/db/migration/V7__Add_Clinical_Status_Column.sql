-- Add clinical_status column to patients table
ALTER TABLE patients ADD COLUMN clinical_status VARCHAR(50) DEFAULT 'Stable';
