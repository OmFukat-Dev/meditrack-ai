package com.meditrack.ai.dto;

import java.time.LocalDateTime;
import java.util.List;

public class NewsScoreResult {
    private String patientId;
    private int newsScore;
    private String riskLevel;
    private String confidence;
    private LocalDateTime timestamp;
    private List<String> recommendations;
    private String message;
    private String severity;
    
    // Constructors
    public NewsScoreResult() {}
    
    public NewsScoreResult(String patientId, int newsScore, String riskLevel, String confidence) {
        this.patientId = patientId;
        this.newsScore = newsScore;
        this.riskLevel = riskLevel;
        this.confidence = confidence;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    
    public int getNewsScore() { return newsScore; }
    public void setNewsScore(int newsScore) { this.newsScore = newsScore; }
    
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    
    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public List<String> getRecommendations() { return recommendations; }
    public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
}
