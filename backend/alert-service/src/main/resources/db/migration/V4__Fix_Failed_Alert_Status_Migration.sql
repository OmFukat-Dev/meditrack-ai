-- Correct FAILED status migration per Phase 1 plan: FAILED -> ESCALATED (not ACTIVE)
UPDATE alerts SET status = 'ESCALATED' WHERE status = 'FAILED';

-- Ensure PENDING maps to ACTIVE (legacy records that may remain)
UPDATE alerts SET status = 'ACTIVE' WHERE status = 'PENDING';

-- Add performance indexes for dashboard queries
CREATE INDEX idx_alert_status ON alerts (status);
CREATE INDEX idx_alert_department ON alerts (department);
CREATE INDEX idx_alert_patient_department ON alerts (patient_id, department);
