package com.meditrack.ai.controller;

import com.meditrack.ai.service.*;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/explainability-monitoring")
@CrossOrigin(origins = "*")
public class ExplainabilityMonitoringController {
    
    private static final Logger logger = LoggerFactory.getLogger(ExplainabilityMonitoringController.class);
    
    @Autowired
    private FeatureImportanceService featureImportanceService;
    
    @Autowired
    private ModelPerformanceDashboardService dashboardService;
    
    @Autowired
    private DriftDetectionService driftDetectionService;
    
    @Autowired
    private MonthlyRetrainingPipeline retrainingPipeline;
    
    @Autowired
    private ModelPerformanceTrackingService performanceTrackingService;
    
    @Autowired
    private WekaService wekaService;
    
    // Feature Importance Endpoints
    @PostMapping("/feature-importance/extract/{modelName}")
    @Counted(value = "explainability.feature.importance.extract", description = "Number of feature importance extractions")
    @Timed(value = "explainability.feature.importance.extract.time", description = "Time taken to extract feature importance")
    public ResponseEntity<?> extractFeatureImportance(@PathVariable String modelName) {
        try {
            logger.info("Extracting feature importance for model: {}", modelName);
            
            // Get model and data
            wekaService.WekaService.ModelInfo modelInfo = wekaService.getModelInfo(modelName);
            if (modelInfo == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Model not found", "modelName", modelName));
            }
            
            // Extract feature importance (simplified - would need actual model and data)
            FeatureImportanceService.FeatureImportanceResults results = 
                featureImportanceService.extractFeatureImportance(modelName, null, null);
            
            return ResponseEntity.ok(results);
            
        } catch (Exception e) {
            logger.error("Error extracting feature importance for model: {}", modelName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to extract feature importance", "message", e.getMessage()));
        }
    }
    
    @GetMapping("/feature-importance/{modelName}")
    @Counted(value = "explainability.feature.importance.get", description = "Number of feature importance requests")
    @Timed(value = "explainability.feature.importance.get.time", description = "Time taken to get feature importance")
    public ResponseEntity<?> getFeatureImportance(@PathVariable String modelName) {
        try {
            FeatureImportanceService.FeatureImportanceResults results = 
                featureImportanceService.getFeatureImportance(modelName);
            
            if (results == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Feature importance not found", "modelName", modelName));
            }
            
            return ResponseEntity.ok(results);
            
        } catch (Exception e) {
            logger.error("Error getting feature importance for model: {}", modelName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get feature importance", "message", e.getMessage()));
        }
    }
    
    @GetMapping("/feature-importance")
    @Counted(value = "explainability.feature.importance.all", description = "Number of all feature importance requests")
    @Timed(value = "explainability.feature.importance.all.time", description = "Time taken to get all feature importance")
    public ResponseEntity<?> getAllFeatureImportance() {
        try {
            Map<String, FeatureImportanceService.FeatureImportanceResults> allResults = 
                featureImportanceService.getAllFeatureImportance();
            
            return ResponseEntity.ok(allResults);
            
        } catch (Exception e) {
            logger.error("Error getting all feature importance", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get all feature importance", "message", e.getMessage()));
        }
    }
    
    @GetMapping("/feature-importance/report/{modelName}")
    @Counted(value = "explainability.feature.importance.report", description = "Number of feature importance report requests")
    @Timed(value = "explainability.feature.importance.report.time", description = "Time taken to generate feature importance report")
    public ResponseEntity<?> generateFeatureImportanceReport(@PathVariable String modelName) {
        try {
            String report = featureImportanceService.generateFeatureImportanceReport(modelName);
            
            return ResponseEntity.ok(Map.of(
                "modelName", modelName,
                "report", report,
                "generatedAt", LocalDateTime.now()
            ));
            
        } catch (Exception e) {
            logger.error("Error generating feature importance report for model: {}", modelName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to generate report", "message", e.getMessage()));
        }
    }
    
    // Model Performance Dashboard Endpoints
    @PostMapping("/dashboard/generate/{modelName}")
    @Counted(value = "explainability.dashboard.generate", description = "Number of dashboard generations")
    @Timed(value = "explainability.dashboard.generate.time", description = "Time taken to generate dashboard")
    public ResponseEntity<?> generateDashboard(@PathVariable String modelName) {
        try {
            logger.info("Generating dashboard for model: {}", modelName);
            
            // Get model metrics
            ModelPerformanceTrackingService.ModelPerformanceMetrics metrics = 
                performanceTrackingService.getModelMetrics(modelName);
            if (metrics == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Model metrics not found", "modelName", modelName));
            }
            
            // Get feature importance
            FeatureImportanceService.FeatureImportanceResults featureImportance = 
                featureImportanceService.getFeatureImportance(modelName);
            
            // Generate dashboard data
            ModelPerformanceDashboardService.ModelDashboardData dashboardData = 
                dashboardService.generateDashboardData(modelName, metrics, featureImportance);
            
            return ResponseEntity.ok(dashboardData);
            
        } catch (Exception e) {
            logger.error("Error generating dashboard for model: {}", modelName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to generate dashboard", "message", e.getMessage()));
        }
    }
    
    @GetMapping("/dashboard/{modelName}")
    @Counted(value = "explainability.dashboard.get", description = "Number of dashboard requests")
    @Timed(value = "explainability.dashboard.get.time", description = "Time taken to get dashboard")
    public ResponseEntity<?> getDashboard(@PathVariable String modelName) {
        try {
            Map<String, ModelPerformanceDashboardService.ModelDashboardData> allDashboards = 
                dashboardService.getAllDashboardData();
            
            ModelPerformanceDashboardService.ModelDashboardData dashboardData = 
                allDashboards.get(modelName);
            
            if (dashboardData == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Dashboard not found", "modelName", modelName));
            }
            
            return ResponseEntity.ok(dashboardData);
            
        } catch (Exception e) {
            logger.error("Error getting dashboard for model: {}", modelName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get dashboard", "message", e.getMessage()));
        }
    }
    
    @GetMapping("/dashboard/overview")
    @Counted(value = "explainability.dashboard.overview", description = "Number of dashboard overview requests")
    @Timed(value = "explainability.dashboard.overview.time", description = "Time taken to get dashboard overview")
    public ResponseEntity<?> getDashboardOverview() {
        try {
            ModelPerformanceDashboardService.DashboardOverview overview = 
                dashboardService.getDashboardOverview();
            
            return ResponseEntity.ok(overview);
            
        } catch (Exception e) {
            logger.error("Error getting dashboard overview", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get dashboard overview", "message", e.getMessage()));
        }
    }
    
    @GetMapping("/dashboard/report/{modelName}")
    @Counted(value = "explainability.dashboard.report", description = "Number of dashboard report requests")
    @Timed(value = "explainability.dashboard.report.time", description = "Time taken to generate dashboard report")
    public ResponseEntity<?> generateDashboardReport(@PathVariable String modelName,
                                                  @RequestParam(required = false) LocalDateTime startTime,
                                                  @RequestParam(required = false) LocalDateTime endTime) {
        try {
            if (startTime == null) {
                startTime = LocalDateTime.now().minusHours(24);
            }
            if (endTime == null) {
                endTime = LocalDateTime.now();
            }
            
            String report = dashboardService.generatePerformanceReport(modelName, startTime, endTime);
            
            return ResponseEntity.ok(Map.of(
                "modelName", modelName,
                "startTime", startTime,
                "endTime", endTime,
                "report", report,
                "generatedAt", LocalDateTime.now()
            ));
            
        } catch (Exception e) {
            logger.error("Error generating dashboard report for model: {}", modelName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to generate report", "message", e.getMessage()));
        }
    }
    
    // Drift Detection Endpoints
    @PostMapping("/drift/detect/{modelName}")
    @Counted(value = "explainability.drift.detect", description = "Number of drift detections")
    @Timed(value = "explainability.drift.detect.time", description = "Time taken to detect drift")
    public ResponseEntity<?> detectDrift(@PathVariable String modelName) {
        try {
            logger.info("Detecting drift for model: {}", modelName);
            
            // Get model metrics
            ModelPerformanceTrackingService.ModelPerformanceMetrics metrics = 
                performanceTrackingService.getModelMetrics(modelName);
            if (metrics == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Model metrics not found", "modelName", modelName));
            }
            
            // Get recent predictions (simplified)
            List<ModelPerformanceTrackingService.PredictionRecord> recentPredictions = 
                performanceTrackingService.getPredictionHistory(modelName, 100);
            
            // Detect drift
            DriftDetectionService.DriftDetectionResult driftResult = 
                driftDetectionService.detectModelDrift(modelName, metrics, recentPredictions);
            
            return ResponseEntity.ok(driftResult);
            
        } catch (Exception e) {
            logger.error("Error detecting drift for model: {}", modelName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to detect drift", "message", e.getMessage()));
        }
    }
    
    @PostMapping("/drift/baseline/create/{modelName}")
    @Counted(value = "explainability.drift.baseline.create", description = "Number of baseline creations")
    @Timed(value = "explainability.drift.baseline.create.time", description = "Time taken to create baseline")
    public ResponseEntity<?> createDriftBaseline(@PathVariable String modelName) {
        try {
            logger.info("Creating drift baseline for model: {}", modelName);
            
            // Get model metrics
            ModelPerformanceTrackingService.ModelPerformanceMetrics metrics = 
                performanceTrackingService.getModelMetrics(modelName);
            if (metrics == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Model metrics not found", "modelName", modelName));
            }
            
            // Create baseline
            DriftDetectionService.DriftBaseline baseline = 
                driftDetectionService.createBaseline(modelName, metrics);
            
            return ResponseEntity.ok(baseline);
            
        } catch (Exception e) {
            logger.error("Error creating drift baseline for model: {}", modelName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to create baseline", "message", e.getMessage()));
        }
    }
    
    @GetMapping("/drift/history/{modelName}")
    @Counted(value = "explainability.drift.history", description = "Number of drift history requests")
    @Timed(value = "explainability.drift.history.time", description = "Time taken to get drift history")
    public ResponseEntity<?> getDriftHistory(@PathVariable String modelName, 
                                         @RequestParam(defaultValue = "50") int limit) {
        try {
            List<DriftDetectionService.DriftResult> history = 
                driftDetectionService.getDriftHistory(modelName, limit);
            
            return ResponseEntity.ok(history);
            
        } catch (Exception e) {
            logger.error("Error getting drift history for model: {}", modelName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get drift history", "message", e.getMessage()));
        }
    }
    
    @GetMapping("/drift/all")
    @Counted(value = "explainability.drift.all", description = "Number of all drift requests")
    @Timed(value = "explainability.drift.all.time", description = "Time taken to get all drift")
    public ResponseEntity<?> getAllDriftResults() {
        try {
            Map<String, List<DriftDetectionService.DriftResult>> allDriftResults = 
                driftDetectionService.getAllDriftResults();
            
            return ResponseEntity.ok(allDriftResults);
            
        } catch (Exception e) {
            logger.error("Error getting all drift results", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get all drift results", "message", e.getMessage()));
        }
    }
    
    @GetMapping("/drift/report/{modelName}")
    @Counted(value = "explainability.drift.report", description = "Number of drift report requests")
    @Timed(value = "explainability.drift.report.time", description = "Time taken to generate drift report")
    public ResponseEntity<?> generateDriftReport(@PathVariable String modelName,
                                              @RequestParam(required = false) LocalDateTime startTime,
                                              @RequestParam(required = false) LocalDateTime endTime) {
        try {
            if (startTime == null) {
                startTime = LocalDateTime.now().minusDays(30);
            }
            if (endTime == null) {
                endTime = LocalDateTime.now();
            }
            
            String report = driftDetectionService.generateDriftReport(modelName, startTime, endTime);
            
            return ResponseEntity.ok(Map.of(
                "modelName", modelName,
                "startTime", startTime,
                "endTime", endTime,
                "report", report,
                "generatedAt", LocalDateTime.now()
            ));
            
        } catch (Exception e) {
            logger.error("Error generating drift report for model: {}", modelName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to generate report", "message", e.getMessage()));
        }
    }
    
    // Monthly Retraining Pipeline Endpoints
    @PostMapping("/retraining/schedule/{modelName}")
    @Counted(value = "explainability.retraining.schedule", description = "Number of retraining schedules")
    @Timed(value = "explainability.retraining.schedule.time", description = "Time taken to schedule retraining")
    public ResponseEntity<?> scheduleRetraining(@PathVariable String modelName, 
                                            @Valid @RequestBody RetrainingScheduleRequest request) {
        try {
            logger.info("Scheduling monthly retraining for model: {}", modelName);
            
            MonthlyRetrainingPipeline.RetrainingSchedule schedule = 
                new MonthlyRetrainingPipeline.RetrainingSchedule();
            schedule.setDayOfMonth(request.getDayOfMonth());
            schedule.setMinDataPoints(request.getMinDataPoints());
            schedule.setPerformanceThreshold(request.getPerformanceThreshold());
            schedule.setAutoDeploy(request.isAutoDeploy());
            schedule.setBackupOldModels(request.isBackupOldModels());
            
            retrainingPipeline.scheduleMonthlyRetraining(modelName, schedule);
            
            return ResponseEntity.ok(Map.of(
                "message", "Retraining scheduled successfully",
                "modelName", modelName,
                "schedule", schedule
            ));
            
        } catch (Exception e) {
            logger.error("Error scheduling retraining for model: {}", modelName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to schedule retraining", "message", e.getMessage()));
        }
    }
    
    @PostMapping("/retraining/execute/{modelName}")
    @Counted(value = "explainability.retraining.execute", description = "Number of retraining executions")
    @Timed(value = "explainability.retraining.execute.time", description = "Time taken to execute retraining")
    public ResponseEntity<?> executeRetraining(@PathVariable String modelName) {
        try {
            logger.info("Executing retraining pipeline for model: {}", modelName);
            
            MonthlyRetrainingPipeline.RetrainingResult result = 
                retrainingPipeline.executeRetrainingPipeline(modelName);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error executing retraining for model: {}", modelName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to execute retraining", "message", e.getMessage()));
        }
    }
    
    @GetMapping("/retraining/schedule/{modelName}")
    @Counted(value = "explainability.retraining.schedule.get", description = "Number of schedule requests")
    @Timed(value = "explainability.retraining.schedule.get.time", description = "Time taken to get schedule")
    public ResponseEntity<?> getRetrainingSchedule(@PathVariable String modelName) {
        try {
            MonthlyRetrainingPipeline.RetrainingSchedule schedule = 
                retrainingPipeline.getRetrainingSchedule(modelName);
            
            if (schedule == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Retraining schedule not found", "modelName", modelName));
            }
            
            return ResponseEntity.ok(schedule);
            
        } catch (Exception e) {
            logger.error("Error getting retraining schedule for model: {}", modelName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get schedule", "message", e.getMessage()));
        }
    }
    
    @GetMapping("/retraining/schedules")
    @Counted(value = "explainability.retraining.schedules.all", description = "Number of all schedules requests")
    @Timed(value = "explainability.retraining.schedules.all.time", description = "Time taken to get all schedules")
    public ResponseEntity<?> getAllRetrainingSchedules() {
        try {
            Map<String, MonthlyRetrainingPipeline.RetrainingSchedule> allSchedules = 
                retrainingPipeline.getAllSchedules();
            
            return ResponseEntity.ok(allSchedules);
            
        } catch (Exception e) {
            logger.error("Error getting all retraining schedules", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get all schedules", "message", e.getMessage()));
        }
    }
    
    @GetMapping("/retraining/history/{modelName}")
    @Counted(value = "explainability.retraining.history", description = "Number of retraining history requests")
    @Timed(value = "explainability.retraining.history.time", description = "Time taken to get retraining history")
    public ResponseEntity<?> getRetrainingHistory(@PathVariable String modelName, 
                                              @RequestParam(defaultValue = "20") int limit) {
        try {
            List<MonthlyRetrainingPipeline.RetrainingResult> history = 
                retrainingPipeline.getRetrainingHistory(modelName, limit);
            
            return ResponseEntity.ok(history);
            
        } catch (Exception e) {
            logger.error("Error getting retraining history for model: {}", modelName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get retraining history", "message", e.getMessage()));
        }
    }
    
    @GetMapping("/retraining/metrics/{modelName}")
    @Counted(value = "explainability.retraining.metrics", description = "Number of retraining metrics requests")
    @Timed(value = "explainability.retraining.metrics.time", description = "Time taken to get retraining metrics")
    public ResponseEntity<?> getRetrainingMetrics(@PathVariable String modelName) {
        try {
            MonthlyRetrainingPipeline.RetrainingMetrics metrics = 
                retrainingPipeline.getRetrainingMetrics(modelName);
            
            if (metrics == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Retraining metrics not found", "modelName", modelName));
            }
            
            return ResponseEntity.ok(metrics);
            
        } catch (Exception e) {
            logger.error("Error getting retraining metrics for model: {}", modelName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get retraining metrics", "message", e.getMessage()));
        }
    }
    
    @GetMapping("/retraining/report/{modelName}")
    @Counted(value = "explainability.retraining.report", description = "Number of retraining report requests")
    @Timed(value = "explainability.retraining.report.time", description = "Time taken to generate retraining report")
    public ResponseEntity<?> generateRetrainingReport(@PathVariable String modelName,
                                                   @RequestParam(required = false) LocalDateTime startTime,
                                                   @RequestParam(required = false) LocalDateTime endTime) {
        try {
            if (startTime == null) {
                startTime = LocalDateTime.now().minusMonths(6);
            }
            if (endTime == null) {
                endTime = LocalDateTime.now();
            }
            
            String report = retrainingPipeline.generateRetrainingReport(modelName, startTime, endTime);
            
            return ResponseEntity.ok(Map.of(
                "modelName", modelName,
                "startTime", startTime,
                "endTime", endTime,
                "report", report,
                "generatedAt", LocalDateTime.now()
            ));
            
        } catch (Exception e) {
            logger.error("Error generating retraining report for model: {}", modelName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to generate report", "message", e.getMessage()));
        }
    }
    
    // Health Check Endpoint
    @GetMapping("/health")
    @Counted(value = "explainability.health", description = "Number of health checks")
    @Timed(value = "explainability.health.time", description = "Time taken for health check")
    public ResponseEntity<?> healthCheck() {
        try {
            boolean isHealthy = true; // Simplified health check
            
            Map<String, Object> health = Map.of(
                "status", isHealthy ? "UP" : "DOWN",
                "timestamp", LocalDateTime.now(),
                "service", "explainability-monitoring",
                "components", Map.of(
                    "featureImportanceService", "UP",
                    "dashboardService", "UP",
                    "driftDetectionService", "UP",
                    "retrainingPipeline", "UP"
                )
            );
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            logger.error("Error in health check: {}", e.getMessage(), e);
            
            Map<String, Object> health = Map.of(
                "status", "DOWN",
                "timestamp", LocalDateTime.now(),
                "service", "explainability-monitoring",
                "error", e.getMessage()
            );
            
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(health);
        }
    }
    
    // Request DTOs
    public static class RetrainingScheduleRequest {
        private int dayOfMonth = 1;
        private int minDataPoints = 1000;
        private double performanceThreshold = 0.8;
        private boolean autoDeploy = true;
        private boolean backupOldModels = true;
        
        // Getters and setters
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
    }
}
