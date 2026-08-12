-- Rollback for V4__Fix_Failed_Alert_Status_Migration.sql
-- Reverts ESCALATED alerts that originated from FAILED back to FAILED
-- Note: only safe when no new ESCALATED alerts were created after migration

DROP INDEX IF EXISTS idx_alert_status ON alerts;
DROP INDEX IF EXISTS idx_alert_department ON alerts;
DROP INDEX IF EXISTS idx_alert_patient_department ON alerts;

UPDATE alerts SET alert_status = 'FAILED' WHERE alert_status = 'ESCALATED' AND escalation_level IS NULL;
