package com.meditrack.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class NewsScoringService {
    
    private static final Logger logger = LoggerFactory.getLogger(NewsScoringService.class);
    
    // NEWS scoring parameters
    private static final Map<String, Map<String, Integer>> NEWS_SCORES = Map.of(
        "respiratory_rate", Map.of(
            "normal", 0,    // 8-24 breaths/min
            "low", 3,        // < 8 breaths/min
            "high", 2         // > 24 breaths/min
        ),
        "oxygen_saturation", Map.of(
            "normal", 0,    // >= 96%
            "mild_low", 1,   // 94-95%
            "moderate_low", 2, // 92-93%
            "severe_low", 3  // < 92%
        ),
        "supplemental_oxygen", Map.of(
            "no", 0,         // No oxygen
            "yes", 2          // Any oxygen
        ),
        "temperature", Map.of(
            "normal", 0,     // 36.1-37.2°C
            "low", 2,        // < 35.0°C
            "mild_low", 1,    // 35.0-36.0°C
            "mild_high", 1,   // 37.3-38.0°C
            "high", 2         // > 38.0°C
        ),
        "systolic_bp", Map.of(
            "normal", 0,     // 111-219 mmHg
            "low", 3,        // <= 90 mmHg
            "mild_low", 2,   // 91-100 mmHg
            "high", 2        // >= 220 mmHg
        ),
        "heart_rate", Map.of(
            "normal", 0,     // 51-90 bpm
            "low", 3,        // <= 40 bpm
            "mild_low", 2,   // 41-50 bpm
            "mild_high", 1,   // 91-110 bpm
            "high", 3        // >= 111 bpm
        ),
        "consciousness", Map.of(
            "alert", 0,      // Alert
            "voice", 1,      // Responds to voice
            "pain", 2,       // Responds to pain
            "unresponsive", 3 // Unresponsive
        )
    );
    
    // Calculate NEWS score for a patient
    public NewsScore calculateNewsScore(PatientVitals vitals) {
        try {
            logger.debug("Calculating NEWS score for patient: {}", vitals.getPatientId());
            
            NewsScore newsScore = new NewsScore();
            newsScore.setPatientId(vitals.getPatientId());
            newsScore.setTimestamp(LocalDateTime.now());
            
            // Calculate individual vital scores
            VitalScore respiratoryScore = calculateRespiratoryRateScore(vitals.getRespiratoryRate());
            VitalScore oxygenScore = calculateOxygenSaturationScore(vitals.getOxygenSaturation(), vitals.isSupplementalOxygen());
            VitalScore temperatureScore = calculateTemperatureScore(vitals.getTemperature());
            VitalScore systolicBpScore = calculateSystolicBpScore(vitals.getSystolicBp());
            VitalScore heartRateScore = calculateHeartRateScore(vitals.getHeartRate());
            VitalScore consciousnessScore = calculateConsciousnessScore(vitals.getConsciousnessLevel());
            
            // Set individual scores
            newsScore.setRespiratoryRateScore(respiratoryScore);
            newsScore.setOxygenSaturationScore(oxygenScore);
            newsScore.setTemperatureScore(temperatureScore);
            newsScore.setSystolicBpScore(systolicBpScore);
            newsScore.setHeartRateScore(heartRateScore);
            newsScore.setConsciousnessScore(consciousnessScore);
            
            // Calculate total score
            int totalScore = respiratoryScore.getScore() + oxygenScore.getScore() + 
                           temperatureScore.getScore() + systolicBpScore.getScore() + 
                           heartRateScore.getScore() + consciousnessScore.getScore();
            
            newsScore.setTotalScore(totalScore);
            newsScore.setRiskLevel(determineRiskLevel(totalScore));
            
            // Calculate trend scores if historical data available
            if (vitals.getPreviousVitals() != null && !vitals.getPreviousVitals().isEmpty()) {
                TrendScore trendScore = calculateTrendScore(vitals);
                newsScore.setTrendScore(trendScore);
                newsScore.setTotalScoreWithTrend(totalScore + trendScore.getScore());
                newsScore.setRiskLevelWithTrend(determineRiskLevel(totalScore + trendScore.getScore()));
            }
            
            logger.info("NEWS score calculated: patientId={}, totalScore={}, riskLevel={}", 
                       vitals.getPatientId(), totalScore, newsScore.getRiskLevel());
            
            return newsScore;
            
        } catch (Exception e) {
            logger.error("Error calculating NEWS score: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to calculate NEWS score", e);
        }
    }
    
    // Calculate respiratory rate score
    private VitalScore calculateRespiratoryRateScore(BigDecimal respiratoryRate) {
        if (respiratoryRate == null) {
            return new VitalScore("respiratory_rate", 0, "normal", "No data available");
        }
        
        double rate = respiratoryRate.doubleValue();
        String category;
        int score;
        String description;
        
        if (rate < 8) {
            category = "low";
            score = 3;
            description = "Respiratory rate critically low (< 8 breaths/min)";
        } else if (rate <= 24) {
            category = "normal";
            score = 0;
            description = "Respiratory rate normal (8-24 breaths/min)";
        } else {
            category = "high";
            score = 2;
            description = "Respiratory rate elevated (> 24 breaths/min)";
        }
        
        return new VitalScore("respiratory_rate", score, category, description);
    }
    
    // Calculate oxygen saturation score
    private VitalScore calculateOxygenSaturationScore(BigDecimal oxygenSaturation, boolean supplementalOxygen) {
        // First check supplemental oxygen
        if (supplementalOxygen) {
            return new VitalScore("supplemental_oxygen", 2, "yes", "Patient on supplemental oxygen");
        }
        
        if (oxygenSaturation == null) {
            return new VitalScore("oxygen_saturation", 0, "normal", "No data available");
        }
        
        double saturation = oxygenSaturation.doubleValue();
        String category;
        int score;
        String description;
        
        if (saturation < 92) {
            category = "severe_low";
            score = 3;
            description = "Oxygen saturation severely low (< 92%)";
        } else if (saturation <= 93) {
            category = "moderate_low";
            score = 2;
            description = "Oxygen saturation moderately low (92-93%)";
        } else if (saturation <= 95) {
            category = "mild_low";
            score = 1;
            description = "Oxygen saturation mildly low (94-95%)";
        } else {
            category = "normal";
            score = 0;
            description = "Oxygen saturation normal (>= 96%)";
        }
        
        return new VitalScore("oxygen_saturation", score, category, description);
    }
    
    // Calculate temperature score
    private VitalScore calculateTemperatureScore(BigDecimal temperature) {
        if (temperature == null) {
            return new VitalScore("temperature", 0, "normal", "No data available");
        }
        
        double temp = temperature.doubleValue();
        String category;
        int score;
        String description;
        
        if (temp < 35.0) {
            category = "low";
            score = 3;
            description = "Temperature critically low (< 35.0°C)";
        } else if (temp <= 36.0) {
            category = "mild_low";
            score = 1;
            description = "Temperature mildly low (35.0-36.0°C)";
        } else if (temp <= 37.2) {
            category = "normal";
            score = 0;
            description = "Temperature normal (36.1-37.2°C)";
        } else if (temp <= 38.0) {
            category = "mild_high";
            score = 1;
            description = "Temperature mildly elevated (37.3-38.0°C)";
        } else {
            category = "high";
            score = 2;
            description = "Temperature elevated (> 38.0°C)";
        }
        
        return new VitalScore("temperature", score, category, description);
    }
    
    // Calculate systolic blood pressure score
    private VitalScore calculateSystolicBpScore(BigDecimal systolicBp) {
        if (systolicBp == null) {
            return new VitalScore("systolic_bp", 0, "normal", "No data available");
        }
        
        double bp = systolicBp.doubleValue();
        String category;
        int score;
        String description;
        
        if (bp <= 90) {
            category = "low";
            score = 3;
            description = "Systolic BP critically low (<= 90 mmHg)";
        } else if (bp <= 100) {
            category = "mild_low";
            score = 2;
            description = "Systolic BP low (91-100 mmHg)";
        } else if (bp <= 219) {
            category = "normal";
            score = 0;
            description = "Systolic BP normal (101-219 mmHg)";
        } else {
            category = "high";
            score = 2;
            description = "Systolic BP elevated (>= 220 mmHg)";
        }
        
        return new VitalScore("systolic_bp", score, category, description);
    }
    
    // Calculate heart rate score
    private VitalScore calculateHeartRateScore(BigDecimal heartRate) {
        if (heartRate == null) {
            return new VitalScore("heart_rate", 0, "normal", "No data available");
        }
        
        double hr = heartRate.doubleValue();
        String category;
        int score;
        String description;
        
        if (hr <= 40) {
            category = "low";
            score = 3;
            description = "Heart rate critically low (<= 40 bpm)";
        } else if (hr <= 50) {
            category = "mild_low";
            score = 2;
            description = "Heart rate low (41-50 bpm)";
        } else if (hr <= 90) {
            category = "normal";
            score = 0;
            description = "Heart rate normal (51-90 bpm)";
        } else if (hr <= 110) {
            category = "mild_high";
            score = 1;
            description = "Heart rate mildly elevated (91-110 bpm)";
        } else {
            category = "high";
            score = 3;
            description = "Heart rate elevated (> 110 bpm)";
        }
        
        return new VitalScore("heart_rate", score, category, description);
    }
    
    // Calculate consciousness score
    private VitalScore calculateConsciousnessScore(String consciousnessLevel) {
        if (consciousnessLevel == null) {
            return new VitalScore("consciousness", 0, "alert", "Assumed alert - no data available");
        }
        
        String category = consciousnessLevel.toLowerCase();
        int score;
        String description;
        
        switch (category) {
            case "alert":
                score = 0;
                description = "Patient alert and responsive";
                break;
            case "voice":
            case "verbal":
                score = 1;
                description = "Patient responds to voice";
                break;
            case "pain":
            case "painful":
                score = 2;
                description = "Patient responds to pain";
                break;
            case "unresponsive":
            case "unconscious":
                score = 3;
                description = "Patient unresponsive";
                break;
            default:
                score = 0;
                description = "Unknown consciousness level - assumed alert";
                category = "alert";
                break;
        }
        
        return new VitalScore("consciousness", score, category, description);
    }
    
    // Calculate trend score based on historical data
    private TrendScore calculateTrendScore(PatientVitals vitals) {
        try {
            TrendScore trendScore = new TrendScore();
            List<PatientVitals> previousVitals = vitals.getPreviousVitals();
            
            // Get most recent vital for comparison
            PatientVitals previous = previousVitals.get(previousVitals.size() - 1);
            
            // Calculate rate of change for each vital
            double respiratoryChange = calculateRateOfChange(previous.getRespiratoryRate(), vitals.getRespiratoryRate());
            double oxygenChange = calculateRateOfChange(previous.getOxygenSaturation(), vitals.getOxygenSaturation());
            double temperatureChange = calculateRateOfChange(previous.getTemperature(), vitals.getTemperature());
            double systolicChange = calculateRateOfChange(previous.getSystolicBp(), vitals.getSystolicBp());
            double heartRateChange = calculateRateOfChange(previous.getHeartRate(), vitals.getHeartRate());
            
            // Determine trend scores
            int respiratoryTrend = calculateTrendScoreValue("respiratory_rate", respiratoryChange);
            int oxygenTrend = calculateTrendScoreValue("oxygen_saturation", oxygenChange);
            int temperatureTrend = calculateTrendScoreValue("temperature", temperatureChange);
            int systolicTrend = calculateTrendScoreValue("systolic_bp", systolicChange);
            int heartRateTrend = calculateTrendScoreValue("heart_rate", heartRateChange);
            
            trendScore.setRespiratoryRateTrend(respiratoryTrend);
            trendScore.setOxygenSaturationTrend(oxygenTrend);
            trendScore.setTemperatureTrend(temperatureTrend);
            trendScore.setSystolicBpTrend(systolicTrend);
            trendScore.setHeartRateTrend(heartRateTrend);
            
            int totalTrendScore = respiratoryTrend + oxygenTrend + temperatureTrend + systolicTrend + heartRateTrend;
            trendScore.setTotalTrendScore(totalTrendScore);
            trendScore.setTrendSeverity(determineTrendSeverity(totalTrendScore));
            
            return trendScore;
            
        } catch (Exception e) {
            logger.error("Error calculating trend score: {}", e.getMessage(), e);
            return new TrendScore(); // Return empty trend score
        }
    }
    
    // Calculate rate of change between two values
    private double calculateRateOfChange(BigDecimal previous, BigDecimal current) {
        if (previous == null || current == null) {
            return 0.0;
        }
        
        double prev = previous.doubleValue();
        double curr = current.doubleValue();
        
        if (prev == 0) {
            return curr > 0 ? Double.MAX_VALUE : 0.0;
        }
        
        return ((curr - prev) / prev) * 100.0; // Percentage change
    }
    
    // Calculate trend score value based on rate of change
    private int calculateTrendScoreValue(String vitalType, double rateOfChange) {
        // Define thresholds for significant changes (percentage)
        Map<String, Map<String, Double>> trendThresholds = Map.of(
            "respiratory_rate", Map.of("high", 25.0, "low", -20.0),
            "oxygen_saturation", Map.of("high", 5.0, "low", -3.0),
            "temperature", Map.of("high", 2.0, "low", -1.5),
            "systolic_bp", Map.of("high", 15.0, "low", -10.0),
            "heart_rate", Map.of("high", 20.0, "low", -15.0)
        );
        
        Map<String, Double> thresholds = trendThresholds.getOrDefault(vitalType, Map.of("high", 10.0, "low", -10.0));
        
        if (rateOfChange >= thresholds.get("high")) {
            return 2; // Significant increase
        } else if (rateOfChange <= thresholds.get("low")) {
            return 2; // Significant decrease
        } else if (Math.abs(rateOfChange) >= 5.0) {
            return 1; // Moderate change
        } else {
            return 0; // Stable
        }
    }
    
    // Determine risk level based on total score
    private String determineRiskLevel(int totalScore) {
        if (totalScore <= 4) {
            return "LOW";
        } else if (totalScore <= 6) {
            return "MEDIUM";
        } else if (totalScore <= 7) {
            return "HIGH";
        } else {
            return "CRITICAL";
        }
    }
    
    // Determine trend severity
    private String determineTrendSeverity(int trendScore) {
        if (trendScore <= 2) {
            return "STABLE";
        } else if (trendScore <= 4) {
            return "CHANGING";
        } else {
            return "RAPIDLY_CHANGING";
        }
    }
    
    // Get NEWS scoring parameters
    public Map<String, Map<String, Integer>> getNewsScoringParameters() {
        return new HashMap<>(NEWS_SCORES);
    }
    
    // Inner classes
    public static class PatientVitals {
        private String patientId;
        private BigDecimal respiratoryRate;
        private BigDecimal oxygenSaturation;
        private boolean supplementalOxygen;
        private BigDecimal temperature;
        private BigDecimal systolicBp;
        private BigDecimal heartRate;
        private String consciousnessLevel;
        private List<PatientVitals> previousVitals;
        
        // Getters and setters
        public String getPatientId() { return patientId; }
        public void setPatientId(String patientId) { this.patientId = patientId; }
        
        public BigDecimal getRespiratoryRate() { return respiratoryRate; }
        public void setRespiratoryRate(BigDecimal respiratoryRate) { this.respiratoryRate = respiratoryRate; }
        
        public BigDecimal getOxygenSaturation() { return oxygenSaturation; }
        public void setOxygenSaturation(BigDecimal oxygenSaturation) { this.oxygenSaturation = oxygenSaturation; }
        
        public boolean isSupplementalOxygen() { return supplementalOxygen; }
        public void setSupplementalOxygen(boolean supplementalOxygen) { this.supplementalOxygen = supplementalOxygen; }
        
        public BigDecimal getTemperature() { return temperature; }
        public void setTemperature(BigDecimal temperature) { this.temperature = temperature; }
        
        public BigDecimal getSystolicBp() { return systolicBp; }
        public void setSystolicBp(BigDecimal systolicBp) { this.systolicBp = systolicBp; }
        
        public BigDecimal getHeartRate() { return heartRate; }
        public void setHeartRate(BigDecimal heartRate) { this.heartRate = heartRate; }
        
        public String getConsciousnessLevel() { return consciousnessLevel; }
        public void setConsciousnessLevel(String consciousnessLevel) { this.consciousnessLevel = consciousnessLevel; }
        
        public List<PatientVitals> getPreviousVitals() { return previousVitals; }
        public void setPreviousVitals(List<PatientVitals> previousVitals) { this.previousVitals = previousVitals; }
    }
    
    public static class NewsScore {
        private String patientId;
        private LocalDateTime timestamp;
        private VitalScore respiratoryRateScore;
        private VitalScore oxygenSaturationScore;
        private VitalScore temperatureScore;
        private VitalScore systolicBpScore;
        private VitalScore heartRateScore;
        private VitalScore consciousnessScore;
        private int totalScore;
        private String riskLevel;
        private TrendScore trendScore;
        private int totalScoreWithTrend;
        private String riskLevelWithTrend;
        
        // Getters and setters
        public String getPatientId() { return patientId; }
        public void setPatientId(String patientId) { this.patientId = patientId; }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        
        public VitalScore getRespiratoryRateScore() { return respiratoryRateScore; }
        public void setRespiratoryRateScore(VitalScore respiratoryRateScore) { this.respiratoryRateScore = respiratoryRateScore; }
        
        public VitalScore getOxygenSaturationScore() { return oxygenSaturationScore; }
        public void setOxygenSaturationScore(VitalScore oxygenSaturationScore) { this.oxygenSaturationScore = oxygenSaturationScore; }
        
        public VitalScore getTemperatureScore() { return temperatureScore; }
        public void setTemperatureScore(VitalScore temperatureScore) { this.temperatureScore = temperatureScore; }
        
        public VitalScore getSystolicBpScore() { return systolicBpScore; }
        public void setSystolicBpScore(VitalScore systolicBpScore) { this.systolicBpScore = systolicBpScore; }
        
        public VitalScore getHeartRateScore() { return heartRateScore; }
        public void setHeartRateScore(VitalScore heartRateScore) { this.heartRateScore = heartRateScore; }
        
        public VitalScore getConsciousnessScore() { return consciousnessScore; }
        public void setConsciousnessScore(VitalScore consciousnessScore) { this.consciousnessScore = consciousnessScore; }
        
        public int getTotalScore() { return totalScore; }
        public void setTotalScore(int totalScore) { this.totalScore = totalScore; }
        
        public String getRiskLevel() { return riskLevel; }
        public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
        
        public TrendScore getTrendScore() { return trendScore; }
        public void setTrendScore(TrendScore trendScore) { this.trendScore = trendScore; }
        
        public int getTotalScoreWithTrend() { return totalScoreWithTrend; }
        public void setTotalScoreWithTrend(int totalScoreWithTrend) { this.totalScoreWithTrend = totalScoreWithTrend; }
        
        public String getRiskLevelWithTrend() { return riskLevelWithTrend; }
        public void setRiskLevelWithTrend(String riskLevelWithTrend) { this.riskLevelWithTrend = riskLevelWithTrend; }
    }
    
    public static class VitalScore {
        private String vitalType;
        private int score;
        private String category;
        private String description;
        
        public VitalScore(String vitalType, int score, String category, String description) {
            this.vitalType = vitalType;
            this.score = score;
            this.category = category;
            this.description = description;
        }
        
        // Getters
        public String getVitalType() { return vitalType; }
        public int getScore() { return score; }
        public String getCategory() { return category; }
        public String getDescription() { return description; }
    }
    
    public static class TrendScore {
        private int respiratoryRateTrend;
        private int oxygenSaturationTrend;
        private int temperatureTrend;
        private int systolicBpTrend;
        private int heartRateTrend;
        private int totalTrendScore;
        private String trendSeverity;
        
        // Getters and setters
        public int getRespiratoryRateTrend() { return respiratoryRateTrend; }
        public void setRespiratoryRateTrend(int respiratoryRateTrend) { this.respiratoryRateTrend = respiratoryRateTrend; }
        
        public int getOxygenSaturationTrend() { return oxygenSaturationTrend; }
        public void setOxygenSaturationTrend(int oxygenSaturationTrend) { this.oxygenSaturationTrend = oxygenSaturationTrend; }
        
        public int getTemperatureTrend() { return temperatureTrend; }
        public void setTemperatureTrend(int temperatureTrend) { this.temperatureTrend = temperatureTrend; }
        
        public int getSystolicBpTrend() { return systolicBpTrend; }
        public void setSystolicBpTrend(int systolicBpTrend) { this.systolicBpTrend = systolicBpTrend; }
        
        public int getHeartRateTrend() { return heartRateTrend; }
        public void setHeartRateTrend(int heartRateTrend) { this.heartRateTrend = heartRateTrend; }
        
        public int getTotalTrendScore() { return totalTrendScore; }
        public void setTotalTrendScore(int totalTrendScore) { this.totalTrendScore = totalTrendScore; }
        
        public String getTrendSeverity() { return trendSeverity; }
        public void setTrendSeverity(String trendSeverity) { this.trendSeverity = trendSeverity; }
    }
}
