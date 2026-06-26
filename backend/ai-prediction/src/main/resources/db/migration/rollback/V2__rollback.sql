-- Rollback for V2__Add_Prediction_Department_Column.sql
DROP INDEX IF EXISTS idx_ai_predictions_department ON ai_predictions;
DROP INDEX IF EXISTS idx_ai_predictions_patient_department ON ai_predictions;
ALTER TABLE ai_predictions DROP COLUMN department;
