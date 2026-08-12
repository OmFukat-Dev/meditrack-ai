-- Standardize Alert Statuses Migration
-- Maps existing legacy Java-side statuses to the standardized lifecycle:
-- ACTIVE, ACKNOWLEDGED, IN_PROGRESS, RESOLVED, ESCALATED

-- Map PENDING and FAILED back to ACTIVE
UPDATE alerts SET status = 'ACTIVE' WHERE status IN ('PENDING', 'FAILED', 'ACTIVE');

-- Map PROCESSING to IN_PROGRESS
UPDATE alerts SET status = 'IN_PROGRESS' WHERE status = 'PROCESSING';

-- Map PROCESSED to RESOLVED
UPDATE alerts SET status = 'RESOLVED' WHERE status = 'PROCESSED';

-- Modify column size if necessary to support future status additions
ALTER TABLE alerts MODIFY COLUMN status VARCHAR(30) DEFAULT 'ACTIVE';
