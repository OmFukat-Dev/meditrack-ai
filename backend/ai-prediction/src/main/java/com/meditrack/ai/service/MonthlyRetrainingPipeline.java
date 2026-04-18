package com.meditrack.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class MonthlyRetrainingPipeline {
    
    private static final Logger logger = LoggerFactory.getLogger(MonthlyRetrainingPipeline.class);
    
    // Retraining pipeline storage
    private final Map<String, RetrainingSchedule> modelSchedules = new ConcurrentHashMap<>();
    private final Map<String, List<RetrainingResult>> retrainingHistory = new ConcurrentHashMap<>();
    private final Map<String, RetrainingMetrics> modelMetrics = new ConcurrentHashMap<>();
    
    // Schedule monthly retraining for a model
    public void scheduleMonthlyRetraining(String modelName, RetrainingSchedule schedule) {
        try {
            logger.info("Scheduling monthly retraining for model: {}", modelName);
            
            schedule.setModelName(modelName);
            schedule.setScheduleTimestamp(LocalDateTime.now());
            
            modelSchedules.put(modelName, schedule);
            
            logger.info("Monthly retraining scheduled for model: {} on day {} of each month", 
                       modelName, schedule.getDayOfMonth());
            
        } catch (Exception e) {
            logger.error("Error scheduling monthly retraining for model: {}", modelName, e);
            throw new RuntimeException("Failed to schedule retraining", e);
        }
    }
    
    // Execute retraining pipeline
    public RetrainingResult executeRetrainingPipeline(String modelName) {
        try {
            logger.info("Executing retraining pipeline for model: {}", modelName);
            
            RetrainingResult result = new RetrainingResult();
            result.setModelName(modelName);
            result.setStartedAt(LocalDateTime.now());
            
            // Get schedule
            RetrainingSchedule schedule = modelSchedules.get(modelName);
            if (schedule == null) {
                result.setSuccess(false);
                result.setMessage("No retraining schedule found for model: " + modelName);
                result.setCompletedAt(LocalDateTime.now());
                return result;
            }
            
            // Step 1: Data collection
            RetrainingStepResult dataCollectionResult = collectTrainingData(modelName, schedule);
            result.setDataCollectionResult(dataCollectionResult);
            
            if (!dataCollectionResult.isSuccess()) {
                result.setSuccess(false);
                result.setMessage("Data collection failed: " + dataCollectionResult.getMessage());
                result.setCompletedAt(LocalDateTime.now());
                return result;
            }
            
            // Step 2: Data validation
            RetrainingStepResult validationResult = validateTrainingData(modelName, dataCollectionResult);
            result.setValidationResult(validationResult);
            
            if (!validationResult.isSuccess()) {
                result.setSuccess(false);
                result.setMessage("Data validation failed: " + validationResult.getMessage());
                result.setCompletedAt(LocalDateTime.now());
                return result;
            }
            
            // Step 3: Model training
            RetrainingStepResult trainingResult = trainNewModel(modelName, dataCollectionResult, schedule);
            result.setTrainingResult(trainingResult);
            
            if (!trainingResult.isSuccess()) {
                result.setSuccess(false);
                result.setMessage("Model training failed: " + trainingResult.getMessage());
                result.setCompletedAt(LocalDateTime.now());
                return result;
            }
            
            // Step 4: Model evaluation
            RetrainingStepResult evaluationResult = evaluateNewModel(modelName, trainingResult, schedule);
            result.setEvaluationResult(evaluationResult);
            
            if (!evaluationResult.isSuccess()) {
                result.setSuccess(false);
                result.setMessage("Model evaluation failed: " + evaluationResult.getMessage());
                result.setCompletedAt(LocalDateTime.now());
                return result;
            }
            
            // Step 5: Model deployment
            RetrainingStepResult deploymentResult = deployNewModel(modelName, trainingResult, evaluationResult);
            result.setDeploymentResult(deploymentResult);
            
            if (!deploymentResult.isSuccess()) {
                result.setSuccess(false);
                result.setMessage("Model deployment failed: " + deploymentResult.getMessage());
                result.setCompletedAt(LocalDateTime.now());
                return result;
            }
            
            // Step 6: Cleanup
            RetrainingStepResult cleanupResult = cleanupOldModels(modelName, schedule);
            result.setCleanupResult(cleanupResult);
            
            // Calculate overall metrics
            RetrainingMetrics metrics = calculateRetrainingMetrics(result);
            result.setMetrics(metrics);
            
            // Update model metrics
            modelMetrics.put(modelName, metrics);
            
            // Store result
            retrainingHistory.computeIfAbsent(modelName, k -> new ArrayList<>()).add(result);
            
            result.setSuccess(true);
            result.setMessage("Retraining pipeline completed successfully");
            result.setCompletedAt(LocalDateTime.now());
            
            logger.info("Retraining pipeline completed successfully for model: {} in {} minutes", 
                       modelName, ChronoUnit.MINUTES.between(result.getStartedAt(), result.getCompletedAt()));
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error executing retraining pipeline for model: {}", modelName, e);
            
            RetrainingResult result = new RetrainingResult();
            result.setModelName(modelName);
            result.setStartedAt(LocalDateTime.now());
            result.setCompletedAt(LocalDateTime.now());
            result.setSuccess(false);
            result.setMessage("Retraining pipeline failed: " + e.getMessage());
            
            return result;
        }
    }
    
    // Get retraining schedule
    public RetrainingSchedule getRetrainingSchedule(String modelName) {
        return modelSchedules.get(modelName);
    }
    
    // Get all schedules
    public Map<String, RetrainingSchedule> getAllSchedules() {
        return new HashMap<>(modelSchedules);
    }
    
    // Get retraining history
    public List<RetrainingResult> getRetrainingHistory(String modelName, int limit) {
        List<RetrainingResult> history = retrainingHistory.getOrDefault(modelName, new ArrayList<>());
        return history.stream()
            .sorted((a, b) -> b.getStartedAt().compareTo(a.getStartedAt()))
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    // Get retraining metrics
    public RetrainingMetrics getRetrainingMetrics(String modelName) {
        return modelMetrics.get(modelName);
    }
    
    // Generate retraining report
    public String generateRetrainingReport(String modelName, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            StringBuilder report = new StringBuilder();
            report.append("Monthly Retraining Pipeline Report\n");
            report.append("=================================\n");
            report.append("Model: ").append(modelName).append("\n");
            report.append("Period: ").append(startTime).append(" to ").append(endTime).append("\n");
            report.append("Generated: ").append(LocalDateTime.now()).append("\n\n");
            
            // Schedule information
            RetrainingSchedule schedule = modelSchedules.get(modelName);
            if (schedule != null) {
                report.append("Retraining Schedule:\n");
                report.append("- Day of Month: ").append(schedule.getDayOfMonth()).append("\n");
                report.append("- Minimum Data Points: ").append(schedule.getMinDataPoints()).append("\n");
                report.append("- Performance Threshold: ").append(String.format("%.3f", schedule.getPerformanceThreshold())).append("\n");
                report.append("- Auto-deploy: ").append(schedule.isAutoDeploy()).append("\n");
                report.append("- Backup Old Models: ").append(schedule.isBackupOldModels()).append("\n\n");
            }
            
            // Recent retraining results
            List<RetrainingResult> recentResults = getRetrainingHistory(modelName, 10);
            if (!recentResults.isEmpty()) {
                report.append("Recent Retraining Results:\n");
                report.append("---------------------------\n");
                
                for (RetrainingResult result : recentResults) {
                    report.append("Started: ").append(result.getStartedAt()).append("\n");
                    report.append("Completed: ").append(result.getCompletedAt()).append("\n");
                    report.append("Success: ").append(result.isSuccess() ? "YES" : "NO").append("\n");
                    report.append("Message: ").append(result.getMessage()).append("\n");
                    
                    if (result.getMetrics() != null) {
                        RetrainingMetrics metrics = result.getMetrics();
                        report.append("- Data Points: ").append(metrics.getDataPointsCollected()).append("\n");
                        report.append("- Training Time: ").append(metrics.getTrainingTimeMinutes()).append(" minutes\n");
                        report.append("- New Accuracy: ").append(String.format("%.3f", metrics.getNewModelAccuracy())).append("\n");
                        report.append("- Old Accuracy: ").append(String.format("%.3f", metrics.getOldModelAccuracy())).append("\n");
                        report.append("- Accuracy Improvement: ").append(String.format("%.3f", metrics.getAccuracyImprovement())).append("\n");
                    }
                    
                    report.append("---\n");
                }
            } else {
                report.append("No retraining results found in the specified period.\n");
            }
            
            // Overall statistics
            report.append("\nRetraining Statistics:\n");
            report.append("---------------------\n");
            
            long totalRetrainings = recentResults.size();
            long successfulRetrainings = recentResults.stream()
                .mapToLong(result -> result.isSuccess() ? 1 : 0)
                .sum();
            
            report.append("Total Retrainings: ").append(totalRetrainings).append("\n");
            report.append("Successful: ").append(successfulRetrainings).append("\n");
            report.append("Failed: ").append(totalRetrainings - successfulRetrainings).append("\n");
            report.append("Success Rate: ").append(String.format("%.1f%%", 
                totalRetrainings > 0 ? (double) successfulRetrainings / totalRetrainings * 100 : 0)).append("\n");
            
            // Performance improvements
            List<Double> improvements = recentResults.stream()
                .filter(RetrainingResult::isSuccess)
                .map(result -> result.getMetrics().getAccuracyImprovement())
                .collect(Collectors.toList());
            
            if (!improvements.isEmpty()) {
                double avgImprovement = improvements.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                double maxImprovement = improvements.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
                double minImprovement = improvements.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
                
                report.append("Average Accuracy Improvement: ").append(String.format("%.3f", avgImprovement)).append("\n");
                report.append("Best Improvement: ").append(String.format("%.3f", maxImprovement)).append("\n");
                report.append("Worst Improvement: ").append(String.format("%.3f", minImprovement)).append("\n");
            }
            
            return report.toString();
            
        } catch (Exception e) {
            logger.error("Error generating retraining report for model: {}", modelName, e);
            return "Error generating retraining report.";
        }
    }
    
    // Scheduled monthly retraining check
    @Scheduled(cron = "0 0 2 1 * ?") // 2 AM on 1st day of each month
    public void scheduledMonthlyRetraining() {
        try {
            logger.info("Starting scheduled monthly retraining check");
            
            LocalDateTime now = LocalDateTime.now();
            int currentDay = now.getDayOfMonth();
            
            for (Map.Entry<String, RetrainingSchedule> entry : modelSchedules.entrySet()) {
                String modelName = entry.getKey();
                RetrainingSchedule schedule = entry.getValue();
                
                if (schedule.getDayOfMonth() == currentDay) {
                    logger.info("Executing scheduled retraining for model: {}", modelName);
                    
                    try {
                        RetrainingResult result = executeRetrainingPipeline(modelName);
                        
                        if (result.isSuccess()) {
                            logger.info("Scheduled retraining completed successfully for model: {}", modelName);
                        } else {
                            logger.error("Scheduled retraining failed for model: {}: {}", 
                                       modelName, result.getMessage());
                        }
                    } catch (Exception e) {
                        logger.error("Error in scheduled retraining for model: {}", modelName, e);
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("Error in scheduled monthly retraining", e);
        }
    }
    
    // Private helper methods
    private RetrainingStepResult collectTrainingData(String modelName, RetrainingSchedule schedule) {
        try {
            logger.debug("Collecting training data for model: {}", modelName);
            
            // Simulate data collection
            int dataPoints = new Random().nextInt(5000) + 1000; // 1000-6000 data points
            
            if (dataPoints < schedule.getMinDataPoints()) {
                return new RetrainingStepResult(
                    false, 
                    "Insufficient data points: " + dataPoints + " (minimum: " + schedule.getMinDataPoints() + ")",
                    Map.of("dataPoints", dataPoints)
                );
            }
            
            return new RetrainingStepResult(
                true, 
                "Data collection successful",
                Map.of(
                    "dataPoints", dataPoints,
                    "collectionTime", new Random().nextInt(10) + 1 // 1-10 minutes
                )
            );
            
        } catch (Exception e) {
            logger.error("Error collecting training data for model: {}", modelName, e);
            return new RetrainingStepResult(false, "Data collection failed: " + e.getMessage(), null);
        }
    }
    
    private RetrainingStepResult validateTrainingData(String modelName, RetrainingStepResult dataResult) {
        try {
            logger.debug("Validating training data for model: {}", modelName);
            
            int dataPoints = (Integer) dataResult.getDetails().get("dataPoints");
            
            // Simulate data validation
            boolean hasMissingValues = new Random().nextBoolean();
            boolean hasOutliers = new Random().nextBoolean();
            
            if (hasMissingValues || hasOutliers) {
                return new RetrainingStepResult(
                    false, 
                    "Data validation failed: " + 
                    (hasMissingValues ? "missing values " : "") + 
                    (hasOutliers ? "outliers detected" : ""),
                    Map.of(
                        "hasMissingValues", hasMissingValues,
                        "hasOutliers", hasOutliers
                    )
                );
            }
            
            return new RetrainingStepResult(
                true, 
                "Data validation successful",
                Map.of(
                    "dataPoints", dataPoints,
                    "validationTime", new Random().nextInt(5) + 1 // 1-5 minutes
                )
            );
            
        } catch (Exception e) {
            logger.error("Error validating training data for model: {}", modelName, e);
            return new RetrainingStepResult(false, "Data validation failed: " + e.getMessage(), null);
        }
    }
    
    private RetrainingStepResult trainNewModel(String modelName, RetrainingStepResult dataResult, RetrainingSchedule schedule) {
        try {
            logger.debug("Training new model for: {}", modelName);
            
            int dataPoints = (Integer) dataResult.getDetails().get("dataPoints");
            
            // Simulate model training
            int trainingTime = new Random().nextInt(30) + 10; // 10-40 minutes
            double newAccuracy = 0.7 + new Random().nextDouble() * 0.25; // 0.7-0.95
            
            return new RetrainingStepResult(
                true, 
                "Model training successful",
                Map.of(
                    "dataPoints", dataPoints,
                    "trainingTime", trainingTime,
                    "newAccuracy", newAccuracy,
                    "modelPath", "./models/" + modelName + "_" + System.currentTimeMillis() + ".model"
                )
            );
            
        } catch (Exception e) {
            logger.error("Error training new model for: {}", modelName, e);
            return new RetrainingStepResult(false, "Model training failed: " + e.getMessage(), null);
        }
    }
    
    private RetrainingStepResult evaluateNewModel(String modelName, RetrainingStepResult trainingResult, RetrainingSchedule schedule) {
        try {
            logger.debug("Evaluating new model for: {}", modelName);
            
            double newAccuracy = (Double) trainingResult.getDetails().get("newAccuracy");
            
            // Get old model accuracy (simulated)
            double oldAccuracy = 0.6 + new Random().nextDouble() * 0.3; // 0.6-0.9
            
            // Check if new model meets performance threshold
            if (newAccuracy < schedule.getPerformanceThreshold()) {
                return new RetrainingStepResult(
                    false, 
                    "New model accuracy below threshold: " + String.format("%.3f", newAccuracy) + 
                    " (threshold: " + String.format("%.3f", schedule.getPerformanceThreshold()) + ")",
                    Map.of(
                        "newAccuracy", newAccuracy,
                        "oldAccuracy", oldAccuracy,
                        "threshold", schedule.getPerformanceThreshold()
                    )
                );
            }
            
            // Check if new model is significantly better than old model
            double improvement = newAccuracy - oldAccuracy;
            if (improvement < 0.01) { // Less than 1% improvement
                return new RetrainingStepResult(
                    false, 
                    "New model does not provide significant improvement: " + String.format("%.3f", improvement),
                    Map.of(
                        "newAccuracy", newAccuracy,
                        "oldAccuracy", oldAccuracy,
                        "improvement", improvement
                    )
                );
            }
            
            return new RetrainingStepResult(
                true, 
                "Model evaluation successful",
                Map.of(
                    "newAccuracy", newAccuracy,
                    "oldAccuracy", oldAccuracy,
                    "improvement", improvement,
                    "evaluationTime", new Random().nextInt(10) + 1 // 1-10 minutes
                )
            );
            
        } catch (Exception e) {
            logger.error("Error evaluating new model for: {}", modelName, e);
            return new RetrainingStepResult(false, "Model evaluation failed: " + e.getMessage(), null);
        }
    }
    
    private RetrainingStepResult deployNewModel(String modelName, RetrainingStepResult trainingResult, RetrainingStepResult evaluationResult) {
        try {
            logger.debug("Deploying new model for: {}", modelName);
            
            if (!evaluationResult.isSuccess()) {
                return new RetrainingStepResult(false, "Cannot deploy model - evaluation failed", null);
            }
            
            // Simulate model deployment
            String newModelPath = (String) trainingResult.getDetails().get("modelPath");
            String backupPath = "./models/" + modelName + "_backup_" + System.currentTimeMillis() + ".model";
            
            return new RetrainingStepResult(
                true, 
                "Model deployment successful",
                Map.of(
                    "newModelPath", newModelPath,
                    "backupPath", backupPath,
                    "deploymentTime", new Random().nextInt(5) + 1 // 1-5 minutes
                )
            );
            
        } catch (Exception e) {
            logger.error("Error deploying new model for: {}", modelName, e);
            return new RetrainingStepResult(false, "Model deployment failed: " + e.getMessage(), null);
        }
    }
    
    private RetrainingStepResult cleanupOldModels(String modelName, RetrainingSchedule schedule) {
        try {
            logger.debug("Cleaning up old models for: {}", modelName);
            
            // Simulate cleanup
            int modelsCleaned = new Random().nextInt(3) + 1; // 1-3 models cleaned
            
            return new RetrainingStepResult(
                true, 
                "Model cleanup successful",
                Map.of(
                    "modelsCleaned", modelsCleaned,
                    "cleanupTime", new Random().nextInt(3) + 1 // 1-3 minutes
                )
            );
            
        } catch (Exception e) {
            logger.error("Error cleaning up old models for: {}", modelName, e);
            return new RetrainingStepResult(false, "Model cleanup failed: " + e.getMessage(), null);
        }
    }
    
    private RetrainingMetrics calculateRetrainingMetrics(RetrainingResult result) {
        RetrainingMetrics metrics = new RetrainingMetrics();
        
        // Data collection metrics
        if (result.getDataCollectionResult() != null) {
            metrics.setDataPointsCollected((Integer) result.getDataCollectionResult().getDetails().get("dataPoints"));
        }
        
        // Training metrics
        if (result.getTrainingResult() != null) {
            metrics.setTrainingTimeMinutes((Integer) result.getTrainingResult().getDetails().get("trainingTime"));
            metrics.setNewModelAccuracy((Double) result.getTrainingResult().getDetails().get("newAccuracy"));
        }
        
        // Evaluation metrics
        if (result.getEvaluationResult() != null) {
            metrics.setOldModelAccuracy((Double) result.getEvaluationResult().getDetails().get("oldAccuracy"));
            metrics.setAccuracyImprovement((Double) result.getEvaluationResult().getDetails().get("improvement"));
        }
        
        // Overall metrics
        metrics.setTotalDuration(ChronoUnit.MINUTES.between(result.getStartedAt(), result.getCompletedAt()));
        metrics.setSuccess(result.isSuccess());
        
        return metrics;
    }
    
    // Inner classes
    public static class RetrainingSchedule {
        private String modelName;
        private int dayOfMonth = 1; // Default to 1st day of month
        private int minDataPoints = 1000;
        private double performanceThreshold = 0.8;
        private boolean autoDeploy = true;
        private boolean backupOldModels = true;
        private LocalDateTime scheduleTimestamp;
        
        // Getters and setters
        public String getModelName() { return modelName; }
        public void setModelName(String modelName) { this.modelName = modelName; }
        
        public int getDayOfMonth() { return dayOfMonth; }
        public void setDayOfMonth(int dayOfMonth) { this.dayOfMonth = dayOfMonth; }
        
        public int getMinDataPoints() { return minDataPoints; }
        public void setMinDataPoints(int minDataPoints) { this.minDataPoints = minDataPoints; }
        
        public double getPerformanceThreshold() { return performanceThreshold; }
        public void setPerformanceThreshold(double performanceThreshold) { this.performanceThreshold = performanceThreshold; }
        
        public boolean isAutoDeploy() { return autoDeploy; }
        public void setAutoDeploy(boolean autoDeploy) { this.autoDeploy = autoDeploy; }
        
        public boolean isBackupOldModels() { return backupOldModels; }
        public void setBackupOldModels(boolean backupOldModels) { this.backupOldModels = backupOldModels; }
        
        public LocalDateTime getScheduleTimestamp() { return scheduleTimestamp; }
        public void setScheduleTimestamp(LocalDateTime scheduleTimestamp) { this.scheduleTimestamp = scheduleTimestamp; }
    }
    
    public static class RetrainingResult {
        private String modelName;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private boolean success;
        private String message;
        private RetrainingStepResult dataCollectionResult;
        private RetrainingStepResult validationResult;
        private RetrainingStepResult trainingResult;
        private RetrainingStepResult evaluationResult;
        private RetrainingStepResult deploymentResult;
        private RetrainingStepResult cleanupResult;
        private RetrainingMetrics metrics;
        
        // Getters and setters
        public String getModelName() { return modelName; }
        public void setModelName(String modelName) { this.modelName = modelName; }
        
        public LocalDateTime getStartedAt() { return startedAt; }
        public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
        
        public LocalDateTime getCompletedAt() { return completedAt; }
        public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public RetrainingStepResult getDataCollectionResult() { return dataCollectionResult; }
        public void setDataCollectionResult(RetrainingStepResult dataCollectionResult) { this.dataCollectionResult = dataCollectionResult; }
        
        public RetrainingStepResult getValidationResult() { return validationResult; }
        public void setValidationResult(RetrainingStepResult validationResult) { this.validationResult = validationResult; }
        
        public RetrainingStepResult getTrainingResult() { return trainingResult; }
        public void setTrainingResult(RetrainingStepResult trainingResult) { this.trainingResult = trainingResult; }
        
        public RetrainingStepResult getEvaluationResult() { return evaluationResult; }
        public void setEvaluationResult(RetrainingStepResult evaluationResult) { this.evaluationResult = evaluationResult; }
        
        public RetrainingStepResult getDeploymentResult() { return deploymentResult; }
        public void setDeploymentResult(RetrainingStepResult deploymentResult) { this.deploymentResult = deploymentResult; }
        
        public RetrainingStepResult getCleanupResult() { return cleanupResult; }
        public void setCleanupResult(RetrainingStepResult cleanupResult) { this.cleanupResult = cleanupResult; }
        
        public RetrainingMetrics getMetrics() { return metrics; }
        public void setMetrics(RetrainingMetrics metrics) { this.metrics = metrics; }
    }
    
    public static class RetrainingStepResult {
        private boolean success;
        private String message;
        private Map<String, Object> details;
        
        public RetrainingStepResult(boolean success, String message, Map<String, Object> details) {
            this.success = success;
            this.message = message;
            this.details = details;
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Map<String, Object> getDetails() { return details; }
    }
    
    public static class RetrainingMetrics {
        private int dataPointsCollected;
        private int trainingTimeMinutes;
        private double newModelAccuracy;
        private double oldModelAccuracy;
        private double accuracyImprovement;
        private long totalDuration;
        private boolean success;
        
        // Getters and setters
        public int getDataPointsCollected() { return dataPointsCollected; }
        public void setDataPointsCollected(int dataPointsCollected) { this.dataPointsCollected = dataPointsCollected; }
        
        public int getTrainingTimeMinutes() { return trainingTimeMinutes; }
        public void setTrainingTimeMinutes(int trainingTimeMinutes) { this.trainingTimeMinutes = trainingTimeMinutes; }
        
        public double getNewModelAccuracy() { return newModelAccuracy; }
        public void setNewModelAccuracy(double newModelAccuracy) { this.newModelAccuracy = newModelAccuracy; }
        
        public double getOldModelAccuracy() { return oldModelAccuracy; }
        public void setOldModelAccuracy(double oldModelAccuracy) { this.oldModelAccuracy = oldModelAccuracy; }
        
        public double getAccuracyImprovement() { return accuracyImprovement; }
        public void setAccuracyImprovement(double accuracyImprovement) { this.accuracyImprovement = accuracyImprovement; }
        
        public long getTotalDuration() { return totalDuration; }
        public void setTotalDuration(long totalDuration) { this.totalDuration = totalDuration; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
    }
}
