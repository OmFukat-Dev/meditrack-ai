-- Add department ownership column to AI predictions
ALTER TABLE ai_predictions ADD COLUMN department VARCHAR(100) NULL AFTER patient_id;

UPDATE ai_predictions SET department = 'General' WHERE department IS NULL;

ALTER TABLE ai_predictions MODIFY COLUMN department VARCHAR(100) NOT NULL;

CREATE INDEX idx_ai_predictions_department ON ai_predictions (department);
CREATE INDEX idx_ai_predictions_patient_department ON ai_predictions (patient_id, department);
