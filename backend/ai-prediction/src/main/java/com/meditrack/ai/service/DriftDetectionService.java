package com.meditrack.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class DriftDetectionService {
    
    private static final Logger logger = LoggerFactory.getLogger(DriftDetectionService.class);
    
    // Drift detection storage
    private final Map<String, List<DriftResult>> driftHistory = new ConcurrentHashMap<>();
    private final Map<String, DriftBaseline> modelBaselines = new ConcurrentHashMap<>();
    private final Map<String, List<DataDistribution>> dataDistributionHistory = new ConcurrentHashMap<>();
    
    // Drift detection thresholds
    private static final double ACCURACY_DRIFT_THRESHOLD = 0.1; // 10% drop
    private static final double DISTRIBUTION_DRIFT_THRESHOLD = 0.2; // 20% change
    private static final double PREDICTION_DRIFT_THRESHOLD = 0.15; // 15% change
    private static final int MIN_PREDICTIONS_FOR_DRIFT = 100;
    
    // Detect model drift
    public DriftDetectionResult detectModelDrift(String modelName, 
                                               ModelPerformanceTrackingService.ModelPerformanceMetrics currentMetrics,
                                               List<ModelPerformanceTrackingService.PredictionRecord> recentPredictions) {
        try {
            logger.info("Detecting model drift for: {}", modelName);
            
            DriftDetectionResult result = new DriftDetectionResult();
            result.setModelName(modelName);
            result.setDetectionTimestamp(LocalDateTime.now());
            
            // Get baseline
            DriftBaseline baseline = modelBaselines.get(modelName);
            if (baseline == null) {
                // Create baseline if not exists
                baseline = createBaseline(modelName, currentMetrics);
                modelBaselines.put(modelName, baseline);
                result.setDriftDetected(false);
                result.setMessage("Baseline created - no drift detected");
                return result;
            }
            
            // Detect different types of drift
            List<DriftResult> driftResults = new ArrayList<>();
            
            // Accuracy drift
            DriftResult accuracyDrift = detectAccuracyDrift(modelName, currentMetrics, baseline);
            if (accuracyDrift != null) {
                driftResults.add(accuracyDrift);
            }
            
            // Prediction distribution drift
            DriftResult predictionDrift = detectPredictionDistributionDrift(modelName, recentPredictions, baseline);
            if (predictionDrift != null) {
                driftResults.add(predictionDrift);
            }
            
            // Data distribution drift
            DriftResult dataDrift = detectDataDistributionDrift(modelName, recentPredictions, baseline);
            if (dataDrift != null) {
                driftResults.add(dataDrift);
            }
            
            // Performance trend drift
            DriftResult trendDrift = detectPerformanceTrendDrift(modelName, currentMetrics, baseline);
            if (trendDrift != null) {
                driftResults.add(trendDrift);
            }
            
            result.setDriftResults(driftResults);
            
            // Determine if overall drift is detected
            boolean driftDetected = !driftResults.isEmpty();
            result.setDriftDetected(driftDetected);
            
            if (driftDetected) {
                result.setSeverity(calculateOverallDriftSeverity(driftResults));
                result.setMessage("Model drift detected - retraining recommended");
                
                // Store drift result
                driftHistory.computeIfAbsent(modelName, k -> new ArrayList<>()).addAll(driftResults);
                
                logger.warn("Model drift detected for {}: {} drift types detected", 
                           modelName, driftResults.size());
            } else {
                result.setSeverity(DriftSeverity.NONE);
                result.setMessage("No drift detected");
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error detecting model drift for: {}", modelName, e);
            throw new RuntimeException("Failed to detect model drift", e);
        }
    }
    
    // Create baseline for drift detection
    public DriftBaseline createBaseline(String modelName, ModelPerformanceTrackingService.ModelPerformanceMetrics metrics) {
        try {
            logger.info("Creating drift baseline for model: {}", modelName);
            
            DriftBaseline baseline = new DriftBaseline();
            baseline.setModelName(modelName);
            baseline.setCreatedTimestamp(LocalDateTime.now());
            
            // Performance baseline
            baseline.setBaselineAccuracy(metrics.getOverallAccuracy());
            baseline.setBaselinePrecision(calculateMacroPrecision(metrics.getClassMetrics()));
            baseline.setBaselineRecall(calculateMacroRecall(metrics.getClassMetrics()));
            baseline.setBaselineF1Score(calculateMacroF1(metrics.getClassMetrics()));
            baseline.setBaselineAvgConfidence(metrics.getAvgConfidence());
            
            // Prediction distribution baseline
            Map<String, Double> predictionDistribution = calculatePredictionDistribution(
                getRecentPredictions(modelName, 1000));
            baseline.setBaselinePredictionDistribution(predictionDistribution);
            
            // Data distribution baseline
            Map<String, DataDistribution> dataDistribution = calculateDataDistribution(
                getRecentPredictions(modelName, 1000));
            baseline.setBaselineDataDistribution(dataDistribution);
            
            // Performance trend baseline
            baseline.setBaselinePerformanceTrend(calculatePerformanceTrend(modelName));
            
            logger.info("Baseline created for model: {} with accuracy: {:.3f}", 
                       modelName, baseline.getBaselineAccuracy());
            
            return baseline;
            
        } catch (Exception e) {
            logger.error("Error creating baseline for model: {}", modelName, e);
            throw new RuntimeException("Failed to create baseline", e);
        }
    }
    
    // Update baseline
    public void updateBaseline(String modelName, DriftBaseline newBaseline) {
        modelBaselines.put(modelName, newBaseline);
        logger.info("Baseline updated for model: {}", modelName);
    }
    
    // Get drift history
    public List<DriftResult> getDriftHistory(String modelName, int limit) {
        List<DriftResult> history = driftHistory.getOrDefault(modelName, new ArrayList<>());
        return history.stream()
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    // Get all drift results
    public Map<String, List<DriftResult>> getAllDriftResults() {
        return new HashMap<>(driftHistory);
    }
    
    // Generate drift report
    public String generateDriftReport(String modelName, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            StringBuilder report = new StringBuilder();
            report.append("Model Drift Detection Report\n");
            report.append("===========================\n");
            report.append("Model: ").append(modelName).append("\n");
            report.append("Period: ").append(startTime).append(" to ").append(endTime).append("\n");
            report.append("Generated: ").append(LocalDateTime.now()).append("\n\n");
            
            // Baseline information
            DriftBaseline baseline = modelBaselines.get(modelName);
            if (baseline != null) {
                report.append("Baseline Information:\n");
                report.append("- Created: ").append(baseline.getCreatedTimestamp()).append("\n");
                report.append("- Baseline Accuracy: ").append(String.format("%.3f", baseline.getBaselineAccuracy())).append("\n");
                report.append("- Baseline Precision: ").append(String.format("%.3f", baseline.getBaselinePrecision())).append("\n");
                report.append("- Baseline Recall: ").append(String.format("%.3f", baseline.getBaselineRecall())).append("\n");
                report.append("- Baseline F1 Score: ").append(String.format("%.3f", baseline.getBaselineF1Score())).append("\n\n");
            }
            
            // Recent drift results
            List<DriftResult> recentDrifts = getDriftHistory(modelName, 20);
            if (!recentDrifts.isEmpty()) {
                report.append("Recent Drift Detection Results:\n");
                report.append("---------------------------------\n");
                
                for (DriftResult drift : recentDrifts) {
                    report.append("Type: ").append(drift.getDriftType()).append("\n");
                    report.append("Severity: ").append(drift.getSeverity()).append("\n");
                    report.append("Timestamp: ").append(drift.getTimestamp()).append("\n");
                    report.append("Description: ").append(drift.getDescription()).append("\n");
                    report.append("Metric Value: ").append(String.format("%.3f", drift.getMetricValue())).append("\n");
                    report.append("Threshold: ").append(String.format("%.3f", drift.getThreshold())).append("\n");
                    report.append("---\n");
                }
            } else {
                report.append("No drift detected in the specified period.\n");
            }
            
            // Drift statistics
            Map<DriftType, Long> driftTypeCounts = recentDrifts.stream()
                .collect(Collectors.groupingBy(DriftResult::getDriftType, Collectors.counting()));
            
            Map<DriftSeverity, Long> driftSeverityCounts = recentDrifts.stream()
                .collect(Collectors.groupingBy(DriftResult::getSeverity, Collectors.counting()));
            
            report.append("\nDrift Statistics:\n");
            report.append("---------------\n");
            report.append("Total Drift Events: ").append(recentDrifts.size()).append("\n");
            
            report.append("By Type:\n");
            for (Map.Entry<DriftType, Long> entry : driftTypeCounts.entrySet()) {
                report.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            
            report.append("By Severity:\n");
            for (Map.Entry<DriftSeverity, Long> entry : driftSeverityCounts.entrySet()) {
                report.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            
            return report.toString();
            
        } catch (Exception e) {
            logger.error("Error generating drift report for model: {}", modelName, e);
            return "Error generating drift report.";
        }
    }
    
    // Scheduled drift detection
    @Scheduled(fixedRate = 600000) // Every 10 minutes
    public void scheduledDriftDetection() {
        try {
            logger.debug("Running scheduled drift detection for all models");
            
            for (String modelName : modelBaselines.keySet()) {
                try {
                    // This would typically fetch current metrics and predictions
                    // For now, we'll just log the check
                    logger.debug("Scheduled drift check for model: {}", modelName);
                } catch (Exception e) {
                    logger.error("Error in scheduled drift detection for model: {}", modelName, e);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error in scheduled drift detection", e);
        }
    }
    
    // Private helper methods
    private DriftResult detectAccuracyDrift(String modelName, 
                                          ModelPerformanceTrackingService.ModelPerformanceMetrics currentMetrics,
                                          DriftBaseline baseline) {
        try {
            double currentAccuracy = currentMetrics.getOverallAccuracy();
            double baselineAccuracy = baseline.getBaselineAccuracy();
            
            double accuracyDrop = baselineAccuracy - currentAccuracy;
            
            if (accuracyDrop > ACCURACY_DRIFT_THRESHOLD) {
                DriftSeverity severity = accuracyDrop > 0.2 ? DriftSeverity.SEVERE : 
                                       accuracyDrop > 0.15 ? DriftSeverity.HIGH : DriftSeverity.MEDIUM;
                
                return new DriftResult(
                    DriftType.ACCURACY_DRIFT,
                    severity,
                    "Accuracy drop detected",
                    accuracyDrop,
                    ACCURACY_DRIFT_THRESHOLD,
                    LocalDateTime.now(),
                    Map.of(
                        "currentAccuracy", currentAccuracy,
                        "baselineAccuracy", baselineAccuracy,
                        "accuracyDrop", accuracyDrop
                    )
                );
            }
            
            return null;
        } catch (Exception e) {
            logger.error("Error detecting accuracy drift", e);
            return null;
        }
    }
    
    private DriftResult detectPredictionDistributionDrift(String modelName, 
                                                        List<ModelPerformanceTrackingService.PredictionRecord> recentPredictions,
                                                        DriftBaseline baseline) {
        try {
            if (recentPredictions.size() < MIN_PREDICTIONS_FOR_DRIFT) {
                return null;
            }
            
            Map<String, Double> currentDistribution = calculatePredictionDistribution(recentPredictions);
            Map<String, Double> baselineDistribution = baseline.getBaselinePredictionDistribution();
            
            double distributionDistance = calculateDistributionDistance(currentDistribution, baselineDistribution);
            
            if (distributionDistance > DISTRIBUTION_DRIFT_THRESHOLD) {
                DriftSeverity severity = distributionDistance > 0.4 ? DriftSeverity.SEVERE : 
                                       distributionDistance > 0.3 ? DriftSeverity.HIGH : DriftSeverity.MEDIUM;
                
                return new DriftResult(
                    DriftType.PREDICTION_DISTRIBUTION_DRIFT,
                    severity,
                    "Prediction distribution shift detected",
                    distributionDistance,
                    DISTRIBUTION_DRIFT_THRESHOLD,
                    LocalDateTime.now(),
                    Map.of(
                        "currentDistribution", currentDistribution,
                        "baselineDistribution", baselineDistribution,
                        "distributionDistance", distributionDistance
                    )
                );
            }
            
            return null;
        } catch (Exception e) {
            logger.error("Error detecting prediction distribution drift", e);
            return null;
        }
    }
    
    private DriftResult detectDataDistributionDrift(String modelName, 
                                                   List<ModelPerformanceTrackingService.PredictionRecord> recentPredictions,
                                                   DriftBaseline baseline) {
        try {
            if (recentPredictions.size() < MIN_PREDICTIONS_FOR_DRIFT) {
                return null;
            }
            
            Map<String, DataDistribution> currentDistribution = calculateDataDistribution(recentPredictions);
            Map<String, DataDistribution> baselineDistribution = baseline.getBaselineDataDistribution();
            
            double maxDistributionChange = 0.0;
            String mostChangedFeature = "";
            
            for (Map.Entry<String, DataDistribution> entry : currentDistribution.entrySet()) {
                String feature = entry.getKey();
                DataDistribution current = entry.getValue();
                DataDistribution baseline = baselineDistribution.get(feature);
                
                if (baseline != null) {
                    double change = Math.abs(current.getMean() - baseline.getMean()) / baseline.getMean();
                    if (change > maxDistributionChange) {
                        maxDistributionChange = change;
                        mostChangedFeature = feature;
                    }
                }
            }
            
            if (maxDistributionChange > PREDICTION_DRIFT_THRESHOLD) {
                DriftSeverity severity = maxDistributionChange > 0.3 ? DriftSeverity.SEVERE : 
                                       maxDistributionChange > 0.25 ? DriftSeverity.HIGH : DriftSeverity.MEDIUM;
                
                return new DriftResult(
                    DriftType.DATA_DISTRIBUTION_DRIFT,
                    severity,
                    "Data distribution shift detected",
                    maxDistributionChange,
                    PREDICTION_DRIFT_THRESHOLD,
                    LocalDateTime.now(),
                    Map.of(
                        "mostChangedFeature", mostChangedFeature,
                        "maxChange", maxDistributionChange
                    )
                );
            }
            
            return null;
        } catch (Exception e) {
            logger.error("Error detecting data distribution drift", e);
            return null;
        }
    }
    
    private DriftResult detectPerformanceTrendDrift(String modelName, 
                                                   ModelPerformanceTrackingService.ModelPerformanceMetrics currentMetrics,
                                                   DriftBaseline baseline) {
        try {
            // Simplified trend detection - would normally use more sophisticated methods
            double currentAccuracy = currentMetrics.getOverallAccuracy();
            double baselineAccuracy = baseline.getBaselineAccuracy();
            
            // Check for consistent decline
            List<DriftResult> recentDrifts = driftHistory.getOrDefault(modelName, new ArrayList<>());
            long recentAccuracyDrifts = recentDrifts.stream()
                .filter(drift -> drift.getDriftType() == DriftType.ACCURACY_DRIFT)
                .filter(drift -> drift.getTimestamp().isAfter(LocalDateTime.now().minusHours(24)))
                .count();
            
            if (recentAccuracyDrifts >= 3) { // Multiple accuracy drifts in 24 hours
                return new DriftResult(
                    DriftType.PERFORMANCE_TREND_DRIFT,
                    DriftSeverity.HIGH,
                    "Consistent performance decline detected",
                    recentAccuracyDrifts,
                    2.0, // threshold
                    LocalDateTime.now(),
                    Map.of(
                        "recentAccuracyDrifts", recentAccuracyDrifts,
                        "currentAccuracy", currentAccuracy,
                        "baselineAccuracy", baselineAccuracy
                    )
                );
            }
            
            return null;
        } catch (Exception e) {
            logger.error("Error detecting performance trend drift", e);
            return null;
        }
    }
    
    private DriftSeverity calculateOverallDriftSeverity(List<DriftResult> driftResults) {
        if (driftResults.isEmpty()) {
            return DriftSeverity.NONE;
        }
        
        // Find the maximum severity
        return driftResults.stream()
            .map(DriftResult::getSeverity)
            .max(Comparator.comparingInt(severity -> severity.ordinal()))
            .orElse(DriftSeverity.NONE);
    }
    
    private Map<String, Double> calculatePredictionDistribution(List<ModelPerformanceTrackingService.PredictionRecord> predictions) {
        Map<String, Double> distribution = new HashMap<>();
        long total = predictions.size();
        
        for (ModelPerformanceTrackingService.PredictionRecord prediction : predictions) {
            String predictedClass = prediction.getPredictedClass();
            distribution.put(predictedClass, distribution.getOrDefault(predictedClass, 0.0) + 1.0);
        }
        
        // Normalize to percentages
        for (Map.Entry<String, Double> entry : distribution.entrySet()) {
            entry.setValue(entry.getValue() / total);
        }
        
        return distribution;
    }
    
    private Map<String, DataDistribution> calculateDataDistribution(List<ModelPerformanceTrackingService.PredictionRecord> predictions) {
        Map<String, List<Double>> featureValues = new HashMap<>();
        
        // Collect feature values
        for (ModelPerformanceTrackingService.PredictionRecord prediction : predictions) {
            for (Map.Entry<String, Object> entry : prediction.getFeatures().entrySet()) {
                String feature = entry.getKey();
                Object value = entry.getValue();
                
                if (value instanceof Number) {
                    double numericValue = ((Number) value).doubleValue();
                    featureValues.computeIfAbsent(feature, k -> new ArrayList<>()).add(numericValue);
                }
            }
        }
        
        // Calculate distribution statistics
        Map<String, DataDistribution> distributions = new HashMap<>();
        for (Map.Entry<String, List<Double>> entry : featureValues.entrySet()) {
            String feature = entry.getKey();
            List<Double> values = entry.getValue();
            
            if (!values.isEmpty()) {
                double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                double variance = values.stream()
                    .mapToDouble(v -> Math.pow(v - mean, 2))
                    .average().orElse(0.0);
                double stdDev = Math.sqrt(variance);
                
                distributions.put(feature, new DataDistribution(mean, stdDev, values.size()));
            }
        }
        
        return distributions;
    }
    
    private double calculateDistributionDistance(Map<String, Double> current, Map<String, Double> baseline) {
        // Calculate Earth Mover's Distance (simplified)
        double distance = 0.0;
        
        Set<String> allClasses = new HashSet<>();
        allClasses.addAll(current.keySet());
        allClasses.addAll(baseline.keySet());
        
        for (String className : allClasses) {
            double currentProb = current.getOrDefault(className, 0.0);
            double baselineProb = baseline.getOrDefault(className, 0.0);
            distance += Math.abs(currentProb - baselineProb);
        }
        
        return distance / 2.0; // Normalize
    }
    
    private List<ModelPerformanceTrackingService.PredictionRecord> getRecentPredictions(String modelName, int limit) {
        // This would typically fetch from the performance tracking service
        // For now, return empty list
        return new ArrayList<>();
    }
    
    private String calculatePerformanceTrend(String modelName) {
        // Simplified trend calculation
        return "STABLE";
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
    public static class DriftDetectionResult {
        private String modelName;
        private LocalDateTime detectionTimestamp;
        private boolean driftDetected;
        private DriftSeverity severity;
        private String message;
        private List<DriftResult> driftResults;
        
        // Getters and setters
        public String getModelName() { return modelName; }
        public void setModelName(String modelName) { this.modelName = modelName; }
        
        public LocalDateTime getDetectionTimestamp() { return detectionTimestamp; }
        public void setDetectionTimestamp(LocalDateTime detectionTimestamp) { this.detectionTimestamp = detectionTimestamp; }
        
        public boolean isDriftDetected() { return driftDetected; }
        public void setDriftDetected(boolean driftDetected) { this.driftDetected = driftDetected; }
        
        public DriftSeverity getSeverity() { return severity; }
        public void setSeverity(DriftSeverity severity) { this.severity = severity; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public List<DriftResult> getDriftResults() { return driftResults; }
        public void setDriftResults(List<DriftResult> driftResults) { this.driftResults = driftResults; }
    }
    
    public static class DriftBaseline {
        private String modelName;
        private LocalDateTime createdTimestamp;
        private double baselineAccuracy;
        private double baselinePrecision;
        private double baselineRecall;
        private double baselineF1Score;
        private double baselineAvgConfidence;
        private Map<String, Double> baselinePredictionDistribution;
        private Map<String, DataDistribution> baselineDataDistribution;
        private String baselinePerformanceTrend;
        
        // Getters and setters
        public String getModelName() { return modelName; }
        public void setModelName(String modelName) { this.modelName = modelName; }
        
        public LocalDateTime getCreatedTimestamp() { return createdTimestamp; }
        public void setCreatedTimestamp(LocalDateTime createdTimestamp) { this.createdTimestamp = createdTimestamp; }
        
        public double getBaselineAccuracy() { return baselineAccuracy; }
        public void setBaselineAccuracy(double baselineAccuracy) { this.baselineAccuracy = baselineAccuracy; }
        
        public double getBaselinePrecision() { return baselinePrecision; }
        public void setBaselinePrecision(double baselinePrecision) { this.baselinePrecision = baselinePrecision; }
        
        public double getBaselineRecall() { return baselineRecall; }
        public void setBaselineRecall(double baselineRecall) { this.baselineRecall = baselineRecall; }
        
        public double getBaselineF1Score() { return baselineF1Score; }
        public void setBaselineF1Score(double baselineF1Score) { this.baselineF1Score = baselineF1Score; }
        
        public double getBaselineAvgConfidence() { return baselineAvgConfidence; }
        public void setBaselineAvgConfidence(double baselineAvgConfidence) { this.baselineAvgConfidence = baselineAvgConfidence; }
        
        public Map<String, Double> getBaselinePredictionDistribution() { return baselinePredictionDistribution; }
        public void setBaselinePredictionDistribution(Map<String, Double> baselinePredictionDistribution) { this.baselinePredictionDistribution = baselinePredictionDistribution; }
        
        public Map<String, DataDistribution> getBaselineDataDistribution() { return baselineDataDistribution; }
        public void setBaselineDataDistribution(Map<String, DataDistribution> baselineDataDistribution) { this.baselineDataDistribution = baselineDataDistribution; }
        
        public String getBaselinePerformanceTrend() { return baselinePerformanceTrend; }
        public void setBaselinePerformanceTrend(String baselinePerformanceTrend) { this.baselinePerformanceTrend = baselinePerformanceTrend; }
    }
    
    public static class DriftResult {
        private DriftType driftType;
        private DriftSeverity severity;
        private String description;
        private double metricValue;
        private double threshold;
        private LocalDateTime timestamp;
        private Map<String, Object> details;
        
        public DriftResult(DriftType driftType, DriftSeverity severity, String description, 
                          double metricValue, double threshold, LocalDateTime timestamp, 
                          Map<String, Object> details) {
            this.driftType = driftType;
            this.severity = severity;
            this.description = description;
            this.metricValue = metricValue;
            this.threshold = threshold;
            this.timestamp = timestamp;
            this.details = details;
        }
        
        // Getters
        public DriftType getDriftType() { return driftType; }
        public DriftSeverity getSeverity() { return severity; }
        public String getDescription() { return description; }
        public double getMetricValue() { return metricValue; }
        public double getThreshold() { return threshold; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public Map<String, Object> getDetails() { return details; }
    }
    
    public static class DataDistribution {
        private double mean;
        private double stdDev;
        private int sampleSize;
        
        public DataDistribution(double mean, double stdDev, int sampleSize) {
            this.mean = mean;
            this.stdDev = stdDev;
            this.sampleSize = sampleSize;
        }
        
        // Getters
        public double getMean() { return mean; }
        public double getStdDev() { return stdDev; }
        public int getSampleSize() { return sampleSize; }
    }
    
    public enum DriftType {
        ACCURACY_DRIFT,
        PREDICTION_DISTRIBUTION_DRIFT,
        DATA_DISTRIBUTION_DRIFT,
        PERFORMANCE_TREND_DRIFT
    }
    
    public enum DriftSeverity {
        NONE, LOW, MEDIUM, HIGH, SEVERE
    }
}
