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
@RequestMapping("/api/ai-prediction")
@CrossOrigin(origins = "*")
public class AiPredictionController {
    
    private static final Logger logger = LoggerFactory.getLogger(AiPredictionController.class);
    
    @Autowired
    private WekaService wekaService;
    
    @Autowired
    private NewsScoringService newsScoringService;
    
    @Autowired
    private FeatureEngineeringService featureEngineeringService;
    
    @Autowired
    private ModelPerformanceTrackingService performanceTrackingService;
    
    // Weka Model Management Endpoints
    @PostMapping("/weka/models/train")
    @Counted(value = "ai.prediction.weka.train", description = "Number of Weka model trainings")
    @Timed(value = "ai.prediction.weka.train.time", description = "Time taken to train Weka model")
    public ResponseEntity<?> trainWekaModel(@Valid @RequestBody WekaTrainingRequest request) {
        try {
            logger.info("Training Weka model: {} with {} samples", 
                       request.getModelName(), request.getTrainingData().size());
            
            WekaService.ModelTrainingResult result = wekaService.trainJ48Model(
                request.getModelName(),
                request.getTrainingData(),
                request.getTargetAttribute(),
                request.getTrainingParams()
            );
            
            if (result.isSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
            
        } catch (Exception e) {
            logger.error("Error training Weka model: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to train model", "message", e.getMessage()));
        }
    }
    
    @PostMapping("/weka/models/predict")
    @Counted(value = "ai.prediction.weka.predict", description = "Number of Weka predictions")
    @Timed(value = "ai.prediction.weka.predict.time", description = "Time taken to make Weka prediction")
    public ResponseEntity<?> makeWekaPrediction(@Valid @RequestBody WekaPredictionRequest request) {
        try {
            logger.info("Making Weka prediction for model: {}", request.getModelName());
            
            WekaService.PredictionResult result = wekaService.makePrediction(
                request.getModelName(),
                request.getInputFeatures()
            );
            
            if (result.hasError()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
            
            // Track prediction performance
            performanceTrackingService.trackPrediction(
                request.getModelName(),
                result.getPredictedClass(),
                request.getActualClass(),
                result.getConfidence(),
                request.getInputFeatures()
            );
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error making Weka prediction: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to make prediction", "message", e.getMessage()));
        }
    }
    
    @PostMapping("/weka/models/predict/batch")
    @Counted(value = "ai.prediction.weka.predict.batch", description = "Number of Weka batch predictions")
    @Timed(value = "ai.prediction.weka.predict.batch.time", description = "Time taken to make Weka batch predictions")
    public ResponseEntity<?> makeWekaBatchPrediction(@Valid @RequestBody WekaBatchPredictionRequest request) {
        try {
            logger.info("Making Weka batch prediction for model: {} with {} samples", 
                       request.getModelName(), request.getInputFeaturesList().size());
            
            List<WekaService.PredictionResult> results = wekaService.makeBatchPrediction(
                request.getModelName(),
                request.getInputFeaturesList()
            );
            
            // Track batch predictions
            List<ModelPerformanceTrackingService.BatchPredictionRecord> batchRecords = results.stream()
                .map(result -> new ModelPerformanceTrackingService.BatchPredictionRecord(
                    result.getPredictedClass(),
                    request.getActualClassForPrediction(result.getPredictedClass()),
                    result.getConfidence(),
                    result.getInputFeatures(),
                    result.getTimestamp()
                ))
                .toList();
            
            performanceTrackingService.trackBatchPredictions(request.getModelName(), batchRecords);
            
            return ResponseEntity.ok(results);
            
        } catch (Exception e) {
            logger.error("Error making Weka batch prediction: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to make batch prediction", "message", e.getMessage()));
        }
    }
    
    @GetMapping("/weka/models")
    @Counted(value = "ai.prediction.weka.models.list", description = "Number of Weka model list requests")
    @Timed(value = "ai.prediction.weka.models.list.time", description = "Time taken to list Weka models")
    public ResponseEntity<?> listWekaModels() {
        try {
            List<WekaService.ModelInfo> models = wekaService.listAllModels();
            return ResponseEntity.ok(models);
            
        } catch (Exception e) {
            logger.error("Error listing Weka models: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to list models", "message", e.getMessage()));
        }
    }
    
    @GetMapping("/weka/models/{modelName}")
    @Counted(value = "ai.prediction.weka.models.get", description = "Number of Weka model get requests")
    @Timed(value = "ai.prediction.weka.models.get.time", description = "Time taken to get Weka model")
    public ResponseEntity<?> getWekaModel(@PathVariable String modelName) {
        try {
            WekaService.ModelInfo modelInfo = wekaService.getModelInfo(modelName);
            if (modelInfo == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Model not found", "modelName", modelName));
            }
            
            return ResponseEntity.ok(modelInfo);
            
        } catch (Exception e) {
            logger.error("Error getting Weka model: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get model", "message", e.getMessage()));
        }
    }
    
    @PostMapping("/weka/models/{modelName}/save")
    @Counted(value = "ai.prediction.weka.models.save", description = "Number of Weka model saves")
    @Timed(value = "ai.prediction.weka.models.save.time", description = "Time taken to save Weka model")
    public ResponseEntity<?> saveWekaModel(@PathVariable String modelName, @RequestParam String filePath) {
        try {
            boolean success = wekaService.saveModel(modelName, filePath);
            if (success) {
                return ResponseEntity.ok(Map.of("message", "Model saved successfully", "filePath", filePath));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to save model"));
            }
            
        } catch (Exception e) {
            logger.error("Error saving Weka model: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to save model", "message", e.getMessage()));
        }
    }
    
    @DeleteMapping("/weka/models/{modelName}")
    @Counted(value = "ai.prediction.weka.models.delete", description = "Number of Weka model deletions")
    @Timed(value = "ai.prediction.weka.models.delete.time", description = "Time taken to delete Weka model")
    public ResponseEntity<?> deleteWekaModel(@PathVariable String modelName) {
        try {
            boolean success = wekaService.deleteModel(modelName);
            if (success) {
                return ResponseEntity.ok(Map.of("message", "Model deleted successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Model not found", "modelName", modelName));
            }
            
        } catch (Exception e) {
            logger.error("Error deleting Weka model: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to delete model", "message", e.getMessage()));
        }
    }
    
    // NEWS Scoring Endpoints
    @PostMapping("/news/score")
    @Counted(value = "ai.prediction.news.score", description = "Number of NEWS score calculations")
    @Timed(value = "ai.prediction.news.score.time", description = "Time taken to calculate NEWS score")
    public ResponseEntity<?> calculateNewsScore(@Valid @RequestBody NewsScoringRequest request) {
        try {
            logger.info("Calculating NEWS score for patient: {}", request.getPatientId());
            
            NewsScoringService.NewsScore newsScore = newsScoringService.calculateNewsScore(request.getVitals());
            
            return ResponseEntity.ok(newsScore);
            
        } catch (Exception e) {
            logger.error("Error calculating NEWS score: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to calculate NEWS score", "message", e.getMessage()));
        }
    }
    
    @GetMapping("/news/parameters")
    @Counted(value = "ai.prediction.news.parameters", description = "Number of NEWS parameters requests")
    @Timed(value = "ai.prediction.news.parameters.time", description = "Time taken to get NEWS parameters")
    public ResponseEntity<?> getNewsScoringParameters() {
        try {
            Map<String, Map<String, Integer>> parameters = newsScoringService.getNewsScoringParameters();
            return ResponseEntity.ok(parameters);
            
        } catch (Exception e) {
            logger.error("Error getting NEWS scoring parameters: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get NEWS parameters", "message", e.getMessage()));
        }
    }
    
    // Feature Engineering Endpoints
    @PostMapping("/features/engineer")
    @Counted(value = "ai.prediction.features.engineer", description = "Number of feature engineering requests")
    @Timed(value = "ai.prediction.features.engineer.time", description = "Time taken to engineer features")
    public ResponseEntity<?> engineerFeatures(@Valid @RequestBody FeatureEngineeringRequest request) {
        try {
            logger.info("Engineering features for patient: {} with {} readings", 
                       request.getPatientId(), request.getVitalReadings().size());
            
            FeatureEngineeringService.EngineeredFeatures features = featureEngineeringService.engineerFeatures(
                request.getVitalReadings(),
                request.getPatientId()
            );
            
            return ResponseEntity.ok(features);
            
        } catch (Exception e) {
            logger.error("Error engineering features: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to engineer features", "message", e.getMessage()));
        }
    }
    
    // Model Performance Tracking Endpoints
    @GetMapping("/performance/models/{modelName}")
    @Counted(value = "ai.prediction.performance.model", description = "Number of model performance requests")
    @Timed(value = "ai.prediction.performance.model.time", description = "Time taken to get model performance")
    public ResponseEntity<?> getModelPerformance(@PathVariable String modelName) {
        try {
            ModelPerformanceTrackingService.ModelPerformanceMetrics metrics = 
                performanceTrackingService.getModelMetrics(modelName);
            
            if (metrics == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Model not found", "modelName", modelName));
            }
            
            return ResponseEntity.ok(metrics);
            
        } catch (Exception e) {
            logger.error("Error getting model performance: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get model performance", "message", e.getMessage()));
        }
    }
    
    @GetMapping("/performance/models")
    @Counted(value = "ai.prediction.performance.models", description = "Number of all model performance requests")
    @Timed(value = "ai.prediction.performance.models.time", description = "Time taken to get all model performance")
    public ResponseEntity<?> getAllModelPerformance() {
        try {
            Map<String, ModelPerformanceTrackingService.ModelPerformanceMetrics> allMetrics = 
                performanceTrackingService.getAllModelMetrics();
            
            return ResponseEntity.ok(allMetrics);
            
        } catch (Exception e) {
            logger.error("Error getting all model performance: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get all model performance", "message", e.getMessage()));
        }
    }
    
    @GetMapping("/performance/predictions/{modelName}")
    @Counted(value = "ai.prediction.performance.predictions", description = "Number of prediction history requests")
    @Timed(value = "ai.prediction.performance.predictions.time", description = "Time taken to get prediction history")
    public ResponseEntity<?> getPredictionHistory(@PathVariable String modelName, 
                                            @RequestParam(defaultValue = "100") int limit) {
        try {
            List<ModelPerformanceTrackingService.PredictionRecord> history = 
                performanceTrackingService.getPredictionHistory(modelName, limit);
            
            return ResponseEntity.ok(history);
            
        } catch (Exception e) {
            logger.error("Error getting prediction history: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get prediction history", "message", e.getMessage()));
        }
    }
    
    @GetMapping("/performance/summary/{modelName}")
    @Counted(value = "ai.prediction.performance.summary", description = "Number of performance summary requests")
    @Timed(value = "ai.prediction.performance.summary.time", description = "Time taken to get performance summary")
    public ResponseEntity<?> getPerformanceSummary(@PathVariable String modelName,
                                             @RequestParam(required = false) LocalDateTime startTime,
                                             @RequestParam(required = false) LocalDateTime endTime) {
        try {
            if (startTime == null) {
                startTime = LocalDateTime.now().minusHours(24);
            }
            if (endTime == null) {
                endTime = LocalDateTime.now();
            }
            
            ModelPerformanceTrackingService.ModelPerformanceSummary summary = 
                performanceTrackingService.calculatePerformanceSummary(modelName, startTime, endTime);
            
            if (summary == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No performance data available", "modelName", modelName));
            }
            
            return ResponseEntity.ok(summary);
            
        } catch (Exception e) {
            logger.error("Error getting performance summary: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get performance summary", "message", e.getMessage()));
        }
    }
    
    @GetMapping("/performance/drift/{modelName}")
    @Counted(value = "ai.prediction.performance.drift", description = "Number of drift detection requests")
    @Timed(value = "ai.prediction.performance.drift.time", description = "Time taken to detect drift")
    public ResponseEntity<?> detectModelDrift(@PathVariable String modelName) {
        try {
            ModelPerformanceTrackingService.DriftDetection drift = 
                performanceTrackingService.detectModelDrift(modelName);
            
            if (drift == null) {
                return ResponseEntity.ok(Map.of("message", "No drift detected", "modelName", modelName));
            }
            
            return ResponseEntity.ok(drift);
            
        } catch (Exception e) {
            logger.error("Error detecting model drift: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to detect drift", "message", e.getMessage()));
        }
    }
    
    @PostMapping("/performance/export/{modelName}")
    @Counted(value = "ai.prediction.performance.export", description = "Number of performance export requests")
    @Timed(value = "ai.prediction.performance.export.time", description = "Time taken to export performance")
    public ResponseEntity<?> exportPerformanceData(@PathVariable String modelName,
                                              @RequestParam(required = false) LocalDateTime startTime,
                                              @RequestParam(required = false) LocalDateTime endTime) {
        try {
            if (startTime == null) {
                startTime = LocalDateTime.now().minusHours(24);
            }
            if (endTime == null) {
                endTime = LocalDateTime.now();
            }
            
            String exportData = performanceTrackingService.exportPerformanceData(modelName, startTime, endTime);
            
            return ResponseEntity.ok(Map.of(
                "modelName", modelName,
                "startTime", startTime,
                "endTime", endTime,
                "data", exportData
            ));
            
        } catch (Exception e) {
            logger.error("Error exporting performance data: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to export performance data", "message", e.getMessage()));
        }
    }
    
    @DeleteMapping("/performance/models/{modelName}/reset")
    @Counted(value = "ai.prediction.performance.reset", description = "Number of model reset requests")
    @Timed(value = "ai.prediction.performance.reset.time", description = "Time taken to reset model")
    public ResponseEntity<?> resetModelMetrics(@PathVariable String modelName) {
        try {
            performanceTrackingService.resetModelMetrics(modelName);
            return ResponseEntity.ok(Map.of("message", "Model metrics reset successfully"));
            
        } catch (Exception e) {
            logger.error("Error resetting model metrics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to reset model metrics", "message", e.getMessage()));
        }
    }
    
    // Global Statistics Endpoints
    @GetMapping("/performance/global")
    @Counted(value = "ai.prediction.performance.global", description = "Number of global statistics requests")
    @Timed(value = "ai.prediction.performance.global.time", description = "Time taken to get global statistics")
    public ResponseEntity<?> getGlobalStatistics() {
        try {
            ModelPerformanceTrackingService.GlobalStatistics stats = 
                performanceTrackingService.getGlobalStatistics();
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.error("Error getting global statistics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get global statistics", "message", e.getMessage()));
        }
    }
    
    // Health Check Endpoint
    @GetMapping("/health")
    @Counted(value = "ai.prediction.health", description = "Number of health checks")
    @Timed(value = "ai.prediction.health.time", description = "Time taken for health check")
    public ResponseEntity<?> healthCheck() {
        try {
            boolean isHealthy = true; // Simplified health check
            
            Map<String, Object> health = Map.of(
                "status", isHealthy ? "UP" : "DOWN",
                "timestamp", LocalDateTime.now(),
                "service", "ai-prediction",
                "components", Map.of(
                    "wekaService", "UP",
                    "newsScoringService", "UP",
                    "featureEngineeringService", "UP",
                    "performanceTrackingService", "UP"
                )
            );
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            logger.error("Error in health check: {}", e.getMessage(), e);
            
            Map<String, Object> health = Map.of(
                "status", "DOWN",
                "timestamp", LocalDateTime.now(),
                "service", "ai-prediction",
                "error", e.getMessage()
            );
            
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(health);
        }
    }
    
    // Request/Response DTOs
    public static class WekaTrainingRequest {
        private String modelName;
        private List<Map<String, Object>> trainingData;
        private String targetAttribute;
        private Map<String, Object> trainingParams;
        
        // Getters and setters
        public String getModelName() { return modelName; }
        public void setModelName(String modelName) { this.modelName = modelName; }
        
        public List<Map<String, Object>> getTrainingData() { return trainingData; }
        public void setTrainingData(List<Map<String, Object>> trainingData) { this.trainingData = trainingData; }
        
        public String getTargetAttribute() { return targetAttribute; }
        public void setTargetAttribute(String targetAttribute) { this.targetAttribute = targetAttribute; }
        
        public Map<String, Object> getTrainingParams() { return trainingParams; }
        public void setTrainingParams(Map<String, Object> trainingParams) { this.trainingParams = trainingParams; }
    }
    
    public static class WekaPredictionRequest {
        private String modelName;
        private Map<String, Object> inputFeatures;
        private String actualClass;
        
        // Getters and setters
        public String getModelName() { return modelName; }
        public void setModelName(String modelName) { this.modelName = modelName; }
        
        public Map<String, Object> getInputFeatures() { return inputFeatures; }
        public void setInputFeatures(Map<String, Object> inputFeatures) { this.inputFeatures = inputFeatures; }
        
        public String getActualClass() { return actualClass; }
        public void setActualClass(String actualClass) { this.actualClass = actualClass; }
    }
    
    public static class WekaBatchPredictionRequest {
        private String modelName;
        private List<Map<String, Object>> inputFeaturesList;
        private Map<String, String> actualClasses;
        
        // Getters and setters
        public String getModelName() { return modelName; }
        public void setModelName(String modelName) { this.modelName = modelName; }
        
        public List<Map<String, Object>> getInputFeaturesList() { return inputFeaturesList; }
        public void setInputFeaturesList(List<Map<String, Object>> inputFeaturesList) { this.inputFeaturesList = inputFeaturesList; }
        
        public Map<String, String> getActualClasses() { return actualClasses; }
        public void setActualClasses(Map<String, String> actualClasses) { this.actualClasses = actualClasses; }
        
        public String getActualClassForPrediction(String predictedClass) {
            return actualClasses != null ? actualClasses.get(predictedClass) : null;
        }
    }
    
    public static class NewsScoringRequest {
        private String patientId;
        private NewsScoringService.PatientVitals vitals;
        
        // Getters and setters
        public String getPatientId() { return patientId; }
        public void setPatientId(String patientId) { this.patientId = patientId; }
        
        public NewsScoringService.PatientVitals getVitals() { return vitals; }
        public void setVitals(NewsScoringService.PatientVitals vitals) { this.vitals = vitals; }
    }
    
    public static class FeatureEngineeringRequest {
        private String patientId;
        private List<FeatureEngineeringService.VitalReading> vitalReadings;
        
        // Getters and setters
        public String getPatientId() { return patientId; }
        public void setPatientId(String patientId) { this.patientId = patientId; }
        
        public List<FeatureEngineeringService.VitalReading> getVitalReadings() { return vitalReadings; }
        public void setVitalReadings(List<FeatureEngineeringService.VitalReading> vitalReadings) { this.vitalReadings = vitalReadings; }
    }
}
