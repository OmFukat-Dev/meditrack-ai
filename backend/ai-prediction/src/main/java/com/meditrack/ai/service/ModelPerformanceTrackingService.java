package com.meditrack.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class ModelPerformanceTrackingService {
    
    private static final Logger logger = LoggerFactory.getLogger(ModelPerformanceTrackingService.class);
    
    // Performance tracking storage
    private final Map<String, ModelPerformanceMetrics> modelMetrics = new ConcurrentHashMap<>();
    private final Map<String, List<PredictionRecord>> predictionHistory = new ConcurrentHashMap<>();
    private final Map<String, List<DriftDetection>> driftHistory = new ConcurrentHashMap<>();
    private final AtomicLong totalPredictions = new AtomicLong(0);
    private final AtomicLong totalModelRetrains = new AtomicLong(0);
    
    // Track prediction performance
    public void trackPrediction(String modelName, String predictedClass, String actualClass, 
                              double confidence, Map<String, Object> features) {
        try {
            // Create prediction record
            PredictionRecord record = new PredictionRecord(
                modelName,
                predictedClass,
                actualClass,
                confidence,
                features,
                LocalDateTime.now()
            );
            
            // Add to prediction history
            predictionHistory.computeIfAbsent(modelName, k -> new ArrayList<>()).add(record);
            totalPredictions.incrementAndGet();
            
            // Update model metrics
            updateModelMetrics(modelName, record);
            
            // Check for model drift
            checkModelDrift(modelName);
            
            // Limit history size
            limitHistorySize(modelName);
            
            logger.debug("Tracked prediction for model: {}, predicted: {}, actual: {}, confidence: {}", 
                        modelName, predictedClass, actualClass, confidence);
            
        } catch (Exception e) {
            logger.error("Error tracking prediction for model: {}", modelName, e);
        }
    }
    
    // Track batch predictions
    public void trackBatchPredictions(String modelName, List<BatchPredictionRecord> batchRecords) {
        try {
            for (BatchPredictionRecord batchRecord : batchRecords) {
                PredictionRecord record = new PredictionRecord(
                    modelName,
                    batchRecord.getPredictedClass(),
                    batchRecord.getActualClass(),
                    batchRecord.getConfidence(),
                    batchRecord.getFeatures(),
                    batchRecord.getTimestamp()
                );
                
                predictionHistory.computeIfAbsent(modelName, k -> new ArrayList<>()).add(record);
                totalPredictions.incrementAndGet();
                updateModelMetrics(modelName, record);
            }
            
            // Check for model drift after batch
            checkModelDrift(modelName);
            limitHistorySize(modelName);
            
            logger.info("Tracked {} batch predictions for model: {}", batchRecords.size(), modelName);
            
        } catch (Exception e) {
            logger.error("Error tracking batch predictions for model: {}", modelName, e);
        }
    }
    
    // Get model performance metrics
    public ModelPerformanceMetrics getModelMetrics(String modelName) {
        return modelMetrics.get(modelName);
    }
    
    // Get all model metrics
    public Map<String, ModelPerformanceMetrics> getAllModelMetrics() {
        return new HashMap<>(modelMetrics);
    }
    
    // Get prediction history
    public List<PredictionRecord> getPredictionHistory(String modelName, int limit) {
        List<PredictionRecord> history = predictionHistory.getOrDefault(modelName, new ArrayList<>());
        return history.stream()
            .sorted(Comparator.comparing(PredictionRecord::getTimestamp).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    // Get drift history
    public List<DriftDetection> getDriftHistory(String modelName, int limit) {
        List<DriftDetection> history = driftHistory.getOrDefault(modelName, new ArrayList<>());
        return history.stream()
            .sorted(Comparator.comparing(DriftDetection::getTimestamp).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    // Calculate model performance summary
    public ModelPerformanceSummary calculatePerformanceSummary(String modelName, 
                                                        LocalDateTime startTime, 
                                                        LocalDateTime endTime) {
        try {
            List<PredictionRecord> records = predictionHistory.getOrDefault(modelName, new ArrayList<>());
            
            // Filter by time range
            List<PredictionRecord> filteredRecords = records.stream()
                .filter(r -> !r.getTimestamp().isBefore(startTime) && 
                           !r.getTimestamp().isAfter(endTime))
                .collect(Collectors.toList());
            
            if (filteredRecords.isEmpty()) {
                return new ModelPerformanceSummary(modelName, startTime, endTime, 0, 0.0, 0.0, 0.0, 0.0);
            }
            
            // Calculate metrics
            long totalPredictions = filteredRecords.size();
            long correctPredictions = filteredRecords.stream()
                .mapToLong(r -> r.getPredictedClass().equals(r.getActualClass()) ? 1 : 0)
                .sum();
            
            double accuracy = (double) correctPredictions / totalPredictions;
            
            // Calculate precision, recall, F1 by class
            Map<String, ClassMetrics> classMetrics = calculateClassMetrics(filteredRecords);
            
            // Calculate average confidence
            double avgConfidence = filteredRecords.stream()
                .mapToDouble(PredictionRecord::getConfidence)
                .average().orElse(0.0);
            
            // Calculate confidence distribution
            Map<String, Long> confidenceDistribution = calculateConfidenceDistribution(filteredRecords);
            
            return new ModelPerformanceSummary(
                modelName,
                startTime,
                endTime,
                totalPredictions,
                accuracy,
                calculateMacroPrecision(classMetrics),
                calculateMacroRecall(classMetrics),
                calculateMacroF1(classMetrics),
                avgConfidence,
                confidenceDistribution,
                classMetrics
            );
            
        } catch (Exception e) {
            logger.error("Error calculating performance summary for model: {}", modelName, e);
            return null;
        }
    }
    
    // Detect model drift
    public DriftDetection detectModelDrift(String modelName) {
        try {
            ModelPerformanceMetrics metrics = modelMetrics.get(modelName);
            if (metrics == null) {
                return null;
            }
            
            // Get recent predictions
            List<PredictionRecord> recentPredictions = getPredictionHistory(modelName, 100);
            if (recentPredictions.size() < 50) {
                return null; // Not enough data
            }
            
            // Calculate recent accuracy
            long correctRecent = recentPredictions.stream()
                .mapToLong(r -> r.getPredictedClass().equals(r.getActualClass()) ? 1 : 0)
                .sum();
            
            double recentAccuracy = (double) correctRecent / recentPredictions.size();
            
            // Compare with baseline accuracy
            double baselineAccuracy = metrics.getOverallAccuracy();
            double accuracyDrop = baselineAccuracy - recentAccuracy;
            
            // Check for significant drift
            if (accuracyDrop > 0.1) { // 10% drop
                DriftDetection drift = new DriftDetection(
                    modelName,
                    "ACCURACY_DRIFT",
                    "Significant accuracy drop detected",
                    baselineAccuracy,
                    recentAccuracy,
                    accuracyDrop,
                    LocalDateTime.now()
                );
                
                // Store drift detection
                driftHistory.computeIfAbsent(modelName, k -> new ArrayList<>()).add(drift);
                
                logger.warn("Model drift detected for {}: baseline={}, recent={}, drop={}", 
                           modelName, baselineAccuracy, recentAccuracy, accuracyDrop);
                
                return drift;
            }
            
            return null;
            
        } catch (Exception e) {
            logger.error("Error detecting model drift for: {}", modelName, e);
            return null;
        }
    }
    
    // Update model performance metrics
    private void updateModelMetrics(String modelName, PredictionRecord record) {
        ModelPerformanceMetrics metrics = modelMetrics.computeIfAbsent(
            modelName, k -> new ModelPerformanceMetrics(modelName)
        );
        
        metrics.addPrediction(record);
        metrics.setLastUpdated(LocalDateTime.now());
    }
    
    // Check for model drift
    private void checkModelDrift(String modelName) {
        try {
            DriftDetection drift = detectModelDrift(modelName);
            if (drift != null) {
                // Trigger model retraining alert
                logger.warn("Model retraining recommended for: {}", modelName);
            }
        } catch (Exception e) {
            logger.error("Error checking model drift for: {}", modelName, e);
        }
    }
    
    // Limit history size to prevent memory issues
    private void limitHistorySize(String modelName) {
        List<PredictionRecord> history = predictionHistory.get(modelName);
        if (history != null && history.size() > 10000) {
            // Keep only the most recent 10000 predictions
            List<PredictionRecord> limited = history.stream()
                .sorted(Comparator.comparing(PredictionRecord::getTimestamp).reversed())
                .limit(10000)
                .collect(Collectors.toList());
            
            predictionHistory.put(modelName, limited);
        }
    }
    
    // Calculate class-specific metrics
    private Map<String, ClassMetrics> calculateClassMetrics(List<PredictionRecord> records) {
        Map<String, ClassMetrics> classMetrics = new HashMap<>();
        
        // Group by actual class
        Map<String, List<PredictionRecord>> byActualClass = records.stream()
            .collect(Collectors.groupingBy(PredictionRecord::getActualClass));
        
        for (Map.Entry<String, List<PredictionRecord>> entry : byActualClass.entrySet()) {
            String className = entry.getKey();
            List<PredictionRecord> classRecords = entry.getValue();
            
            // Calculate TP, FP, FN, TN
            long totalActual = classRecords.size();
            long truePositives = classRecords.stream()
                .mapToLong(r -> r.getPredictedClass().equals(className) ? 1 : 0)
                .sum();
            
            // Calculate false positives for this class
            long falsePositives = records.stream()
                .filter(r -> r.getPredictedClass().equals(className) && 
                           !r.getActualClass().equals(className))
                .count();
            
            // Calculate false negatives for this class
            long falseNegatives = classRecords.stream()
                .mapToLong(r -> !r.getPredictedClass().equals(className) ? 1 : 0)
                .sum();
            
            double precision = (truePositives + falsePositives) > 0 ? 
                (double) truePositives / (truePositives + falsePositives) : 0.0;
            double recall = (truePositives + falseNegatives) > 0 ? 
                (double) truePositives / (truePositives + falseNegatives) : 0.0;
            double f1Score = (precision + recall) > 0 ? 
                2 * (precision * recall) / (precision + recall) : 0.0;
            
            classMetrics.put(className, new ClassMetrics(
                className, totalActual, truePositives, falsePositives, falseNegatives, precision, recall, f1Score
            ));
        }
        
        return classMetrics;
    }
    
    // Calculate macro precision
    private double calculateMacroPrecision(Map<String, ClassMetrics> classMetrics) {
        return classMetrics.values().stream()
            .mapToDouble(ClassMetrics::getPrecision)
            .average().orElse(0.0);
    }
    
    // Calculate macro recall
    private double calculateMacroRecall(Map<String, ClassMetrics> classMetrics) {
        return classMetrics.values().stream()
            .mapToDouble(ClassMetrics::getRecall)
            .average().orElse(0.0);
    }
    
    // Calculate macro F1
    private double calculateMacroF1(Map<String, ClassMetrics> classMetrics) {
        return classMetrics.values().stream()
            .mapToDouble(ClassMetrics::getF1Score)
            .average().orElse(0.0);
    }
    
    // Calculate confidence distribution
    private Map<String, Long> calculateConfidenceDistribution(List<PredictionRecord> records) {
        Map<String, Long> distribution = new HashMap<>();
        
        for (PredictionRecord record : records) {
            double confidence = record.getConfidence();
            String bucket;
            
            if (confidence >= 0.9) {
                bucket = "0.9-1.0";
            } else if (confidence >= 0.8) {
                bucket = "0.8-0.9";
            } else if (confidence >= 0.7) {
                bucket = "0.7-0.8";
            } else if (confidence >= 0.6) {
                bucket = "0.6-0.7";
            } else if (confidence >= 0.5) {
                bucket = "0.5-0.6";
            } else {
                bucket = "0.0-0.5";
            }
            
            distribution.merge(bucket, 1L, Long::sum);
        }
        
        return distribution;
    }
    
    // Reset model metrics
    public void resetModelMetrics(String modelName) {
        modelMetrics.remove(modelName);
        predictionHistory.remove(modelName);
        driftHistory.remove(modelName);
        
        logger.info("Reset metrics for model: {}", modelName);
    }
    
    // Get global statistics
    public GlobalStatistics getGlobalStatistics() {
        long totalModels = modelMetrics.size();
        long totalPredictions = this.totalPredictions.get();
        long totalRetrains = totalModelRetrains.get();
        
        // Calculate average accuracy across all models
        double avgAccuracy = modelMetrics.values().stream()
            .mapToDouble(ModelPerformanceMetrics::getOverallAccuracy)
            .average().orElse(0.0);
        
        // Count models with drift
        long modelsWithDrift = driftHistory.values().stream()
            .mapToLong(List::size)
            .sum();
        
        return new GlobalStatistics(
            totalModels,
            totalPredictions,
            totalRetrains,
            avgAccuracy,
            modelsWithDrift,
            LocalDateTime.now()
        );
    }
    
    // Export performance data
    public String exportPerformanceData(String modelName, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            ModelPerformanceSummary summary = calculatePerformanceSummary(modelName, startTime, endTime);
            if (summary == null) {
                return "No performance data available";
            }
            
            StringBuilder sb = new StringBuilder();
            sb.append("Model Performance Summary\n");
            sb.append("========================\n");
            sb.append("Model: ").append(modelName).append("\n");
            sb.append("Period: ").append(startTime).append(" to ").append(endTime).append("\n");
            sb.append("Total Predictions: ").append(summary.getTotalPredictions()).append("\n");
            sb.append("Accuracy: ").append(String.format("%.2f%%", summary.getAccuracy() * 100)).append("\n");
            sb.append("Precision: ").append(String.format("%.2f%%", summary.getPrecision() * 100)).append("\n");
            sb.append("Recall: ").append(String.format("%.2f%%", summary.getRecall() * 100)).append("\n");
            sb.append("F1 Score: ").append(String.format("%.2f%%", summary.getF1Score() * 100)).append("\n");
            sb.append("Average Confidence: ").append(String.format("%.2f%%", summary.getAvgConfidence() * 100)).append("\n");
            
            sb.append("\nClass-specific Metrics:\n");
            sb.append("---------------------\n");
            for (ClassMetrics classMetrics : summary.getClassMetrics().values()) {
                sb.append("Class: ").append(classMetrics.getClassName()).append("\n");
                sb.append("  Precision: ").append(String.format("%.2f%%", classMetrics.getPrecision() * 100)).append("\n");
                sb.append("  Recall: ").append(String.format("%.2f%%", classMetrics.getRecall() * 100)).append("\n");
                sb.append("  F1 Score: ").append(String.format("%.2f%%", classMetrics.getF1Score() * 100)).append("\n");
                sb.append("  Support: ").append(classMetrics.getSupport()).append("\n");
            }
            
            return sb.toString();
            
        } catch (Exception e) {
            logger.error("Error exporting performance data for model: {}", modelName, e);
            return "Error exporting performance data";
        }
    }
    
    // Inner classes
    public static class ModelPerformanceMetrics {
        private String modelName;
        private long totalPredictions;
        private long correctPredictions;
        private double overallAccuracy;
        private double avgConfidence;
        private Map<String, ClassMetrics> classMetrics;
        private LocalDateTime lastUpdated;
        private List<PerformanceSnapshot> performanceSnapshots;
        
        public ModelPerformanceMetrics(String modelName) {
            this.modelName = modelName;
            this.totalPredictions = 0;
            this.correctPredictions = 0;
            this.overallAccuracy = 0.0;
            this.avgConfidence = 0.0;
            this.classMetrics = new HashMap<>();
            this.performanceSnapshots = new ArrayList<>();
        }
        
        public void addPrediction(PredictionRecord record) {
            totalPredictions++;
            if (record.getPredictedClass().equals(record.getActualClass())) {
                correctPredictions++;
            }
            
            // Update accuracy
            overallAccuracy = (double) correctPredictions / totalPredictions;
            
            // Update average confidence
            avgConfidence = (avgConfidence * (totalPredictions - 1) + record.getConfidence()) / totalPredictions;
            
            // Update class metrics
            updateClassMetrics(record);
            
            // Add performance snapshot periodically
            if (totalPredictions % 100 == 0) {
                performanceSnapshots.add(new PerformanceSnapshot(
                    LocalDateTime.now(), totalPredictions, overallAccuracy, avgConfidence
                ));
            }
        }
        
        private void updateClassMetrics(PredictionRecord record) {
            String actualClass = record.getActualClass();
            ClassMetrics metrics = classMetrics.computeIfAbsent(
                actualClass, k -> new ClassMetrics(k, 0, 0, 0, 0, 0.0, 0.0, 0.0)
            );
            
            metrics.addPrediction(record);
        }
        
        // Getters and setters
        public String getModelName() { return modelName; }
        public void setModelName(String modelName) { this.modelName = modelName; }
        
        public long getTotalPredictions() { return totalPredictions; }
        public void setTotalPredictions(long totalPredictions) { this.totalPredictions = totalPredictions; }
        
        public long getCorrectPredictions() { return correctPredictions; }
        public void setCorrectPredictions(long correctPredictions) { this.correctPredictions = correctPredictions; }
        
        public double getOverallAccuracy() { return overallAccuracy; }
        public void setOverallAccuracy(double overallAccuracy) { this.overallAccuracy = overallAccuracy; }
        
        public double getAvgConfidence() { return avgConfidence; }
        public void setAvgConfidence(double avgConfidence) { this.avgConfidence = avgConfidence; }
        
        public Map<String, ClassMetrics> getClassMetrics() { return classMetrics; }
        public void setClassMetrics(Map<String, ClassMetrics> classMetrics) { this.classMetrics = classMetrics; }
        
        public LocalDateTime getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
        
        public List<PerformanceSnapshot> getPerformanceSnapshots() { return performanceSnapshots; }
        public void setPerformanceSnapshots(List<PerformanceSnapshot> performanceSnapshots) { this.performanceSnapshots = performanceSnapshots; }
    }
    
    public static class PredictionRecord {
        private String modelName;
        private String predictedClass;
        private String actualClass;
        private double confidence;
        private Map<String, Object> features;
        private LocalDateTime timestamp;
        
        public PredictionRecord(String modelName, String predictedClass, String actualClass, 
                            double confidence, Map<String, Object> features, LocalDateTime timestamp) {
            this.modelName = modelName;
            this.predictedClass = predictedClass;
            this.actualClass = actualClass;
            this.confidence = confidence;
            this.features = features;
            this.timestamp = timestamp;
        }
        
        // Getters
        public String getModelName() { return modelName; }
        public String getPredictedClass() { return predictedClass; }
        public String getActualClass() { return actualClass; }
        public double getConfidence() { return confidence; }
        public Map<String, Object> getFeatures() { return features; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
    
    public static class ClassMetrics {
        private String className;
        private long support;
        private long truePositives;
        private long falsePositives;
        private long falseNegatives;
        private double precision;
        private double recall;
        private double f1Score;
        
        public ClassMetrics(String className, long support, long truePositives, 
                          long falsePositives, long falseNegatives, 
                          double precision, double recall, double f1Score) {
            this.className = className;
            this.support = support;
            this.truePositives = truePositives;
            this.falsePositives = falsePositives;
            this.falseNegatives = falseNegatives;
            this.precision = precision;
            this.recall = recall;
            this.f1Score = f1Score;
        }
        
        public void addPrediction(PredictionRecord record) {
            support++;
            if (record.getPredictedClass().equals(className)) {
                if (record.getActualClass().equals(className)) {
                    truePositives++;
                } else {
                    falsePositives++;
                }
            } else if (record.getActualClass().equals(className)) {
                falseNegatives++;
            }
            
            // Recalculate metrics
            precision = (truePositives + falsePositives) > 0 ? 
                (double) truePositives / (truePositives + falsePositives) : 0.0;
            recall = (truePositives + falseNegatives) > 0 ? 
                (double) truePositives / (truePositives + falseNegatives) : 0.0;
            f1Score = (precision + recall) > 0 ? 
                2 * (precision * recall) / (precision + recall) : 0.0;
        }
        
        // Getters
        public String getClassName() { return className; }
        public long getSupport() { return support; }
        public long getTruePositives() { return truePositives; }
        public long getFalsePositives() { return falsePositives; }
        public long getFalseNegatives() { return falseNegatives; }
        public double getPrecision() { return precision; }
        public double getRecall() { return recall; }
        public double getF1Score() { return f1Score; }
    }
    
    public static class DriftDetection {
        private String modelName;
        private String driftType;
        private String description;
        private double baselineAccuracy;
        private double currentAccuracy;
        private double accuracyDrop;
        private LocalDateTime timestamp;
        
        public DriftDetection(String modelName, String driftType, String description, 
                            double baselineAccuracy, double currentAccuracy, 
                            double accuracyDrop, LocalDateTime timestamp) {
            this.modelName = modelName;
            this.driftType = driftType;
            this.description = description;
            this.baselineAccuracy = baselineAccuracy;
            this.currentAccuracy = currentAccuracy;
            this.accuracyDrop = accuracyDrop;
            this.timestamp = timestamp;
        }
        
        // Getters
        public String getModelName() { return modelName; }
        public String getDriftType() { return driftType; }
        public String getDescription() { return description; }
        public double getBaselineAccuracy() { return baselineAccuracy; }
        public double getCurrentAccuracy() { return currentAccuracy; }
        public double getAccuracyDrop() { return accuracyDrop; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
    
    public static class ModelPerformanceSummary {
        private String modelName;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private long totalPredictions;
        private double accuracy;
        private double precision;
        private double recall;
        private double f1Score;
        private double avgConfidence;
        private Map<String, Long> confidenceDistribution;
        private Map<String, ClassMetrics> classMetrics;
        
        public ModelPerformanceSummary(String modelName, LocalDateTime startTime, LocalDateTime endTime,
                                  long totalPredictions, double accuracy, double precision, 
                                  double recall, double f1Score, double avgConfidence,
                                  Map<String, Long> confidenceDistribution, 
                                  Map<String, ClassMetrics> classMetrics) {
            this.modelName = modelName;
            this.startTime = startTime;
            this.endTime = endTime;
            this.totalPredictions = totalPredictions;
            this.accuracy = accuracy;
            this.precision = precision;
            this.recall = recall;
            this.f1Score = f1Score;
            this.avgConfidence = avgConfidence;
            this.confidenceDistribution = confidenceDistribution;
            this.classMetrics = classMetrics;
        }
        
        // Getters
        public String getModelName() { return modelName; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public long getTotalPredictions() { return totalPredictions; }
        public double getAccuracy() { return accuracy; }
        public double getPrecision() { return precision; }
        public double getRecall() { return recall; }
        public double getF1Score() { return f1Score; }
        public double getAvgConfidence() { return avgConfidence; }
        public Map<String, Long> getConfidenceDistribution() { return confidenceDistribution; }
        public Map<String, ClassMetrics> getClassMetrics() { return classMetrics; }
    }
    
    public static class PerformanceSnapshot {
        private LocalDateTime timestamp;
        private long totalPredictions;
        private double accuracy;
        private double avgConfidence;
        
        public PerformanceSnapshot(LocalDateTime timestamp, long totalPredictions, 
                               double accuracy, double avgConfidence) {
            this.timestamp = timestamp;
            this.totalPredictions = totalPredictions;
            this.accuracy = accuracy;
            this.avgConfidence = avgConfidence;
        }
        
        // Getters
        public LocalDateTime getTimestamp() { return timestamp; }
        public long getTotalPredictions() { return totalPredictions; }
        public double getAccuracy() { return accuracy; }
        public double getAvgConfidence() { return avgConfidence; }
    }
    
    public static class BatchPredictionRecord {
        private String predictedClass;
        private String actualClass;
        private double confidence;
        private Map<String, Object> features;
        private LocalDateTime timestamp;
        
        public BatchPredictionRecord(String predictedClass, String actualClass, 
                                double confidence, Map<String, Object> features, 
                                LocalDateTime timestamp) {
            this.predictedClass = predictedClass;
            this.actualClass = actualClass;
            this.confidence = confidence;
            this.features = features;
            this.timestamp = timestamp;
        }
        
        // Getters
        public String getPredictedClass() { return predictedClass; }
        public String getActualClass() { return actualClass; }
        public double getConfidence() { return confidence; }
        public Map<String, Object> getFeatures() { return features; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
    
    public static class GlobalStatistics {
        private long totalModels;
        private long totalPredictions;
        private long totalRetrains;
        private double avgAccuracy;
        private long modelsWithDrift;
        private LocalDateTime timestamp;
        
        public GlobalStatistics(long totalModels, long totalPredictions, long totalRetrains,
                             double avgAccuracy, long modelsWithDrift, LocalDateTime timestamp) {
            this.totalModels = totalModels;
            this.totalPredictions = totalPredictions;
            this.totalRetrains = totalRetrains;
            this.avgAccuracy = avgAccuracy;
            this.modelsWithDrift = modelsWithDrift;
            this.timestamp = timestamp;
        }
        
        // Getters
        public long getTotalModels() { return totalModels; }
        public long getTotalPredictions() { return totalPredictions; }
        public long getTotalRetrains() { return totalRetrains; }
        public double getAvgAccuracy() { return avgAccuracy; }
        public long getModelsWithDrift() { return modelsWithDrift; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}
