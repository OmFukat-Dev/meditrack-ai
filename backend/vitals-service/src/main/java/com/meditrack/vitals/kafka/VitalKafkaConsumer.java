package com.meditrack.vitals.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meditrack.vitals.dto.VitalReadingMessage;
import com.meditrack.vitals.entity.Patient;
import com.meditrack.vitals.entity.VitalReading;
import com.meditrack.vitals.repository.PatientRepository;
import com.meditrack.vitals.repository.VitalReadingRepository;
import com.meditrack.vitals.service.VitalService;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class VitalKafkaConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(VitalKafkaConsumer.class);
    
    @Autowired
    private VitalReadingRepository vitalReadingRepository;
    
    @Autowired
    private PatientRepository patientRepository;
    
    @Autowired
    private VitalService vitalService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // Main vital readings consumer
    @KafkaListener(
        topics = "${meditrack.kafka.topics.vital-readings:vital-readings}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Counted(value = "kafka.vital.consumed", description = "Number of vital readings consumed")
    @Timed(value = "kafka.vital.consume.time", description = "Time taken to consume vital reading")
    public void consumeVitalReading(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String key,
            Acknowledgment acknowledgment) {
        
        try {
            logger.info("Consuming vital reading from topic: {}, partition: {}, offset: {}, key: {}", 
                       topic, partition, offset, key);
            
            // Deserialize message
            VitalReadingMessage vitalMessage = objectMapper.readValue(message, VitalReadingMessage.class);
            
            // Validate message
            if (!validateVitalMessage(vitalMessage)) {
                logger.warn("Invalid vital message received: {}", vitalMessage);
                acknowledgment.acknowledge();
                return;
            }
            
            // Find patient
            Optional<Patient> patientOpt = patientRepository.findByPatientIdentifier(vitalMessage.getPatientIdentifier());
            if (patientOpt.isEmpty()) {
                logger.warn("Patient not found for identifier: {}", vitalMessage.getPatientIdentifier());
                acknowledgment.acknowledge();
                return;
            }
            
            Patient patient = patientOpt.get();
            
            // Create vital reading
            VitalReading vitalReading = convertToVitalReading(vitalMessage, patient);
            
            // Save to database
            vitalReadingRepository.save(vitalReading);
            
            // Update Redis cache
            vitalService.cacheLatestVital(patient.getId(), vitalReading);
            
            // Check for alerts
            vitalService.checkAndTriggerAlerts(patient.getId(), vitalReading);
            
            // Log success
            logger.info("Successfully processed vital reading: patientId={}, vitalType={}, value={}", 
                       patient.getId(), vitalReading.getVitalType(), vitalReading.getDisplayValue());
            
            // Acknowledge message
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            logger.error("Error processing vital reading from Kafka: {}", e.getMessage(), e);
            
            // In production, you might want to implement dead letter queue handling
            // For now, we acknowledge to prevent reprocessing
            acknowledgment.acknowledge();
        }
    }
    
    // Batch vital readings consumer
    @KafkaListener(
        topics = "${meditrack.kafka.topics.vital-batches:vital-batches}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "batchKafkaListenerContainerFactory"
    )
    @Counted(value = "kafka.vital.batch.consumed", description = "Number of vital batches consumed")
    @Timed(value = "kafka.vital.batch.consume.time", description = "Time taken to consume vital batch")
    public void consumeVitalBatch(
            @Payload List<String> messages,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
            Acknowledgment acknowledgment) {
        
        try {
            logger.info("Consuming vital batch of {} messages from topic: {}, partition: {}", 
                       messages.size(), topic, partition);
            
            int successCount = 0;
            int errorCount = 0;
            
            for (String message : messages) {
                try {
                    VitalReadingMessage vitalMessage = objectMapper.readValue(message, VitalReadingMessage.class);
                    
                    if (!validateVitalMessage(vitalMessage)) {
                        errorCount++;
                        continue;
                    }
                    
                    Optional<Patient> patientOpt = patientRepository.findByPatientIdentifier(vitalMessage.getPatientIdentifier());
                    if (patientOpt.isEmpty()) {
                        errorCount++;
                        continue;
                    }
                    
                    Patient patient = patientOpt.get();
                    VitalReading vitalReading = convertToVitalReading(vitalMessage, patient);
                    
                    vitalReadingRepository.save(vitalReading);
                    vitalService.cacheLatestVital(patient.getId(), vitalReading);
                    
                    successCount++;
                    
                } catch (Exception e) {
                    logger.error("Error processing individual vital message in batch: {}", e.getMessage());
                    errorCount++;
                }
            }
            
            logger.info("Batch processing completed: success={}, errors={}", successCount, errorCount);
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            logger.error("Error processing vital batch from Kafka: {}", e.getMessage(), e);
            acknowledgment.acknowledge();
        }
    }
    
    // Vital threshold updates consumer
    @KafkaListener(
        topics = "${meditrack.kafka.topics.vital-thresholds:vital-thresholds}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Counted(value = "kafka.threshold.consumed", description = "Number of vital thresholds consumed")
    @Timed(value = "kafka.threshold.consume.time", description = "Time taken to consume vital threshold")
    public void consumeVitalThreshold(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {
        
        try {
            logger.info("Consuming vital threshold update from topic: {}", topic);
            
            // Process threshold update
            vitalService.processThresholdUpdate(message);
            
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            logger.error("Error processing vital threshold from Kafka: {}", e.getMessage(), e);
            acknowledgment.acknowledge();
        }
    }
    
    // Validation methods
    private boolean validateVitalMessage(VitalReadingMessage message) {
        if (message == null) {
            logger.warn("Null vital message received");
            return false;
        }
        
        if (message.getPatientIdentifier() == null || message.getPatientIdentifier().trim().isEmpty()) {
            logger.warn("Missing patient identifier in vital message");
            return false;
        }
        
        if (message.getVitalType() == null || message.getVitalType().trim().isEmpty()) {
            logger.warn("Missing vital type in vital message");
            return false;
        }
        
        if (message.getValue() == null) {
            logger.warn("Missing value in vital message");
            return false;
        }
        
        if (message.getReadingTimestamp() == null) {
            logger.warn("Missing reading timestamp in vital message");
            return false;
        }
        
        // Validate vital type
        List<String> validTypes = List.of("HEART_RATE", "BLOOD_PRESSURE", "TEMPERATURE", "SPO2", "RESPIRATORY_RATE");
        if (!validTypes.contains(message.getVitalType())) {
            logger.warn("Invalid vital type: {}", message.getVitalType());
            return false;
        }
        
        // Validate value ranges
        if (!isValidValue(message)) {
            logger.warn("Invalid value for vital type {}: {}", message.getVitalType(), message.getValue());
            return false;
        }
        
        return true;
    }
    
    private boolean isValidValue(VitalReadingMessage message) {
        BigDecimal value = message.getValue();
        String vitalType = message.getVitalType();
        
        switch (vitalType) {
            case "HEART_RATE":
                return value.compareTo(new BigDecimal("30")) >= 0 && value.compareTo(new BigDecimal("250")) <= 0;
            case "BLOOD_PRESSURE":
                // For blood pressure, we might have systolic/diastolic in separate fields
                return true; // Will be validated in VitalReading entity
            case "TEMPERATURE":
                return value.compareTo(new BigDecimal("25.0")) >= 0 && value.compareTo(new BigDecimal("45.0")) <= 0;
            case "SPO2":
                return value.compareTo(new BigDecimal("70")) >= 0 && value.compareTo(new BigDecimal("100")) <= 0;
            case "RESPIRATORY_RATE":
                return value.compareTo(new BigDecimal("5")) >= 0 && value.compareTo(new BigDecimal("60")) <= 0;
            default:
                return false;
        }
    }
    
    private VitalReading convertToVitalReading(VitalReadingMessage message, Patient patient) {
        VitalReading vitalReading = new VitalReading();
        
        vitalReading.setPatient(patient);
        vitalReading.setVitalType(message.getVitalType());
        vitalReading.setValue(message.getValue());
        vitalReading.setUnit(message.getUnit());
        vitalReading.setReadingTimestamp(message.getReadingTimestamp());
        vitalReading.setSource(message.getSource());
        vitalReading.setDeviceId(message.getDeviceId());
        vitalReading.setLocation(message.getLocation());
        vitalReading.setQualityScore(message.getQualityScore());
        vitalReading.setNotes(message.getNotes());
        
        // Handle blood pressure systolic/diastolic
        if ("BLOOD_PRESSURE".equals(message.getVitalType())) {
            vitalReading.setSystolic(message.getSystolic());
            vitalReading.setDiastolic(message.getDiastolic());
        }
        
        return vitalReading;
    }
}
