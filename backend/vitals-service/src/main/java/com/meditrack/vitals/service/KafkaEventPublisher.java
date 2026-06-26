package com.meditrack.vitals.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meditrack.vitals.dto.VitalReadingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaEventPublisher {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaEventPublisher.class);
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // Kafka Topics
    private static final String VITALS_TOPIC = "patient-vitals";
    private static final String ALERTS_TOPIC = "patient-alerts";
    private static final String PREDICTIONS_TOPIC = "patient-predictions";
    
    /**
     * Publish vital reading event to Kafka
     */
    public void publishVitalReading(VitalReadingEvent event) {
        try {
            event.setEventId(java.util.UUID.randomUUID().toString());
            event.setTimestamp(java.time.LocalDateTime.now());
            event.setEventType("VITAL_RECORDED");

            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(VITALS_TOPIC, event.getPatientId(), eventJson);
            
            logger.info("Published vital reading event: Patient={}, Type={}, Value={}", 
                event.getPatientId(), event.getVitalType(), event.getValue());
                
        } catch (Exception e) {
            logger.error("Failed to publish vital reading event: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Publish alert event to Kafka
     */
    public void publishAlert(VitalReadingEvent event, String severity, String message) {
        try {
            VitalReadingEvent alertEvent = new VitalReadingEvent();
            alertEvent.setPatientId(event.getPatientId());
            alertEvent.setVitalType(event.getVitalType());
            alertEvent.setValue(event.getValue());
            alertEvent.setEventId(java.util.UUID.randomUUID().toString());
            alertEvent.setTimestamp(java.time.LocalDateTime.now());
            alertEvent.setEventType("ALERT_GENERATED");
            alertEvent.setSeverity(severity);
            alertEvent.setMessage(message);
            
            String alertJson = objectMapper.writeValueAsString(alertEvent);
            kafkaTemplate.send(ALERTS_TOPIC, event.getPatientId(), alertJson);
            
            logger.info("Published alert event: Patient={}, Severity={}, Message={}", 
                event.getPatientId(), severity, message);
                
        } catch (Exception e) {
            logger.error("Failed to publish alert event: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Publish AI prediction request
     */
    public void requestPrediction(String patientId) {
        try {
            VitalReadingEvent predictionRequest = new VitalReadingEvent();
            predictionRequest.setPatientId(patientId);
            predictionRequest.setEventId(java.util.UUID.randomUUID().toString());
            predictionRequest.setTimestamp(java.time.LocalDateTime.now());
            predictionRequest.setEventType("PREDICTION_REQUESTED");
            
            String requestJson = objectMapper.writeValueAsString(predictionRequest);
            kafkaTemplate.send(PREDICTIONS_TOPIC, patientId, requestJson);
            
            logger.info("Requested AI prediction for patient: {}", patientId);
            
        } catch (Exception e) {
            logger.error("Failed to request prediction: {}", e.getMessage(), e);
        }
    }
}
