-- Standardize Alert Statuses Migration
-- Maps existing legacy Java-side statuses to the standardized lifecycle:
-- ACTIVE, ACKNOWLEDGED, IN_PROGRESS, RESOLVED, ESCALATED

-- Map PENDING and FAILED back to ACTIVE
UPDATE alerts SET alert_status = 'ACTIVE' WHERE alert_status IN ('PENDING', 'FAILED', 'ACTIVE');

-- Map PROCESSING to IN_PROGRESS
UPDATE alerts SET alert_status = 'IN_PROGRESS' WHERE alert_status = 'PROCESSING';

-- Map PROCESSED to RESOLVED
UPDATE alerts SET alert_status = 'RESOLVED' WHERE alert_status = 'PROCESSED';

-- Modify column size if necessary to support future status additions (e.g. IN_PROGRESS fits in 20, but let's make it 30 for safety)
ALTER TABLE alerts MODIFY COLUMN alert_status VARCHAR(30) DEFAULT 'ACTIVE';
