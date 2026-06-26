package com.meditrack.patient.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meditrack.patient.dto.PatientTimelineEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class PatientTimelineService {
    
    private static final Logger logger = LoggerFactory.getLogger(PatientTimelineService.class);

    private final ObjectMapper objectMapper;

    public PatientTimelineService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    // In-memory timeline storage (in production, use database)
    private final ConcurrentMap<String, List<PatientTimelineEvent>> patientTimelines = new ConcurrentHashMap<>();
    
    /**
     * Listen for vital reading events and add to timeline
     */
    @KafkaListener(topics = "patient-vitals", groupId = "timeline-group")
    public void handleVitalEvent(String vitalJson) {
        try {
            PatientTimelineEvent event = objectMapper.readValue(vitalJson, PatientTimelineEvent.class);
            
            if ("VITAL_RECORDED".equals(event.getEventType())) {
                logger.info("Adding vital to timeline: Patient={}, Type={}, Value={}", 
                    event.getPatientId(), event.getVitalType(), event.getValue());
                
                addToTimeline(event);
            }
            
        } catch (Exception e) {
            logger.error("Failed to process vital timeline event: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Listen for AI prediction events and add to timeline
     */
    @KafkaListener(topics = "patient-predictions", groupId = "timeline-group")
    public void handlePredictionEvent(String predictionJson) {
        try {
            PatientTimelineEvent event = objectMapper.readValue(predictionJson, PatientTimelineEvent.class);
            
            if ("PREDICTION_GENERATED".equals(event.getEventType())) {
                logger.info("Adding prediction to timeline: Patient={}, Risk={}, Confidence={}%", 
                    event.getPatientId(), event.getRiskLevel(), event.getConfidence());
                
                addToTimeline(event);
            }
            
        } catch (Exception e) {
            logger.error("Failed to process prediction timeline event: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Listen for alert events and add to timeline
     */
    @KafkaListener(topics = "patient-alerts", groupId = "timeline-group")
    public void handleAlertEvent(String alertJson) {
        try {
            PatientTimelineEvent event = objectMapper.readValue(alertJson, PatientTimelineEvent.class);
            
            if ("ALERT_GENERATED".equals(event.getEventType())) {
                logger.warn("Adding alert to timeline: Patient={}, Severity={}", 
                    event.getPatientId(), event.getSeverity());
                
                addToTimeline(event);
            }
            
        } catch (Exception e) {
            logger.error("Failed to process alert timeline event: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Add event to patient timeline
     */
    private void addToTimeline(PatientTimelineEvent event) {
        List<PatientTimelineEvent> timeline = patientTimelines.computeIfAbsent(
            event.getPatientId(),
            k -> Collections.synchronizedList(new ArrayList<>())
        );

        synchronized (timeline) {
            timeline.add(event);

            // Keep only last 100 events per patient
            if (timeline.size() > 100) {
                timeline.sort(Comparator.comparing(PatientTimelineEvent::getTimestamp).reversed());
                patientTimelines.put(event.getPatientId(), new ArrayList<>(timeline.subList(0, 100)));
            }
        }
    }
    
    /**
     * Get patient timeline
     */
    public List<PatientTimelineEvent> getPatientTimeline(String patientId) {
        List<PatientTimelineEvent> timeline = new ArrayList<>(patientTimelines.getOrDefault(patientId, new ArrayList<>()));
        
        // Sort by timestamp (most recent first)
        timeline.sort(Comparator.comparing(PatientTimelineEvent::getTimestamp).reversed());
        
        return timeline;
    }
    
    /**
     * Get patient timeline for time range
     */
    public List<PatientTimelineEvent> getPatientTimeline(String patientId, LocalDateTime start, LocalDateTime end) {
        List<PatientTimelineEvent> fullTimeline = getPatientTimeline(patientId);
        
        return fullTimeline.stream()
            .filter(event -> !event.getTimestamp().isBefore(start) && !event.getTimestamp().isAfter(end))
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    /**
     * Add manual event to timeline (e.g., doctor notes, medication changes)
     */
    public void addManualEvent(String patientId, String eventType, String message, String userId) {
        PatientTimelineEvent event = new PatientTimelineEvent();
        event.setEventId(java.util.UUID.randomUUID().toString());
        event.setPatientId(patientId);
        event.setEventType(eventType);
        event.setTimestamp(LocalDateTime.now());
        event.setMessage(message);
        event.setUserId(userId);
        
        addToTimeline(event);
        
        logger.info("Manual event added to timeline: Patient={}, Type={}, User={}", 
            patientId, eventType, userId);
    }
    
    /**
     * Get timeline summary
     */
    public TimelineSummary getTimelineSummary(String patientId, LocalDateTime since) {
        List<PatientTimelineEvent> timeline = getPatientTimeline(patientId, since, LocalDateTime.now());
        
        TimelineSummary summary = new TimelineSummary();
        summary.setPatientId(patientId);
        summary.setTotalEvents(timeline.size());
        summary.setVitalEvents((int) timeline.stream().filter(e -> "VITAL_RECORDED".equals(e.getEventType())).count());
        summary.setPredictionEvents((int) timeline.stream().filter(e -> "PREDICTION_GENERATED".equals(e.getEventType())).count());
        summary.setAlertEvents((int) timeline.stream().filter(e -> "ALERT_GENERATED".equals(e.getEventType())).count());
        summary.setCriticalAlerts((int) timeline.stream()
            .filter(e -> "ALERT_GENERATED".equals(e.getEventType()) && "CRITICAL".equals(e.getSeverity()))
            .count());
        
        return summary;
    }
    
    /**
     * Timeline summary DTO
     */
    public static class TimelineSummary {
        private String patientId;
        private int totalEvents;
        private int vitalEvents;
        private int predictionEvents;
        private int alertEvents;
        private int criticalAlerts;
        
        // Getters and Setters
        public String getPatientId() { return patientId; }
        public void setPatientId(String patientId) { this.patientId = patientId; }
        
        public int getTotalEvents() { return totalEvents; }
        public void setTotalEvents(int totalEvents) { this.totalEvents = totalEvents; }
        
        public int getVitalEvents() { return vitalEvents; }
        public void setVitalEvents(int vitalEvents) { this.vitalEvents = vitalEvents; }
        
        public int getPredictionEvents() { return predictionEvents; }
        public void setPredictionEvents(int predictionEvents) { this.predictionEvents = predictionEvents; }
        
        public int getAlertEvents() { return alertEvents; }
        public void setAlertEvents(int alertEvents) { this.alertEvents = alertEvents; }
        
        public int getCriticalAlerts() { return criticalAlerts; }
        public void setCriticalAlerts(int criticalAlerts) { this.criticalAlerts = criticalAlerts; }
    }
}
