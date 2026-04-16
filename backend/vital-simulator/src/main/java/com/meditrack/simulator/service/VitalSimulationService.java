package com.meditrack.simulator.service;

import com.meditrack.simulator.config.SimulationConfig;
import com.meditrack.simulator.controller.SimulatorController;
import com.meditrack.simulator.kafka.VitalKafkaProducer;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class VitalSimulationService {
    
    private static final Logger logger = LoggerFactory.getLogger(VitalSimulationService.class);
    
    @Autowired
    private VitalDataGenerator vitalDataGenerator;
    
    @Autowired
    private VitalKafkaProducer kafkaProducer;
    
    @Autowired
    private SimulationConfig simulationConfig;
    
    // Simulation management
    private final Map<String, SimulationInstance> activeSimulations = new ConcurrentHashMap<>();
    private final AtomicLong totalVitalsGenerated = new AtomicLong(0);
    private final AtomicLong totalAnomaliesGenerated = new AtomicLong(0);
    private final AtomicLong totalSimulationsStarted = new AtomicLong(0);
    
    // Start simulation
    @Counted(value = "simulation.started", description = "Number of simulations started")
    @Timed(value = "simulation.start.time", description = "Time taken to start simulation")
    public SimulatorController.SimulationResult startSimulation(SimulatorController.SimulationRequest request) {
        try {
            String simulationId = "sim_" + System.currentTimeMillis();
            
            SimulationInstance instance = new SimulationInstance(
                simulationId,
                request.getPatientCount(),
                request.getIntervalSeconds(),
                request.getVitalTypes(),
                request.isGenerateAnomalies(),
                request.getAnomalyProbability(),
                LocalDateTime.now()
            );
            
            activeSimulations.put(simulationId, instance);
            totalSimulationsStarted.incrementAndGet();
            
            // Start the simulation asynchronously
            startSimulationAsync(instance);
            
            logger.info("Started simulation: {} with {} patients", simulationId, request.getPatientCount());
            
            return new SimulatorController.SimulationResult(
                simulationId, 
                "STARTED", 
                "Simulation started successfully", 
                LocalDateTime.now()
            );
            
        } catch (Exception e) {
            logger.error("Error starting simulation: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to start simulation", e);
        }
    }
    
    // Stop simulation
    @Counted(value = "simulation.stopped", description = "Number of simulations stopped")
    @Timed(value = "simulation.stop.time", description = "Time taken to stop simulation")
    public SimulatorController.SimulationResult stopSimulation(String simulationId) {
        try {
            SimulationInstance instance = activeSimulations.get(simulationId);
            if (instance == null) {
                throw new IllegalArgumentException("Simulation not found: " + simulationId);
            }
            
            instance.setStatus("STOPPED");
            instance.setEndTime(LocalDateTime.now());
            
            activeSimulations.remove(simulationId);
            
            logger.info("Stopped simulation: {}", simulationId);
            
            return new SimulatorController.SimulationResult(
                simulationId, 
                "STOPPED", 
                "Simulation stopped successfully", 
                LocalDateTime.now()
            );
            
        } catch (Exception e) {
            logger.error("Error stopping simulation: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to stop simulation", e);
        }
    }
    
    // Pause simulation
    public SimulatorController.SimulationResult pauseSimulation(String simulationId) {
        try {
            SimulationInstance instance = activeSimulations.get(simulationId);
            if (instance == null) {
                throw new IllegalArgumentException("Simulation not found: " + simulationId);
            }
            
            instance.setStatus("PAUSED");
            
            logger.info("Paused simulation: {}", simulationId);
            
            return new SimulatorController.SimulationResult(
                simulationId, 
                "PAUSED", 
                "Simulation paused successfully", 
                LocalDateTime.now()
            );
            
        } catch (Exception e) {
            logger.error("Error pausing simulation: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to pause simulation", e);
        }
    }
    
    // Resume simulation
    public SimulatorController.SimulationResult resumeSimulation(String simulationId) {
        try {
            SimulationInstance instance = activeSimulations.get(simulationId);
            if (instance == null) {
                throw new IllegalArgumentException("Simulation not found: " + simulationId);
            }
            
            instance.setStatus("RUNNING");
            
            logger.info("Resumed simulation: {}", simulationId);
            
            return new SimulatorController.SimulationResult(
                simulationId, 
                "RUNNING", 
                "Simulation resumed successfully", 
                LocalDateTime.now()
            );
            
        } catch (Exception e) {
            logger.error("Error resuming simulation: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to resume simulation", e);
        }
    }
    
    // Get simulation status
    public SimulatorController.SimulationStatus getSimulationStatus(String simulationId) {
        try {
            SimulationInstance instance = activeSimulations.get(simulationId);
            if (instance == null) {
                throw new IllegalArgumentException("Simulation not found: " + simulationId);
            }
            
            return convertToSimulationStatus(instance);
            
        } catch (Exception e) {
            logger.error("Error getting simulation status: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get simulation status", e);
        }
    }
    
    // Get all simulation status
    public List<SimulatorController.SimulationStatus> getAllSimulationStatus() {
        try {
            List<SimulatorController.SimulationStatus> statusList = new ArrayList<>();
            
            for (SimulationInstance instance : activeSimulations.values()) {
                statusList.add(convertToSimulationStatus(instance));
            }
            
            return statusList;
            
        } catch (Exception e) {
            logger.error("Error getting all simulation status: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get all simulation status", e);
        }
    }
    
    // Generate vital batch
    @Counted(value = "simulation.batch.generated", description = "Number of vital batches generated")
    @Timed(value = "simulation.batch.generate.time", description = "Time taken to generate vital batch")
    public List<VitalDataGenerator.VitalReading> generateVitalBatch(SimulatorController.BatchGenerationRequest request) {
        try {
            List<VitalDataGenerator.VitalReading> vitals = new ArrayList<>();
            VitalDataGenerator.VitalProfile defaultProfile = createDefaultProfile();
            
            for (int i = 0; i < request.getPatientCount(); i++) {
                String patientId = "PATIENT_" + String.format("%03d", i + 1);
                
                for (String vitalType : request.getVitalTypes()) {
                    for (int j = 0; j < request.getVitalsPerPatient(); j++) {
                        VitalDataGenerator.VitalReading vital = vitalDataGenerator.generateVitalReading(
                            patientId, vitalType, defaultProfile, LocalDateTime.now()
                        );
                        vitals.add(vital);
                    }
                }
            }
            
            logger.info("Generated vital batch: {} vitals for {} patients", vitals.size(), request.getPatientCount());
            
            return vitals;
            
        } catch (Exception e) {
            logger.error("Error generating vital batch: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate vital batch", e);
        }
    }
    
    // Get simulation statistics
    public Map<String, Object> getSimulationStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            stats.put("totalSimulationsStarted", totalSimulationsStarted.get());
            stats.put("totalVitalsGenerated", totalVitalsGenerated.get());
            stats.put("totalAnomaliesGenerated", totalAnomaliesGenerated.get());
            stats.put("activeSimulations", activeSimulations.size());
            stats.put("timestamp", LocalDateTime.now());
            
            // Per-simulation stats
            Map<String, Object> perSimulationStats = new HashMap<>();
            for (SimulationInstance instance : activeSimulations.values()) {
                perSimulationStats.put(instance.getSimulationId(), Map.of(
                    "status", instance.getStatus(),
                    "patientCount", instance.getPatientCount(),
                    "vitalsGenerated", instance.getVitalsGenerated(),
                    "anomaliesGenerated", instance.getAnomaliesGenerated(),
                    "startTime", instance.getStartTime(),
                    "lastUpdate", instance.getLastUpdate()
                ));
            }
            stats.put("perSimulationStats", perSimulationStats);
            
            return stats;
            
        } catch (Exception e) {
            logger.error("Error getting simulation stats: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get simulation stats", e);
        }
    }
    
    // Reset statistics
    public void resetStats() {
        totalVitalsGenerated.set(0);
        totalAnomaliesGenerated.set(0);
        totalSimulationsStarted.set(0);
        
        // Reset per-simulation stats
        for (SimulationInstance instance : activeSimulations.values()) {
            instance.setVitalsGenerated(0);
            instance.setAnomaliesGenerated(0);
        }
        
        logger.info("Reset simulation statistics");
    }
    
    // Health check
    public boolean isHealthy() {
        try {
            // Check if we can start/stop simulations
            return true; // Simple health check
        } catch (Exception e) {
            logger.error("Simulation service health check failed: {}", e.getMessage());
            return false;
        }
    }
    
    // Async simulation execution
    @Async
    public void startSimulationAsync(SimulationInstance instance) {
        try {
            logger.info("Starting async simulation: {}", instance.getSimulationId());
            
            while ("RUNNING".equals(instance.getStatus()) && !Thread.currentThread().isInterrupted()) {
                try {
                    // Generate vitals for all patients
                    generateVitalsForSimulation(instance);
                    
                    // Wait for next interval
                    Thread.sleep(instance.getIntervalSeconds() * 1000);
                    
                    instance.setLastUpdate(LocalDateTime.now());
                    
                } catch (InterruptedException e) {
                    logger.info("Simulation interrupted: {}", instance.getSimulationId());
                    break;
                } catch (Exception e) {
                    logger.error("Error in simulation loop: {}", e.getMessage(), e);
                    // Continue running despite errors
                }
            }
            
            logger.info("Simulation ended: {}", instance.getSimulationId());
            
        } catch (Exception e) {
            logger.error("Error in async simulation: {}", e.getMessage(), e);
        }
    }
    
    // Generate vitals for simulation
    private void generateVitalsForSimulation(SimulationInstance instance) {
        try {
            VitalDataGenerator.VitalProfile defaultProfile = createDefaultProfile();
            
            for (int i = 0; i < instance.getPatientCount(); i++) {
                String patientId = "PATIENT_" + String.format("%03d", i + 1);
                
                for (String vitalType : instance.getVitalTypes()) {
                    // Check if we should generate an anomaly
                    boolean generateAnomaly = instance.isGenerateAnomalies() && 
                        Math.random() < instance.getAnomalyProbability();
                    
                    VitalDataGenerator.VitalReading vital;
                    
                    if (generateAnomaly) {
                        // Generate abnormal reading
                        VitalDataGenerator.AbnormalType[] anomalyTypes = VitalDataGenerator.AbnormalType.values();
                        VitalDataGenerator.AbnormalType anomalyType = anomalyTypes[
                            (int) (Math.random() * anomalyTypes.length)
                        ];
                        
                        vital = vitalDataGenerator.generateAbnormalVitalReading(
                            patientId, vitalType, anomalyType, LocalDateTime.now()
                        );
                        
                        instance.incrementAnomaliesGenerated();
                        totalAnomaliesGenerated.incrementAndGet();
                        
                        // Send anomaly alert
                        kafkaProducer.sendAnomalyAlert(
                            patientId, vitalType, anomalyType.toString(), 
                            "Simulated anomaly: " + anomalyType
                        );
                        
                    } else {
                        // Generate normal reading
                        vital = vitalDataGenerator.generateVitalReading(
                            patientId, vitalType, defaultProfile, LocalDateTime.now()
                        );
                    }
                    
                    // Send to Kafka
                    kafkaProducer.sendVitalReading(vital);
                    
                    instance.incrementVitalsGenerated();
                    totalVitalsGenerated.incrementAndGet();
                }
            }
            
        } catch (Exception e) {
            logger.error("Error generating vitals for simulation: {}", e.getMessage(), e);
        }
    }
    
    // Scheduled cleanup of old simulations
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void cleanupOldSimulations() {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusHours(1);
            
            List<String> toRemove = new ArrayList<>();
            for (Map.Entry<String, SimulationInstance> entry : activeSimulations.entrySet()) {
                SimulationInstance instance = entry.getValue();
                if (instance.getLastUpdate().isBefore(cutoffTime) && 
                    ("STOPPED".equals(instance.getStatus()) || "PAUSED".equals(instance.getStatus()))) {
                    toRemove.add(entry.getKey());
                }
            }
            
            for (String simulationId : toRemove) {
                activeSimulations.remove(simulationId);
                logger.info("Cleaned up old simulation: {}", simulationId);
            }
            
            if (!toRemove.isEmpty()) {
                logger.info("Cleaned up {} old simulations", toRemove.size());
            }
            
        } catch (Exception e) {
            logger.error("Error cleaning up old simulations: {}", e.getMessage(), e);
        }
    }
    
    // Utility methods
    private SimulatorController.SimulationStatus convertToSimulationStatus(SimulationInstance instance) {
        SimulatorController.SimulationStatus status = new SimulatorController.SimulationStatus();
        status.setSimulationId(instance.getSimulationId());
        status.setStatus(instance.getStatus());
        status.setPatientCount(instance.getPatientCount());
        status.setVitalsGenerated(instance.getVitalsGenerated());
        status.setAnomaliesGenerated(instance.getAnomaliesGenerated());
        status.setStartTime(instance.getStartTime());
        status.setLastUpdate(instance.getLastUpdate());
        
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("intervalSeconds", instance.getIntervalSeconds());
        metrics.put("vitalTypes", instance.getVitalTypes());
        metrics.put("generateAnomalies", instance.isGenerateAnomalies());
        metrics.put("anomalyProbability", instance.getAnomalyProbability());
        status.setMetrics(metrics);
        
        return status;
    }
    
    private VitalDataGenerator.VitalProfile createDefaultProfile() {
        return new VitalDataGenerator.VitalProfile(
            30, 70, 120, 80, 36.5, 98, 16, 
            VitalDataGenerator.ActivityLevel.RESTING
        );
    }
    
    // Simulation instance class
    private static class SimulationInstance {
        private String simulationId;
        private int patientCount;
        private int intervalSeconds;
        private List<String> vitalTypes;
        private boolean generateAnomalies;
        private double anomalyProbability;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private LocalDateTime lastUpdate;
        private String status = "RUNNING";
        private long vitalsGenerated = 0;
        private long anomaliesGenerated = 0;
        
        public SimulationInstance(String simulationId, int patientCount, int intervalSeconds,
                                 List<String> vitalTypes, boolean generateAnomalies, 
                                 double anomalyProbability, LocalDateTime startTime) {
            this.simulationId = simulationId;
            this.patientCount = patientCount;
            this.intervalSeconds = intervalSeconds;
            this.vitalTypes = vitalTypes;
            this.generateAnomalies = generateAnomalies;
            this.anomalyProbability = anomalyProbability;
            this.startTime = startTime;
            this.lastUpdate = startTime;
        }
        
        // Getters and setters
        public String getSimulationId() { return simulationId; }
        public void setSimulationId(String simulationId) { this.simulationId = simulationId; }
        
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
        
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        
        public LocalDateTime getLastUpdate() { return lastUpdate; }
        public void setLastUpdate(LocalDateTime lastUpdate) { this.lastUpdate = lastUpdate; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public long getVitalsGenerated() { return vitalsGenerated; }
        public void setVitalsGenerated(long vitalsGenerated) { this.vitalsGenerated = vitalsGenerated; }
        
        public long getAnomaliesGenerated() { return anomaliesGenerated; }
        public void setAnomaliesGenerated(long anomaliesGenerated) { this.anomaliesGenerated = anomaliesGenerated; }
        
        public void incrementVitalsGenerated() { vitalsGenerated++; }
        public void incrementAnomaliesGenerated() { anomaliesGenerated++; }
    }
}
