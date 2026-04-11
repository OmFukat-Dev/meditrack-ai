-- MediTrack AI - AI Prediction Service Database Schema
-- Version 1.0

-- AI predictions table
CREATE TABLE ai_predictions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    prediction_type VARCHAR(50) NOT NULL, -- RISK_SCORE, DETERIORATION, SEPSIS, CARDIAC
    prediction_timestamp TIMESTAMP NOT NULL,
    risk_score DECIMAL(5,2) NOT NULL, -- 0.00 to 100.00
    risk_level VARCHAR(20) NOT NULL, -- LOW, MEDIUM, HIGH, CRITICAL
    confidence_score DECIMAL(5,2) NOT NULL, -- 0.00 to 100.00
    model_version VARCHAR(20) NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    input_data JSON NOT NULL, -- Features used for prediction
    feature_importance JSON, -- SHAP-like explainability
    prediction_details JSON, -- Additional prediction context
    is_verified BOOLEAN DEFAULT FALSE,
    verified_by VARCHAR(50),
    verified_at TIMESTAMP,
    verification_notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    INDEX idx_patient_prediction_time (patient_id, prediction_timestamp),
    INDEX idx_risk_level (risk_level),
    INDEX idx_prediction_type (prediction_type),
    INDEX idx_model_version (model_version)
);

-- NEWS scoring table (National Early Warning Score)
CREATE TABLE news_scores (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    score_timestamp TIMESTAMP NOT NULL,
    total_score INT NOT NULL,
    respiratory_rate_score INT,
    oxygen_saturation_score INT,
    supplemental_oxygen_score INT,
    temperature_score INT,
    systolic_bp_score INT,
    heart_rate_score INT,
    consciousness_score INT,
    respiratory_rate_value INT,
    oxygen_saturation_value INT,
    supplemental_oxygen BOOLEAN,
    temperature_value DECIMAL(4,1),
    systolic_bp_value INT,
    heart_rate_value INT,
    consciousness_level VARCHAR(20), -- ALERT, VOICE, PAIN, UNRESPONSIVE
    calculated_by VARCHAR(50) NOT NULL, -- SYSTEM, NURSE, DOCTOR
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    INDEX idx_patient_news_time (patient_id, score_timestamp),
    INDEX idx_news_total_score (total_score),
    INDEX idx_news_calculated_by (calculated_by)
);

-- Model performance metrics table
CREATE TABLE model_performance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    model_name VARCHAR(100) NOT NULL,
    model_version VARCHAR(20) NOT NULL,
    evaluation_date DATE NOT NULL,
    accuracy DECIMAL(5,2), -- 0.00 to 100.00
    precision_score DECIMAL(5,2),
    recall_score DECIMAL(5,2),
    f1_score DECIMAL(5,2),
    roc_auc DECIMAL(5,2),
    true_positives INT,
    false_positives INT,
    true_negatives INT,
    false_negatives INT,
    total_predictions INT,
    positive_threshold DECIMAL(5,2),
    evaluation_dataset VARCHAR(100),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_model_version_date (model_name, model_version, evaluation_date),
    INDEX idx_model_performance (model_name, evaluation_date)
);

-- Feature importance tracking table
CREATE TABLE feature_importance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    model_name VARCHAR(100) NOT NULL,
    model_version VARCHAR(20) NOT NULL,
    feature_name VARCHAR(100) NOT NULL,
    importance_value DECIMAL(10,6) NOT NULL,
    importance_type VARCHAR(50) NOT NULL, -- SHAP, PERMUTATION, GAIN
    calculation_date DATE NOT NULL,
    sample_size INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_feature_model (model_name, model_version),
    INDEX idx_feature_importance (importance_value DESC),
    INDEX idx_feature_calculation_date (calculation_date)
);

-- Model drift detection table
CREATE TABLE model_drift (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    model_name VARCHAR(100) NOT NULL,
    model_version VARCHAR(20) NOT NULL,
    drift_timestamp TIMESTAMP NOT NULL,
    drift_type VARCHAR(50) NOT NULL, -- DATA_DRIFT, CONCEPT_DRIFT, PERFORMANCE_DRIFT
    drift_score DECIMAL(5,2) NOT NULL, -- 0.00 to 100.00
    drift_threshold DECIMAL(5,2) NOT NULL,
    is_drift_detected BOOLEAN NOT NULL,
    baseline_period_start DATE,
    baseline_period_end DATE,
    comparison_period_start DATE,
    comparison_period_end DATE,
    drift_features JSON, -- Features that contributed to drift
    recommended_action VARCHAR(100), -- RETRAIN, MONITOR, IGNORE
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_drift_model_timestamp (model_name, drift_timestamp),
    INDEX idx_drift_detected (is_drift_detected, drift_timestamp)
);

-- Prediction alerts table
CREATE TABLE prediction_alerts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    prediction_id BIGINT NOT NULL,
    alert_type VARCHAR(50) NOT NULL, -- HIGH_RISK, CRITICAL, DRIFT_DETECTED
    alert_message TEXT NOT NULL,
    alert_severity VARCHAR(20) NOT NULL, -- LOW, MEDIUM, HIGH, CRITICAL
    is_acknowledged BOOLEAN DEFAULT FALSE,
    acknowledged_by VARCHAR(50),
    acknowledged_at TIMESTAMP,
    is_resolved BOOLEAN DEFAULT FALSE,
    resolved_by VARCHAR(50),
    resolved_at TIMESTAMP,
    resolution_notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (prediction_id) REFERENCES ai_predictions(id) ON DELETE CASCADE,
    INDEX idx_alert_prediction (prediction_id),
    INDEX idx_alert_severity (alert_severity),
    INDEX idx_alert_acknowledged (is_acknowledged, is_resolved)
);
