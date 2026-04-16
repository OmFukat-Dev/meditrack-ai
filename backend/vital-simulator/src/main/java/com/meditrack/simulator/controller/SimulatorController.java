package com.meditrack.simulator.controller;

import com.meditrack.simulator.config.SimulationConfig;
import com.meditrack.simulator.kafka.VitalKafkaProducer;
import com.meditrack.simulator.service.VitalDataGenerator;
import com.meditrack.simulator.service.VitalSimulationService;
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
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/simulator")
@CrossOrigin(origins = "*")
public class SimulatorController {
    
    private static final Logger logger = LoggerFactory.getLogger(SimulatorController.class);
    
    @Autowired
    private VitalSimulationService simulationService;
    
    @Autowired
    private VitalDataGenerator vitalDataGenerator;
    
    @Autowired
    private VitalKafkaProducer kafkaProducer;
    
    @Autowired
    private SimulationConfig simulationConfig;
    
    // Simulation control endpoints
    @PostMapping("/start")
    @Counted(value = "simulator.start", description = "Number of simulation starts")
    @Timed(value = "simulator.start.time", description = "Time taken to start simulation")
    public ResponseEntity<?> startSimulation(@Valid @RequestBody SimulationRequest request) {
        try {
            logger.info("Starting simulation: patientCount={}, interval={}s", 
                       request.getPatientCount(), request.getIntervalSeconds());
            
            SimulationResult result = simulationService.startSimulation(request);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error starting simulation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to start simulation", "message", e.getMessage()));
        }
    }
    
    @PostMapping("/stop")
    @Counted(value = "simulator.stop", description = "Number of simulation stops")
    @Timed(value = "simulator.stop.time", description = "Time taken to stop simulation")
    public ResponseEntity<?> stopSimulation(@RequestParam(required = false) String simulationId) {
        try {
            logger.info("Stopping simulation: {}", simulationId);
            
            SimulationResult result = simulationService.stopSimulation(simulationId);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error stopping simulation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to stop simulation", "message", e.getMessage()));
        }
    }
    
    @PostMapping("/pause")
    @Counted(value = "simulator.pause", description = "Number of simulation pauses")
    @Timed(value = "simulator.pause.time", description = "Time taken to pause simulation")
    public ResponseEntity<?> pauseSimulation(@RequestParam(required = false) String simulationId) {
        try {
            logger.info("Pausing simulation: {}", simulationId);
            
            SimulationResult result = simulationService.pauseSimulation(simulationId);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error pausing simulation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to pause simulation", "message", e.getMessage()));
        }
    }
    
    @PostMapping("/resume")
    @Counted(value = "simulator.resume", description = "Number of simulation resumes")
    @Timed(value = "simulator.resume.time", description = "Time taken to resume simulation")
    public ResponseEntity<?> resumeSimulation(@RequestParam(required = false) String simulationId) {
        try {
            logger.info("Resuming simulation: {}", simulationId);
            
            SimulationResult result = simulationService.resumeSimulation(simulationId);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error resuming simulation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to resume simulation", "message", e.getMessage()));
        }
    }
    
    // Simulation status endpoints
    @GetMapping("/status")
    @Counted(value = "simulator.status", description = "Number of status requests")
    @Timed(value = "simulator.status.time", description = "Time taken to get status")
    public ResponseEntity<?> getSimulationStatus(@RequestParam(required = false) String simulationId) {
        try {
            SimulationStatus status = simulationService.getSimulationStatus(simulationId);
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            logger.error("Error getting simulation status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get status", "message", e.getMessage()));
        }
    }
    
    @GetMapping("/status/all")
    @Counted(value = "simulator.status.all", description = "Number of all status requests")
    @Timed(value = "simulator.status.all.time", description = "Time taken to get all status")
    public ResponseEntity<?> getAllSimulationStatus() {
        try {
            List<SimulationStatus> allStatus = simulationService.getAllSimulationStatus();
            
            return ResponseEntity.ok(allStatus);
            
        } catch (Exception e) {
            logger.error("Error getting all simulation status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get all status", "message", e.getMessage()));
        }
    }
    
    // Configuration endpoints
    @GetMapping("/config")
    @Counted(value = "simulator.config", description = "Number of config requests")
    @Timed(value = "simulator.config.time", description = "Time taken to get config")
    public ResponseEntity<?> getSimulationConfig() {
        try {
            return ResponseEntity.ok(simulationConfig);
            
        } catch (Exception e) {
            logger.error("Error getting simulation config: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get config", "message", e.getMessage()));
        }
    }
    
    @PostMapping("/config")
    @Counted(value = "simulator.config.update", description = "Number of config updates")
    @Timed(value = "simulator.config.update.time", description = "Time taken to update config")
    public ResponseEntity<?> updateSimulationConfig(@Valid @RequestBody SimulationConfig config) {
        try {
            logger.info("Updating simulation configuration");
            
            // Update configuration (in a real implementation, this would update the actual config)
            simulationConfig.setEnabled(config.isEnabled());
            simulationConfig.setPatientCount(config.getPatientCount());
            simulationConfig.setSimulationIntervalSeconds(config.getSimulationIntervalSeconds());
            
            return ResponseEntity.ok(Map.of("message", "Configuration updated successfully"));
            
        } catch (Exception e) {
            logger.error("Error updating simulation config: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to update config", "message", e.getMessage()));
        }
    }
    
    // Vital generation endpoints
    @PostMapping("/generate/single")
    @Counted(value = "simulator.generate.single", description = "Number of single vital generations")
    @Timed(value = "simulator.generate.single.time", description = "Time taken to generate single vital")
    public ResponseEntity<?> generateSingleVital(@Valid @RequestBody VitalGenerationRequest request) {
        try {
            logger.info("Generating single vital: patientId={}, vitalType={}", 
                       request.getPatientId(), request.getVitalType());
            
            VitalDataGenerator.VitalReading vital = vitalDataGenerator.generateVitalReading(
                request.getPatientId(), 
                request.getVitalType(), 
                request.getProfile(), 
                LocalDateTime.now()
            );
            
            // Send to Kafka if requested
            if (request.isSendToKafka()) {
                CompletableFuture<Boolean> future = kafkaProducer.sendVitalReading(vital);
                return ResponseEntity.ok(Map.of(
                    "vital", vital,
                    "sentToKafka", future.get()
                ));
            }
            
            return ResponseEntity.ok(vital);
            
        } catch (Exception e) {
            logger.error("Error generating single vital: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to generate vital", "message", e.getMessage()));
        }
    }
    
    @PostMapping("/generate/batch")
    @Counted(value = "simulator.generate.batch", description = "Number of batch vital generations")
    @Timed(value = "simulator.generate.batch.time", description = "Time taken to generate batch vitals")
    public ResponseEntity<?> generateVitalBatch(@Valid @RequestBody BatchGenerationRequest request) {
        try {
            logger.info("Generating vital batch: patientCount={}, vitalsPerPatient={}", 
                       request.getPatientCount(), request.getVitalsPerPatient());
            
            List<VitalDataGenerator.VitalReading> vitals = simulationService.generateVitalBatch(request);
            
            // Send to Kafka if requested
            if (request.isSendToKafka()) {
                CompletableFuture<Boolean> future = kafkaProducer.sendVitalBatch(vitals);
                return ResponseEntity.ok(Map.of(
                    "vitals", vitals,
                    "sentToKafka", future.get()
                ));
            }
            
            return ResponseEntity.ok(vitals);
            
        } catch (Exception e) {
            logger.error("Error generating vital batch: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to generate batch", "message", e.getMessage()));
        }
    }
    
    // Anomaly generation endpoints
    @PostMapping("/generate/anomaly")
    @Counted(value = "simulator.generate.anomaly", description = "Number of anomaly generations")
    @Timed(value = "simulator.generate.anomaly.time", description = "Time taken to generate anomaly")
    public ResponseEntity<?> generateAnomaly(@Valid @RequestBody AnomalyRequest request) {
        try {
            logger.info("Generating anomaly: patientId={}, vitalType={}, anomalyType={}", 
                       request.getPatientId(), request.getVitalType(), request.getAnomalyType());
            
            VitalDataGenerator.VitalReading vital = vitalDataGenerator.generateAbnormalVitalReading(
                request.getPatientId(), 
                request.getVitalType(), 
                request.getAnomalyType(), 
                LocalDateTime.now()
            );
            
            // Send anomaly alert to Kafka
            CompletableFuture<Boolean> alertFuture = kafkaProducer.sendAnomalyAlert(
                request.getPatientId(), 
                request.getVitalType(), 
                request.getAnomalyType().toString(), 
                "Simulated anomaly: " + request.getAnomalyType()
            );
            
            // Send vital reading to Kafka
            CompletableFuture<Boolean> vitalFuture = kafkaProducer.sendVitalReading(vital);
            
            return ResponseEntity.ok(Map.of(
                "vital", vital,
                "alertSent", alertFuture.get(),
                "vitalSent", vitalFuture.get()
            ));
            
        } catch (Exception e) {
            logger.error("Error generating anomaly: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to generate anomaly", "message", e.getMessage()));
        }
    }
    
    // Statistics endpoints
    @GetMapping("/stats")
    @Counted(value = "simulator.stats", description = "Number of stats requests")
    @Timed(value = "simulator.stats.time", description = "Time taken to get stats")
    public ResponseEntity<?> getSimulationStats() {
        try {
            Map<String, Object> stats = Map.of(
                "simulationStats", simulationService.getSimulationStats(),
                "kafkaStats", kafkaProducer.getProductionStats(),
                "timestamp", LocalDateTime.now()
            );
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.error("Error getting simulation stats: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get stats", "message", e.getMessage()));
        }
    }
    
    @PostMapping("/stats/reset")
    @Counted(value = "simulator.stats.reset", description = "Number of stats resets")
    @Timed(value = "simulator.stats.reset.time", description = "Time taken to reset stats")
    public ResponseEntity<?> resetStats() {
        try {
            simulationService.resetStats();
            kafkaProducer.resetStats();
            
            return ResponseEntity.ok(Map.of("message", "Statistics reset successfully"));
            
        } catch (Exception e) {
            logger.error("Error resetting stats: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to reset stats", "message", e.getMessage()));
        }
    }
    
    // Health check endpoint
    @GetMapping("/health")
    @Counted(value = "simulator.health", description = "Number of health checks")
    @Timed(value = "simulator.health.time", description = "Time taken for health check")
    public ResponseEntity<?> healthCheck() {
        try {
            boolean isHealthy = simulationService.isHealthy() && kafkaProducer.isHealthy();
            
            Map<String, Object> health = Map.of(
                "status", isHealthy ? "UP" : "DOWN",
                "timestamp", LocalDateTime.now(),
                "service", "vital-simulator",
                "simulationService", simulationService.isHealthy() ? "UP" : "DOWN",
                "kafkaProducer", kafkaProducer.isHealthy() ? "UP" : "DOWN"
            );
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            logger.error("Error in health check: {}", e.getMessage(), e);
            
            Map<String, Object> health = Map.of(
                "status", "DOWN",
                "timestamp", LocalDateTime.now(),
                "service", "vital-simulator",
                "error", e.getMessage()
            );
            
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(health);
        }
    }
    
    // Inner classes for request/response objects
    public static class SimulationRequest {
        private int patientCount = 10;
        private int intervalSeconds = 30;
        private List<String> vitalTypes = List.of("HEART_RATE", "BLOOD_PRESSURE", "TEMPERATURE", "SPO2", "RESPIRATORY_RATE");
        private boolean generateAnomalies = false;
        private double anomalyProbability = 0.05;
        private boolean sendToKafka = true;
        
        // Getters and setters
        public int getPatientCount() { return patientCount; }
        public void setPatientCount(int patientCount) { this.patientCount = patientCount; }
        
        public int getIntervalSeconds() { return intervalSeconds; }
        public void setIntervalSeconds(int intervalSeconds) { this.intervalSeconds = intervalSeconds; }
        
        public List<String> getVitalTypes() { return vitalTypes; }
        public void setVitalTypes(List<String> vitalTypes) { this.vitalTypes = vitalTypes; }
        
        public boolean isGenerateAnomalies() { return generateAnomalies; }
        public void setGenerateAnomalies(boolean generateAnomalies) { this.generateAnomalies = generateAnomalies; }
        
        public double getAnomalyProbability() { return anomalyProbability; }
        public void setAnomalyProbability(double anomalyProbability) { this.anomalyProbability = anomalyProbability; }
        
        public boolean isSendToKafka() { return sendToKafka; }
        public void setSendToKafka(boolean sendToKafka) { this.sendToKafka = sendToKafka; }
    }
    
    public static class VitalGenerationRequest {
        private String patientId;
        private String vitalType;
        private VitalDataGenerator.VitalProfile profile;
        private boolean sendToKafka = false;
        
        // Getters and setters
        public String getPatientId() { return patientId; }
        public void setPatientId(String patientId) { this.patientId = patientId; }
        
        public String getVitalType() { return vitalType; }
        public void setVitalType(String vitalType) { this.vitalType = vitalType; }
        
        public VitalDataGenerator.VitalProfile getProfile() { return profile; }
        public void setProfile(VitalDataGenerator.VitalProfile profile) { this.profile = profile; }
        
        public boolean isSendToKafka() { return sendToKafka; }
        public void setSendToKafka(boolean sendToKafka) { this.sendToKafka = sendToKafka; }
    }
    
    public static class BatchGenerationRequest {
        private int patientCount = 10;
        private int vitalsPerPatient = 5;
        private List<String> vitalTypes = List.of("HEART_RATE", "BLOOD_PRESSURE", "TEMPERATURE", "SPO2", "RESPIRATORY_RATE");
        private boolean sendToKafka = false;
        
        // Getters and setters
        public int getPatientCount() { return patientCount; }
        public void setPatientCount(int patientCount) { this.patientCount = patientCount; }
        
        public int getVitalsPerPatient() { return vitalsPerPatient; }
        public void setVitalsPerPatient(int vitalsPerPatient) { this.vitalsPerPatient = vitalsPerPatient; }
        
        public List<String> getVitalTypes() { return vitalTypes; }
        public void setVitalTypes(List<String> vitalTypes) { this.vitalTypes = vitalTypes; }
        
        public boolean isSendToKafka() { return sendToKafka; }
        public void setSendToKafka(boolean sendToKafka) { this.sendToKafka = sendToKafka; }
    }
    
    public static class AnomalyRequest {
        private String patientId;
        private String vitalType;
        private VitalDataGenerator.AbnormalType anomalyType;
        
        // Getters and setters
        public String getPatientId() { return patientId; }
        public void setPatientId(String patientId) { this.patientId = patientId; }
        
        public String getVitalType() { return vitalType; }
        public void setVitalType(String vitalType) { this.vitalType = vitalType; }
        
        public VitalDataGenerator.AbnormalType getAnomalyType() { return anomalyType; }
        public void setAnomalyType(VitalDataGenerator.AbnormalType anomalyType) { this.anomalyType = anomalyType; }
    }
    
    public static class SimulationResult {
        private String simulationId;
        private String status;
        private String message;
        private LocalDateTime timestamp;
        
        public SimulationResult(String simulationId, String status, String message, LocalDateTime timestamp) {
            this.simulationId = simulationId;
            this.status = status;
            this.message = message;
            this.timestamp = timestamp;
        }
        
        // Getters
        public String getSimulationId() { return simulationId; }
        public String getStatus() { return status; }
        public String getMessage() { return message; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
    
    public static class SimulationStatus {
        private String simulationId;
        private String status;
        private int patientCount;
        private int vitalsGenerated;
        private int anomaliesGenerated;
        private LocalDateTime startTime;
        private LocalDateTime lastUpdate;
        private Map<String, Object> metrics;
        
        // Getters and setters
        public String getSimulationId() { return simulationId; }
        public void setSimulationId(String simulationId) { this.simulationId = simulationId; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public int getPatientCount() { return patientCount; }
        public void setPatientCount(int patientCount) { this.patientCount = patientCount; }
        
        public int getVitalsGenerated() { return vitalsGenerated; }
        public void setVitalsGenerated(int vitalsGenerated) { this.vitalsGenerated = vitalsGenerated; }
        
        public int getAnomaliesGenerated() { return anomaliesGenerated; }
        public void setAnomaliesGenerated(int anomaliesGenerated) { this.anomaliesGenerated = anomaliesGenerated; }
        
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        
        public LocalDateTime getLastUpdate() { return lastUpdate; }
        public void setLastUpdate(LocalDateTime lastUpdate) { this.lastUpdate = lastUpdate; }
        
        public Map<String, Object> getMetrics() { return metrics; }
        public void setMetrics(Map<String, Object> metrics) { this.metrics = metrics; }
    }
}
