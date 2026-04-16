package com.meditrack.simulator.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meditrack.simulator.config.SimulationConfig;
import com.meditrack.simulator.service.VitalDataGenerator;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class VitalKafkaProducer {
    
    private static final Logger logger = LoggerFactory.getLogger(VitalKafkaProducer.class);
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private SimulationConfig simulationConfig;
    
    // Batch processing
    private final ConcurrentLinkedQueue<VitalDataGenerator.VitalReading> vitalQueue = new ConcurrentLinkedQueue<>();
    private final AtomicLong messagesSent = new AtomicLong(0);
    private final AtomicLong messagesFailed = new AtomicLong(0);
    
    // Send single vital reading
    @Counted(value = "vital.simulator.produced", description = "Number of vital readings produced")
    @Timed(value = "vital.simulator.produce.time", description = "Time taken to produce vital reading")
    public CompletableFuture<Boolean> sendVitalReading(VitalDataGenerator.VitalReading vitalReading) {
        try {
            // Convert to VitalReadingMessage format
            VitalReadingMessage message = convertToVitalReadingMessage(vitalReading);
            String messageJson = objectMapper.writeValueAsString(message);
            
            // Send to Kafka
            ListenableFuture<SendResult<String, String>> future = kafkaTemplate.send(
                simulationConfig.getKafkaProducer().getTopic(), 
                vitalReading.getPatientId(), 
                messageJson
            );
            
            CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
            
            future.addCallback(new ListenableFutureCallback<SendResult<String, String>>() {
                @Override
                public void onSuccess(SendResult<String, String> result) {
                    messagesSent.incrementAndGet();
                    logger.debug("Successfully sent vital reading: patientId={}, vitalType={}, value={}", 
                               vitalReading.getPatientId(), vitalReading.getVitalType(), vitalReading.getDisplayValue());
                    completableFuture.complete(true);
                }
                
                @Override
                public void onFailure(Throwable ex) {
                    messagesFailed.incrementAndGet();
                    logger.error("Failed to send vital reading: patientId={}, vitalType={}, error={}", 
                               vitalReading.getPatientId(), vitalReading.getVitalType(), ex.getMessage());
                    completableFuture.complete(false);
                }
            });
            
            return completableFuture;
            
        } catch (Exception e) {
            messagesFailed.incrementAndGet();
            logger.error("Error preparing vital reading for Kafka: {}", e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }
    
    // Send batch of vital readings
    @Counted(value = "vital.simulator.batch.produced", description = "Number of vital batches produced")
    @Timed(value = "vital.simulator.batch.produce.time", description = "Time taken to produce vital batch")
    public CompletableFuture<Boolean> sendVitalBatch(List<VitalDataGenerator.VitalReading> vitalReadings) {
        try {
            if (vitalReadings.isEmpty()) {
                return CompletableFuture.completedFuture(true);
            }
            
            // Convert batch to JSON
            List<VitalReadingMessage> messages = new ArrayList<>();
            for (VitalDataGenerator.VitalReading reading : vitalReadings) {
                messages.add(convertToVitalReadingMessage(reading));
            }
            
            String batchJson = objectMapper.writeValueAsString(messages);
            
            // Send batch to Kafka
            ListenableFuture<SendResult<String, String>> future = kafkaTemplate.send(
                "vital-batches", 
                "batch_" + System.currentTimeMillis(), 
                batchJson
            );
            
            CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
            
            future.addCallback(new ListenableFutureCallback<SendResult<String, String>>() {
                @Override
                public void onSuccess(SendResult<String, String> result) {
                    messagesSent.addAndGet(vitalReadings.size());
                    logger.debug("Successfully sent vital batch: size={}", vitalReadings.size());
                    completableFuture.complete(true);
                }
                
                @Override
                public void onFailure(Throwable ex) {
                    messagesFailed.addAndGet(vitalReadings.size());
                    logger.error("Failed to send vital batch: size={}, error={}", vitalReadings.size(), ex.getMessage());
                    completableFuture.complete(false);
                }
            });
            
            return completableFuture;
            
        } catch (Exception e) {
            messagesFailed.addAndGet(vitalReadings.size());
            logger.error("Error preparing vital batch for Kafka: {}", e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }
    
    // Add vital reading to batch queue
    public void addToBatch(VitalDataGenerator.VitalReading vitalReading) {
        vitalQueue.offer(vitalReading);
        
        // Check if batch is ready to send
        if (vitalQueue.size() >= simulationConfig.getPerformance().getBatchSize()) {
            sendBatchFromQueue();
        }
    }
    
    // Send batch from queue
    private void sendBatchFromQueue() {
        List<VitalDataGenerator.VitalReading> batch = new ArrayList<>();
        
        // Collect batch items
        VitalDataGenerator.VitalReading reading;
        while ((reading = vitalQueue.poll()) != null && batch.size() < simulationConfig.getPerformance().getBatchSize()) {
            batch.add(reading);
        }
        
        if (!batch.isEmpty()) {
            sendVitalBatch(batch);
        }
    }
    
    // Flush remaining items in queue
    public CompletableFuture<Boolean> flushBatch() {
        if (!vitalQueue.isEmpty()) {
            return sendBatchFromQueue();
        }
        return CompletableFuture.completedFuture(true);
    }
    
    // Send anomaly alert
    public CompletableFuture<Boolean> sendAnomalyAlert(String patientId, String vitalType, 
                                                    String anomalyType, String description) {
        try {
            AnomalyAlert alert = new AnomalyAlert(
                patientId,
                vitalType,
                anomalyType,
                description,
                LocalDateTime.now()
            );
            
            String alertJson = objectMapper.writeValueAsString(alert);
            
            ListenableFuture<SendResult<String, String>> future = kafkaTemplate.send(
                "vital-alerts", 
                patientId + "_" + vitalType, 
                alertJson
            );
            
            CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
            
            future.addCallback(new ListenableFutureCallback<SendResult<String, String>>() {
                @Override
                public void onSuccess(SendResult<String, String> result) {
                    logger.info("Successfully sent anomaly alert: patientId={}, vitalType={}, anomalyType={}", 
                               patientId, vitalType, anomalyType);
                    completableFuture.complete(true);
                }
                
                @Override
                public void onFailure(Throwable ex) {
                    logger.error("Failed to send anomaly alert: patientId={}, vitalType={}, error={}", 
                               patientId, vitalType, ex.getMessage());
                    completableFuture.complete(false);
                }
            });
            
            return completableFuture;
            
        } catch (Exception e) {
            logger.error("Error preparing anomaly alert for Kafka: {}", e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }
    
    // Send simulation status update
    public CompletableFuture<Boolean> sendSimulationStatus(SimulationStatus status) {
        try {
            String statusJson = objectMapper.writeValueAsString(status);
            
            ListenableFuture<SendResult<String, String>> future = kafkaTemplate.send(
                "simulation-status", 
                "status_" + System.currentTimeMillis(), 
                statusJson
            );
            
            CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
            
            future.addCallback(new ListenableFutureCallback<SendResult<String, String>>() {
                @Override
                public void onSuccess(SendResult<String, String> result) {
                    logger.debug("Successfully sent simulation status: {}", status.getStatus());
                    completableFuture.complete(true);
                }
                
                @Override
                public void onFailure(Throwable ex) {
                    logger.error("Failed to send simulation status: error={}", ex.getMessage());
                    completableFuture.complete(false);
                }
            });
            
            return completableFuture;
            
        } catch (Exception e) {
            logger.error("Error preparing simulation status for Kafka: {}", e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }
    
    // Get production statistics
    public ProductionStats getProductionStats() {
        return new ProductionStats(
            messagesSent.get(),
            messagesFailed.get(),
            vitalQueue.size(),
            calculateSuccessRate()
        );
    }
    
    // Reset statistics
    public void resetStats() {
        messagesSent.set(0);
        messagesFailed.set(0);
        vitalQueue.clear();
        logger.info("Reset Kafka producer statistics");
    }
    
    // Health check
    public boolean isHealthy() {
        try {
            // Try to send a test message
            kafkaTemplate.send(simulationConfig.getKafkaProducer().getTopic(), "health_check", "test");
            return true;
        } catch (Exception e) {
            logger.error("Kafka producer health check failed: {}", e.getMessage());
            return false;
        }
    }
    
    // Utility methods
    private VitalReadingMessage convertToVitalReadingMessage(VitalDataGenerator.VitalReading reading) {
        VitalReadingMessage message = new VitalReadingMessage();
        message.setPatientIdentifier(reading.getPatientId());
        message.setVitalType(reading.getVitalType());
        message.setValue(reading.getValue());
        message.setUnit(reading.getUnit());
        message.setReadingTimestamp(reading.getTimestamp());
        message.setSource(reading.getSource());
        message.setDeviceId(reading.getDeviceId());
        message.setLocation(reading.getLocation());
        message.setQualityScore(reading.getQualityScore());
        message.setNotes(reading.getNotes());
        message.setMessageId("sim_" + System.currentTimeMillis() + "_" + reading.getPatientId());
        message.setProcessingSource("vital-simulator");
        
        // Handle blood pressure
        if ("BLOOD_PRESSURE".equals(reading.getVitalType())) {
            message.setSystolic(reading.getSystolic());
            message.setDiastolic(reading.getDiastolic());
        }
        
        return message;
    }
    
    private double calculateSuccessRate() {
        long total = messagesSent.get() + messagesFailed.get();
        return total > 0 ? (double) messagesSent.get() / total : 0.0;
    }
    
    // Inner classes
    public static class VitalReadingMessage {
        private String patientIdentifier;
        private String vitalType;
        private java.math.BigDecimal value;
        private String unit;
        private java.math.BigDecimal systolic;
        private java.math.BigDecimal diastolic;
        private LocalDateTime readingTimestamp;
        private String source;
        private String deviceId;
        private String location;
        private java.math.BigDecimal qualityScore;
        private String notes;
        private String messageId;
        private String correlationId;
        private String processingSource;
        
        // Getters and setters
        public String getPatientIdentifier() { return patientIdentifier; }
        public void setPatientIdentifier(String patientIdentifier) { this.patientIdentifier = patientIdentifier; }
        
        public String getVitalType() { return vitalType; }
        public void setVitalType(String vitalType) { this.vitalType = vitalType; }
        
        public java.math.BigDecimal getValue() { return value; }
        public void setValue(java.math.BigDecimal value) { this.value = value; }
        
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
        
        public java.math.BigDecimal getSystolic() { return systolic; }
        public void setSystolic(java.math.BigDecimal systolic) { this.systolic = systolic; }
        
        public java.math.BigDecimal getDiastolic() { return diastolic; }
        public void setDiastolic(java.math.BigDecimal diastolic) { this.diastolic = diastolic; }
        
        public LocalDateTime getReadingTimestamp() { return readingTimestamp; }
        public void setReadingTimestamp(LocalDateTime readingTimestamp) { this.readingTimestamp = readingTimestamp; }
        
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        
        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
        
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        
        public java.math.BigDecimal getQualityScore() { return qualityScore; }
        public void setQualityScore(java.math.BigDecimal qualityScore) { this.qualityScore = qualityScore; }
        
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
        
        public String getMessageId() { return messageId; }
        public void setMessageId(String messageId) { this.messageId = messageId; }
        
        public String getCorrelationId() { return correlationId; }
        public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
        
        public String getProcessingSource() { return processingSource; }
        public void setProcessingSource(String processingSource) { this.processingSource = processingSource; }
    }
    
    public static class AnomalyAlert {
        private String patientId;
        private String vitalType;
        private String anomalyType;
        private String description;
        private LocalDateTime timestamp;
        
        public AnomalyAlert(String patientId, String vitalType, String anomalyType, 
                          String description, LocalDateTime timestamp) {
            this.patientId = patientId;
            this.vitalType = vitalType;
            this.anomalyType = anomalyType;
            this.description = description;
            this.timestamp = timestamp;
        }
        
        // Getters
        public String getPatientId() { return patientId; }
        public String getVitalType() { return vitalType; }
        public String getAnomalyType() { return anomalyType; }
        public String getDescription() { return description; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
    
    public static class SimulationStatus {
        private String status;
        private int activeSimulations;
        private int totalPatients;
        private String lastUpdate;
        private Map<String, Object> metrics;
        
        public SimulationStatus(String status, int activeSimulations, int totalPatients, 
                              String lastUpdate, Map<String, Object> metrics) {
            this.status = status;
            this.activeSimulations = activeSimulations;
            this.totalPatients = totalPatients;
            this.lastUpdate = lastUpdate;
            this.metrics = metrics;
        }
        
        // Getters
        public String getStatus() { return status; }
        public int getActiveSimulations() { return activeSimulations; }
        public int getTotalPatients() { return totalPatients; }
        public String getLastUpdate() { return lastUpdate; }
        public Map<String, Object> getMetrics() { return metrics; }
    }
    
    public static class ProductionStats {
        private long messagesSent;
        private long messagesFailed;
        private int queueSize;
        private double successRate;
        
        public ProductionStats(long messagesSent, long messagesFailed, int queueSize, double successRate) {
            this.messagesSent = messagesSent;
            this.messagesFailed = messagesFailed;
            this.queueSize = queueSize;
            this.successRate = successRate;
        }
        
        // Getters
        public long getMessagesSent() { return messagesSent; }
        public long getMessagesFailed() { return messagesFailed; }
        public int getQueueSize() { return queueSize; }
        public double getSuccessRate() { return successRate; }
    }
}
