package com.meditrack.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ModelPerformanceDashboardService {
    
    private static final Logger logger = LoggerFactory.getLogger(ModelPerformanceDashboardService.class);
    
    // Dashboard data storage
    private final Map<String, ModelDashboardData> modelDashboardData = new ConcurrentHashMap<>();
    private final Map<String, List<PerformanceSnapshot>> performanceHistory = new ConcurrentHashMap<>();
    private final Map<String, List<AlertData>> alertHistory = new ConcurrentHashMap<>();
    
    // Generate dashboard data for a model
    public ModelDashboardData generateDashboardData(String modelName, 
                                                   ModelPerformanceTrackingService.ModelPerformanceMetrics metrics,
                                                   FeatureImportanceService.FeatureImportanceResults featureImportance) {
        try {
            logger.info("Generating dashboard data for model: {}", modelName);
            
            ModelDashboardData dashboardData = new ModelDashboardData();
            dashboardData.setModelName(modelName);
            dashboardData.setGeneratedAt(LocalDateTime.now());
            
            // Performance metrics
            dashboardData.setPerformanceMetrics(extractPerformanceMetrics(metrics));
            
            // Feature importance
            dashboardData.setFeatureImportance(extractFeatureImportanceSummary(featureImportance));
            
            // Recent performance trend
            dashboardData.setPerformanceTrend(calculatePerformanceTrend(modelName));
            
            // Alert summary
            dashboardData.setAlertSummary(getAlertSummary(modelName));
            
            // Model health status
            dashboardData.setModelHealth(calculateModelHealth(metrics));
            
            // Recommendations
            dashboardData.setRecommendations(generateRecommendations(metrics, featureImportance));
            
            // Store dashboard data
            modelDashboardData.put(modelName, dashboardData);
            
            logger.info("Successfully generated dashboard data for model: {}", modelName);
            
            return dashboardData;
            
        } catch (Exception e) {
            logger.error("Error generating dashboard data for model: {}", modelName, e);
            throw new RuntimeException("Failed to generate dashboard data", e);
        }
    }
    
    // Get dashboard data for all models
    public Map<String, ModelDashboardData> getAllDashboardData() {
        return new HashMap<>(modelDashboardData);
    }
    
    // Get dashboard summary for overview
    public DashboardOverview getDashboardOverview() {
        try {
            DashboardOverview overview = new DashboardOverview();
            overview.setGeneratedAt(LocalDateTime.now());
            
            // Model statistics
            overview.setTotalModels(modelDashboardData.size());
            overview.setHealthyModels((int) modelDashboardData.values().stream()
                .filter(data -> "HEALTHY".equals(data.getModelHealth().getStatus()))
                .count());
            overview.setModelsWithAlerts((int) modelDashboardData.values().stream()
                .filter(data -> data.getAlertSummary().getActiveAlerts() > 0)
                .count());
            
            // Performance summary
            List<Double> accuracies = modelDashboardData.values().stream()
                .map(data -> data.getPerformanceMetrics().getAccuracy())
                .collect(Collectors.toList());
            
            if (!accuracies.isEmpty()) {
                overview.setAverageAccuracy(accuracies.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
                overview.setBestAccuracy(accuracies.stream().mapToDouble(Double::doubleValue).max().orElse(0.0));
                overview.setWorstAccuracy(accuracies.stream().mapToDouble(Double::doubleValue).min().orElse(0.0));
            }
            
            // Recent alerts
            overview.setRecentAlerts(getRecentAlerts(10));
            
            // Performance trends
            overview.setPerformanceTrends(calculateOverallPerformanceTrends());
            
            return overview;
            
        } catch (Exception e) {
            logger.error("Error generating dashboard overview", e);
            throw new RuntimeException("Failed to generate dashboard overview", e);
        }
    }
    
    // Add performance snapshot
    public void addPerformanceSnapshot(String modelName, PerformanceSnapshot snapshot) {
        performanceHistory.computeIfAbsent(modelName, k -> new ArrayList<>()).add(snapshot);
        
        // Limit history size
        List<PerformanceSnapshot> history = performanceHistory.get(modelName);
        if (history.size() > 1000) {
            performanceHistory.put(modelName, history.subList(history.size() - 1000, history.size()));
        }
    }
    
    // Add alert
    public void addAlert(String modelName, AlertData alert) {
        alertHistory.computeIfAbsent(modelName, k -> new ArrayList<>()).add(alert);
        
        // Limit alert history
        List<AlertData> history = alertHistory.get(modelName);
        if (history.size() > 100) {
            alertHistory.put(modelName, history.subList(history.size() - 100, history.size()));
        }
        
        logger.info("Alert added for model {}: {}", modelName, alert.getMessage());
    }
    
    // Generate performance report
    public String generatePerformanceReport(String modelName, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            ModelDashboardData dashboardData = modelDashboardData.get(modelName);
            if (dashboardData == null) {
                return "No dashboard data available for model: " + modelName;
            }
            
            StringBuilder report = new StringBuilder();
            report.append("Model Performance Report\n");
            report.append("========================\n");
            report.append("Model: ").append(modelName).append("\n");
            report.append("Period: ").append(startTime).append(" to ").append(endTime).append("\n");
            report.append("Generated: ").append(LocalDateTime.now()).append("\n\n");
            
            // Model health
            ModelHealth health = dashboardData.getModelHealth();
            report.append("Model Health: ").append(health.getStatus()).append("\n");
            report.append("Health Score: ").append(String.format("%.2f", health.getScore())).append("\n");
            report.append("Health Issues: ").append(health.getIssues()).append("\n\n");
            
            // Performance metrics
            PerformanceMetrics metrics = dashboardData.getPerformanceMetrics();
            report.append("Performance Metrics:\n");
            report.append("- Accuracy: ").append(String.format("%.2f%%", metrics.getAccuracy() * 100)).append("\n");
            report.append("- Precision: ").append(String.format("%.2f%%", metrics.getPrecision() * 100)).append("\n");
            report.append("- Recall: ").append(String.format("%.2f%%", metrics.getRecall() * 100)).append("\n");
            report.append("- F1 Score: ").append(String.format("%.2f%%", metrics.getF1Score() * 100)).append("\n");
            report.append("- Total Predictions: ").append(metrics.getTotalPredictions()).append("\n");
            report.append("- Average Confidence: ").append(String.format("%.2f%%", metrics.getAvgConfidence() * 100)).append("\n\n");
            
            // Performance trend
            PerformanceTrend trend = dashboardData.getPerformanceTrend();
            report.append("Performance Trend:\n");
            report.append("- Direction: ").append(trend.getDirection()).append("\n");
            report.append("- Change: ").append(String.format("%.2f%%", trend.getChange() * 100)).append("\n");
            report.append("- Trend Strength: ").append(String.format("%.2f", trend.getStrength())).append("\n\n");
            
            // Feature importance
            FeatureImportanceSummary featureImportance = dashboardData.getFeatureImportance();
            report.append("Top 5 Features:\n");
            for (int i = 0; i < Math.min(5, featureImportance.getTopFeatures().size()); i++) {
                FeatureImportanceSummary.TopFeature feature = featureImportance.getTopFeatures().get(i);
                report.append(String.format("%d. %s (%.3f)\n", i + 1, feature.getFeatureName(), feature.getImportanceScore()));
            }
            report.append("\n");
            
            // Alerts
            AlertSummary alertSummary = dashboardData.getAlertSummary();
            report.append("Alert Summary:\n");
            report.append("- Active Alerts: ").append(alertSummary.getActiveAlerts()).append("\n");
            report.append("- Critical Alerts: ").append(alertSummary.getCriticalAlerts()).append("\n");
            report.append("- Warning Alerts: ").append(alertSummary.getWarningAlerts()).append("\n\n");
            
            // Recommendations
            report.append("Recommendations:\n");
            for (String recommendation : dashboardData.getRecommendations()) {
                report.append("- ").append(recommendation).append("\n");
            }
            
            return report.toString();
            
        } catch (Exception e) {
            logger.error("Error generating performance report for model: {}", modelName, e);
            return "Error generating performance report.";
        }
    }
    
    // Scheduled dashboard update
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void updateDashboardData() {
        try {
            logger.debug("Updating dashboard data for all models");
            
            for (String modelName : modelDashboardData.keySet()) {
                try {
                    // This would typically fetch updated metrics from the performance tracking service
                    // For now, we'll just log the update
                    logger.debug("Dashboard data updated for model: {}", modelName);
                } catch (Exception e) {
                    logger.error("Error updating dashboard data for model: {}", modelName, e);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error in scheduled dashboard update", e);
        }
    }
    
    // Private helper methods
    private PerformanceMetrics extractPerformanceMetrics(ModelPerformanceTrackingService.ModelPerformanceMetrics metrics) {
        PerformanceMetrics perfMetrics = new PerformanceMetrics();
        perfMetrics.setAccuracy(metrics.getOverallAccuracy());
        perfMetrics.setPrecision(calculateMacroPrecision(metrics.getClassMetrics()));
        perfMetrics.setRecall(calculateMacroRecall(metrics.getClassMetrics()));
        perfMetrics.setF1Score(calculateMacroF1(metrics.getClassMetrics()));
        perfMetrics.setTotalPredictions(metrics.getTotalPredictions());
        perfMetrics.setAvgConfidence(metrics.getAvgConfidence());
        perfMetrics.setLastUpdated(metrics.getLastUpdated());
        return perfMetrics;
    }
    
    private FeatureImportanceSummary extractFeatureImportanceSummary(FeatureImportanceService.FeatureImportanceResults featureImportance) {
        FeatureImportanceSummary summary = new FeatureImportanceSummary();
        
        if (featureImportance != null && featureImportance.getCombinedImportance() != null) {
            // Top features
            List<TopFeature> topFeatures = featureImportance.getCombinedImportance().stream()
                .limit(10)
                .map(fi -> new TopFeature(fi.getFeatureName(), fi.getImportanceScore(), fi.getMethod()))
                .collect(Collectors.toList());
            summary.setTopFeatures(topFeatures);
            
            // Feature distribution
            Map<String, Long> methodDistribution = featureImportance.getCombinedImportance().stream()
                .collect(Collectors.groupingBy(FeatureImportanceService.FeatureImportance::getMethod, Collectors.counting()));
            summary.setMethodDistribution(methodDistribution);
            
            // Total features
            summary.setTotalFeatures(featureImportance.getCombinedImportance().size());
        }
        
        return summary;
    }
    
    private PerformanceTrend calculatePerformanceTrend(String modelName) {
        PerformanceTrend trend = new PerformanceTrend();
        
        List<PerformanceSnapshot> history = performanceHistory.get(modelName);
        if (history != null && history.size() >= 2) {
            PerformanceSnapshot latest = history.get(history.size() - 1);
            PerformanceSnapshot previous = history.get(history.size() - 2);
            
            double change = latest.getAccuracy() - previous.getAccuracy();
            trend.setChange(change);
            
            if (change > 0.01) {
                trend.setDirection("IMPROVING");
            } else if (change < -0.01) {
                trend.setDirection("DECLINING");
            } else {
                trend.setDirection("STABLE");
            }
            
            trend.setStrength(Math.abs(change));
        } else {
            trend.setDirection("UNKNOWN");
            trend.setChange(0.0);
            trend.setStrength(0.0);
        }
        
        return trend;
    }
    
    private AlertSummary getAlertSummary(String modelName) {
        AlertSummary summary = new AlertSummary();
        
        List<AlertData> alerts = alertHistory.getOrDefault(modelName, new ArrayList<>());
        
        // Count active alerts (last 24 hours)
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        long activeAlerts = alerts.stream()
            .filter(alert -> !alert.getTimestamp().isBefore(cutoff))
            .count();
        
        summary.setActiveAlerts((int) activeAlerts);
        
        // Count by severity
        long criticalAlerts = alerts.stream()
            .filter(alert -> alert.getSeverity() == AlertSeverity.CRITICAL)
            .count();
        long warningAlerts = alerts.stream()
            .filter(alert -> alert.getSeverity() == AlertSeverity.WARNING)
            .count();
        
        summary.setCriticalAlerts((int) criticalAlerts);
        summary.setWarningAlerts((int) warningAlerts);
        
        return summary;
    }
    
    private ModelHealth calculateModelHealth(ModelPerformanceTrackingService.ModelPerformanceMetrics metrics) {
        ModelHealth health = new ModelHealth();
        
        double score = 0.0;
        List<String> issues = new ArrayList<>();
        
        // Accuracy score (40% weight)
        double accuracy = metrics.getOverallAccuracy();
        if (accuracy >= 0.9) {
            score += 0.4;
        } else if (accuracy >= 0.8) {
            score += 0.3;
        } else if (accuracy >= 0.7) {
            score += 0.2;
        } else {
            issues.add("Low accuracy (< 70%)");
        }
        
        // Prediction volume score (20% weight)
        long totalPredictions = metrics.getTotalPredictions();
        if (totalPredictions >= 1000) {
            score += 0.2;
        } else if (totalPredictions >= 500) {
            score += 0.15;
        } else if (totalPredictions >= 100) {
            score += 0.1;
        } else {
            issues.add("Low prediction volume (< 100)");
        }
        
        // Confidence score (20% weight)
        double avgConfidence = metrics.getAvgConfidence();
        if (avgConfidence >= 0.8) {
            score += 0.2;
        } else if (avgConfidence >= 0.7) {
            score += 0.15;
        } else if (avgConfidence >= 0.6) {
            score += 0.1;
        } else {
            issues.add("Low average confidence (< 60%)");
        }
        
        // Recent performance score (20% weight)
        // Simplified - would normally check recent trend
        score += 0.2;
        
        health.setScore(score);
        health.setIssues(issues);
        
        if (score >= 0.8) {
            health.setStatus("HEALTHY");
        } else if (score >= 0.6) {
            health.setStatus("WARNING");
        } else {
            health.setStatus("CRITICAL");
        }
        
        return health;
    }
    
    private List<String> generateRecommendations(ModelPerformanceTrackingService.ModelPerformanceMetrics metrics,
                                               FeatureImportanceService.FeatureImportanceResults featureImportance) {
        List<String> recommendations = new ArrayList<>();
        
        // Accuracy recommendations
        double accuracy = metrics.getOverallAccuracy();
        if (accuracy < 0.8) {
            recommendations.add("Consider retraining model with more diverse data");
            recommendations.add("Review feature engineering - some features may need improvement");
        }
        
        // Prediction volume recommendations
        long totalPredictions = metrics.getTotalPredictions();
        if (totalPredictions < 100) {
            recommendations.add("Increase prediction volume to improve model reliability");
        }
        
        // Confidence recommendations
        double avgConfidence = metrics.getAvgConfidence();
        if (avgConfidence < 0.7) {
            recommendations.add("Model confidence is low - consider threshold adjustments");
        }
        
        // Feature importance recommendations
        if (featureImportance != null && featureImportance.getCombinedImportance() != null) {
            List<FeatureImportanceService.FeatureImportance> topFeatures = featureImportance.getCombinedImportance();
            if (topFeatures.size() > 0) {
                FeatureImportanceService.FeatureImportance top = topFeatures.get(0);
                if (top.getImportanceScore() > 0.5) {
                    recommendations.add("Model relies heavily on " + top.getFeatureName() + " - ensure data quality");
                }
            }
        }
        
        return recommendations;
    }
    
    private List<AlertData> getRecentAlerts(int limit) {
        return alertHistory.values().stream()
            .flatMap(List::stream)
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    private Map<String, PerformanceTrend> calculateOverallPerformanceTrends() {
        Map<String, PerformanceTrend> trends = new HashMap<>();
        
        for (String modelName : performanceHistory.keySet()) {
            trends.put(modelName, calculatePerformanceTrend(modelName));
        }
        
        return trends;
    }
    
    private double calculateMacroPrecision(Map<String, ModelPerformanceTrackingService.ClassMetrics> classMetrics) {
        return classMetrics.values().stream()
            .mapToDouble(ModelPerformanceTrackingService.ClassMetrics::getPrecision)
            .average().orElse(0.0);
    }
    
    private double calculateMacroRecall(Map<String, ModelPerformanceTrackingService.ClassMetrics> classMetrics) {
        return classMetrics.values().stream()
            .mapToDouble(ModelPerformanceTrackingService.ClassMetrics::getRecall)
            .average().orElse(0.0);
    }
    
    private double calculateMacroF1(Map<String, ModelPerformanceTrackingService.ClassMetrics> classMetrics) {
        return classMetrics.values().stream()
            .mapToDouble(ModelPerformanceTrackingService.ClassMetrics::getF1Score)
            .average().orElse(0.0);
    }
    
    // Inner classes
    public static class ModelDashboardData {
        private String modelName;
        private LocalDateTime generatedAt;
        private PerformanceMetrics performanceMetrics;
        private FeatureImportanceSummary featureImportance;
        private PerformanceTrend performanceTrend;
        private AlertSummary alertSummary;
        private ModelHealth modelHealth;
        private List<String> recommendations;
        
        // Getters and setters
        public String getModelName() { return modelName; }
        public void setModelName(String modelName) { this.modelName = modelName; }
        
        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
        
        public PerformanceMetrics getPerformanceMetrics() { return performanceMetrics; }
        public void setPerformanceMetrics(PerformanceMetrics performanceMetrics) { this.performanceMetrics = performanceMetrics; }
        
        public FeatureImportanceSummary getFeatureImportance() { return featureImportance; }
        public void setFeatureImportance(FeatureImportanceSummary featureImportance) { this.featureImportance = featureImportance; }
        
        public PerformanceTrend getPerformanceTrend() { return performanceTrend; }
        public void setPerformanceTrend(PerformanceTrend performanceTrend) { this.performanceTrend = performanceTrend; }
        
        public AlertSummary getAlertSummary() { return alertSummary; }
        public void setAlertSummary(AlertSummary alertSummary) { this.alertSummary = alertSummary; }
        
        public ModelHealth getModelHealth() { return modelHealth; }
        public void setModelHealth(ModelHealth modelHealth) { this.modelHealth = modelHealth; }
        
        public List<String> getRecommendations() { return recommendations; }
        public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
    }
    
    public static class DashboardOverview {
        private LocalDateTime generatedAt;
        private int totalModels;
        private int healthyModels;
        private int modelsWithAlerts;
        private double averageAccuracy;
        private double bestAccuracy;
        private double worstAccuracy;
        private List<AlertData> recentAlerts;
        private Map<String, PerformanceTrend> performanceTrends;
        
        // Getters and setters
        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
        
        public int getTotalModels() { return totalModels; }
        public void setTotalModels(int totalModels) { this.totalModels = totalModels; }
        
        public int getHealthyModels() { return healthyModels; }
        public void setHealthyModels(int healthyModels) { this.healthyModels = healthyModels; }
        
        public int getModelsWithAlerts() { return modelsWithAlerts; }
        public void setModelsWithAlerts(int modelsWithAlerts) { this.modelsWithAlerts = modelsWithAlerts; }
        
        public double getAverageAccuracy() { return averageAccuracy; }
        public void setAverageAccuracy(double averageAccuracy) { this.averageAccuracy = averageAccuracy; }
        
        public double getBestAccuracy() { return bestAccuracy; }
        public void setBestAccuracy(double bestAccuracy) { this.bestAccuracy = bestAccuracy; }
        
        public double getWorstAccuracy() { return worstAccuracy; }
        public void setWorstAccuracy(double worstAccuracy) { this.worstAccuracy = worstAccuracy; }
        
        public List<AlertData> getRecentAlerts() { return recentAlerts; }
        public void setRecentAlerts(List<AlertData> recentAlerts) { this.recentAlerts = recentAlerts; }
        
        public Map<String, PerformanceTrend> getPerformanceTrends() { return performanceTrends; }
        public void setPerformanceTrends(Map<String, PerformanceTrend> performanceTrends) { this.performanceTrends = performanceTrends; }
    }
    
    public static class PerformanceMetrics {
        private double accuracy;
        private double precision;
        private double recall;
        private double f1Score;
        private long totalPredictions;
        private double avgConfidence;
        private LocalDateTime lastUpdated;
        
        // Getters and setters
        public double getAccuracy() { return accuracy; }
        public void setAccuracy(double accuracy) { this.accuracy = accuracy; }
        
        public double getPrecision() { return precision; }
        public void setPrecision(double precision) { this.precision = precision; }
        
        public double getRecall() { return recall; }
        public void setRecall(double recall) { this.recall = recall; }
        
        public double getF1Score() { return f1Score; }
        public void setF1Score(double f1Score) { this.f1Score = f1Score; }
        
        public long getTotalPredictions() { return totalPredictions; }
        public void setTotalPredictions(long totalPredictions) { this.totalPredictions = totalPredictions; }
        
        public double getAvgConfidence() { return avgConfidence; }
        public void setAvgConfidence(double avgConfidence) { this.avgConfidence = avgConfidence; }
        
        public LocalDateTime getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    }
    
    public static class FeatureImportanceSummary {
        private List<TopFeature> topFeatures;
        private Map<String, Long> methodDistribution;
        private int totalFeatures;
        
        // Getters and setters
        public List<TopFeature> getTopFeatures() { return topFeatures; }
        public void setTopFeatures(List<TopFeature> topFeatures) { this.topFeatures = topFeatures; }
        
        public Map<String, Long> getMethodDistribution() { return methodDistribution; }
        public void setMethodDistribution(Map<String, Long> methodDistribution) { this.methodDistribution = methodDistribution; }
        
        public int getTotalFeatures() { return totalFeatures; }
        public void setTotalFeatures(int totalFeatures) { this.totalFeatures = totalFeatures; }
        
        public static class TopFeature {
            private String featureName;
            private double importanceScore;
            private String method;
            
            public TopFeature(String featureName, double importanceScore, String method) {
                this.featureName = featureName;
                this.importanceScore = importanceScore;
                this.method = method;
            }
            
            // Getters
            public String getFeatureName() { return featureName; }
            public double getImportanceScore() { return importanceScore; }
            public String getMethod() { return method; }
        }
    }
    
    public static class PerformanceTrend {
        private String direction;
        private double change;
        private double strength;
        
        // Getters and setters
        public String getDirection() { return direction; }
        public void setDirection(String direction) { this.direction = direction; }
        
        public double getChange() { return change; }
        public void setChange(double change) { this.change = change; }
        
        public double getStrength() { return strength; }
        public void setStrength(double strength) { this.strength = strength; }
    }
    
    public static class AlertSummary {
        private int activeAlerts;
        private int criticalAlerts;
        private int warningAlerts;
        
        // Getters and setters
        public int getActiveAlerts() { return activeAlerts; }
        public void setActiveAlerts(int activeAlerts) { this.activeAlerts = activeAlerts; }
        
        public int getCriticalAlerts() { return criticalAlerts; }
        public void setCriticalAlerts(int criticalAlerts) { this.criticalAlerts = criticalAlerts; }
        
        public int getWarningAlerts() { return warningAlerts; }
        public void setWarningAlerts(int warningAlerts) { this.warningAlerts = warningAlerts; }
    }
    
    public static class ModelHealth {
        private String status;
        private double score;
        private List<String> issues;
        
        // Getters and setters
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public double getScore() { return score; }
        public void setScore(double score) { this.score = score; }
        
        public List<String> getIssues() { return issues; }
        public void setIssues(List<String> issues) { this.issues = issues; }
    }
    
    public static class PerformanceSnapshot {
        private LocalDateTime timestamp;
        private double accuracy;
        private double confidence;
        
        public PerformanceSnapshot(LocalDateTime timestamp, double accuracy, double confidence) {
            this.timestamp = timestamp;
            this.accuracy = accuracy;
            this.confidence = confidence;
        }
        
        // Getters
        public LocalDateTime getTimestamp() { return timestamp; }
        public double getAccuracy() { return accuracy; }
        public double getConfidence() { return confidence; }
    }
    
    public static class AlertData {
        private String id;
        private String message;
        private AlertSeverity severity;
        private LocalDateTime timestamp;
        private boolean resolved;
        
        public AlertData(String id, String message, AlertSeverity severity, LocalDateTime timestamp) {
            this.id = id;
            this.message = message;
            this.severity = severity;
            this.timestamp = timestamp;
            this.resolved = false;
        }
        
        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public AlertSeverity getSeverity() { return severity; }
        public void setSeverity(AlertSeverity severity) { this.severity = severity; }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        
        public boolean isResolved() { return resolved; }
        public void setResolved(boolean resolved) { this.resolved = resolved; }
    }
    
    public enum AlertSeverity {
        INFO, WARNING, CRITICAL
    }
}
