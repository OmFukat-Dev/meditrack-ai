-- Correct FAILED status migration per Phase 1 plan: FAILED -> ESCALATED (not ACTIVE)
UPDATE alerts SET alert_status = 'ESCALATED' WHERE alert_status = 'FAILED';

-- Ensure PENDING maps to ACTIVE (legacy records that may remain)
UPDATE alerts SET alert_status = 'ACTIVE' WHERE alert_status = 'PENDING';

-- Add performance indexes for dashboard queries
CREATE INDEX idx_alert_status ON alerts (alert_status);
CREATE INDEX idx_alert_department ON alerts (department);
CREATE INDEX idx_alert_patient_department ON alerts (patient_id, department);
