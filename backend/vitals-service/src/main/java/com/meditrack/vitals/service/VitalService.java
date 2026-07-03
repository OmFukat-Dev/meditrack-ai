package com.meditrack.vitals.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meditrack.vitals.dto.VitalReadingEvent;
import com.meditrack.vitals.dto.VitalReadingMessage;
import com.meditrack.vitals.entity.Patient;
import com.meditrack.vitals.entity.VitalReading;
import com.meditrack.vitals.repository.PatientRepository;
import com.meditrack.vitals.repository.VitalReadingRepository;
import com.meditrack.vitals.service.VitalValidationService.ValidationResult;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Map;

@Service
@Transactional
public class VitalService {
    
    private static final Logger logger = LoggerFactory.getLogger(VitalService.class);
    
    @Autowired
    private VitalReadingRepository vitalReadingRepository;
    
    @Autowired
    private PatientRepository patientRepository;
    
    @Autowired
    private VitalValidationService validationService;
    
    @Autowired
    private RedisCacheService redisCacheService;
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // Main vital processing methods
    @Counted(value = "vital.processed", description = "Number of vitals processed")
    @Timed(value = "vital.process.time", description = "Time taken to process vital")
    public VitalReading processVitalReading(VitalReadingMessage message) {
        logger.info("Processing vital reading: patientId={}, vitalType={}, value={}", 
                   message.getPatientIdentifier(), message.getVitalType(), message.getDisplayValue());
        
        // Validate and normalize message
        ValidationResult validationResult = validationService.validateAndNormalize(message);
        if (!validationResult.isValid()) {
            logger.warn("Vital reading validation failed: {}", validationResult.getErrors());
            throw new IllegalArgumentException("Invalid vital reading: " + String.join(", ", validationResult.getErrors()));
        }
        
        VitalReadingMessage normalizedMessage = validationResult.getNormalizedMessage();
        
        // Find patient
        Optional<Patient> patientOpt = patientRepository.findByPatientIdentifier(normalizedMessage.getPatientIdentifier());
        if (patientOpt.isEmpty()) {
            logger.warn("Patient not found: {}", normalizedMessage.getPatientIdentifier());
            throw new IllegalArgumentException("Patient not found: " + normalizedMessage.getPatientIdentifier());
        }
        
        Patient patient = patientOpt.get();
        
        // Check for duplicates
        VitalReading latestReading = vitalReadingRepository.findLatestByPatientIdAndVitalType(
            patient.getId(), normalizedMessage.getVitalType());
        
        if (validationService.isDuplicate(normalizedMessage, latestReading)) {
            logger.info("Duplicate vital reading detected, skipping: patientId={}, vitalType={}", 
                       patient.getId(), normalizedMessage.getVitalType());
            return latestReading;
        }
        
        // Check for rapid changes
        if (latestReading != null && validationService.hasRapidChange(
            convertToVitalReading(normalizedMessage, patient), latestReading)) {
            logger.warn("Rapid change detected: patientId={}, vitalType={}, previous={}, current={}", 
                       patient.getId(), normalizedMessage.getVitalType(), 
                       latestReading.getDisplayValue(), normalizedMessage.getDisplayValue());
            
            // Trigger alert for rapid change
            triggerRapidChangeAlert(patient.getId(), normalizedMessage, latestReading);
        }
        
        // Create vital reading
        VitalReading vitalReading = convertToVitalReading(normalizedMessage, patient);
        
        // Save to database
        VitalReading savedReading = vitalReadingRepository.save(vitalReading);
        
        // Update Redis cache
        cacheLatestVital(patient.getId(), savedReading);
        
        // Check for alerts
        checkAndTriggerAlerts(patient.getId(), savedReading);
        
        // Publish to Kafka for downstream services
        publishVitalEvent(savedReading);
        
        logger.info("Successfully processed vital reading: id={}, patientId={}, vitalType={}", 
                   savedReading.getId(), patient.getId(), savedReading.getVitalType());
        
        return savedReading;
    }
    
    // Redis caching methods
    public void cacheLatestVital(Long patientId, VitalReading vitalReading) {
        try {
            String cacheKey = "latest_vital:" + patientId + ":" + vitalReading.getVitalType();
            redisCacheService.set(cacheKey, vitalReading, 3600); // Cache for 1 hour
            
            // Also cache in patient summary
            updatePatientVitalSummary(patientId, vitalReading);
            
            logger.debug("Cached latest vital: patientId={}, vitalType={}, value={}", 
                        patientId, vitalReading.getVitalType(), vitalReading.getDisplayValue());
        } catch (Exception e) {
            logger.error("Error caching vital reading: {}", e.getMessage(), e);
        }
    }
    
    public VitalReading getLatestVitalFromCache(Long patientId, String vitalType) {
        try {
            String cacheKey = "latest_vital:" + patientId + ":" + vitalType;
            return redisCacheService.get(cacheKey, VitalReading.class);
        } catch (Exception e) {
            logger.error("Error retrieving latest vital from cache: {}", e.getMessage(), e);
            return null;
        }
    }
    
    private void updatePatientVitalSummary(Long patientId, VitalReading vitalReading) {
        try {
            String summaryKey = "vital_summary:" + patientId;
            
            // Get existing summary
            String summaryJson = redisCacheService.get(summaryKey, String.class);
            PatientVitalSummary summary = summaryJson != null ? 
                parseVitalSummary(summaryJson) : new PatientVitalSummary();
            
            // Update summary
            summary.updateVital(vitalReading);
            summary.setLastUpdated(LocalDateTime.now());
            
            // Cache updated summary
            redisCacheService.set(summaryKey, summary, 3600); // Cache for 1 hour
            
        } catch (Exception e) {
            logger.error("Error updating patient vital summary: {}", e.getMessage(), e);
        }
    }
    
    // Alert checking methods
    public void checkAndTriggerAlerts(Long patientId, VitalReading vitalReading) {
        try {
            // Check for abnormal readings
            if (isAbnormalReading(vitalReading)) {
                triggerAbnormalAlert(patientId, vitalReading);
            }
            
            // Check for critical readings
            if (isCriticalReading(vitalReading)) {
                triggerCriticalAlert(patientId, vitalReading);
            }
            
            // Check for trend abnormalities
            checkTrendAlerts(patientId, vitalReading);
            
        } catch (Exception e) {
            logger.error("Error checking alerts: {}", e.getMessage(), e);
        }
    }
    
    private boolean isAbnormalReading(VitalReading vitalReading) {
        String status = vitalReading.getVitalStatus();
        return !"NORMAL".equals(status);
    }
    
    private boolean isCriticalReading(VitalReading vitalReading) {
        switch (vitalReading.getVitalType()) {
            case "HEART_RATE":
                return vitalReading.getValue().compareTo(new BigDecimal("40")) < 0 || 
                       vitalReading.getValue().compareTo(new BigDecimal("180")) > 0;
            case "TEMPERATURE":
                return vitalReading.getValue().compareTo(new BigDecimal("34.0")) < 0 || 
                       vitalReading.getValue().compareTo(new BigDecimal("41.0")) > 0;
            case "SPO2":
                return vitalReading.getValue().compareTo(new BigDecimal("85")) < 0;
            case "RESPIRATORY_RATE":
                return vitalReading.getValue().compareTo(new BigDecimal("8")) < 0 || 
                       vitalReading.getValue().compareTo(new BigDecimal("35")) > 0;
            case "BLOOD_PRESSURE":
                return (vitalReading.getSystolic() != null && 
                        (vitalReading.getSystolic().compareTo(new BigDecimal("70")) < 0 || 
                         vitalReading.getSystolic().compareTo(new BigDecimal("200")) > 0)) ||
                       (vitalReading.getDiastolic() != null && 
                        (vitalReading.getDiastolic().compareTo(new BigDecimal("40")) < 0 || 
                         vitalReading.getDiastolic().compareTo(new BigDecimal("120")) > 0));
            default:
                return false;
        }
    }
    
    private void checkTrendAlerts(Long patientId, VitalReading currentReading) {
        try {
            // Get recent readings for trend analysis
            LocalDateTime startTime = currentReading.getReadingTimestamp().minusHours(2);
            List<VitalReading> recentReadings = vitalReadingRepository.findByPatientIdAndVitalTypeAndTimeRange(
                patientId, currentReading.getVitalType(), startTime, currentReading.getReadingTimestamp());
            
            if (recentReadings.size() < 3) {
                return; // Not enough data for trend analysis
            }
            
            // Simple trend detection
            BigDecimal trend = calculateTrend(recentReadings);
            if (Math.abs(trend.doubleValue()) > 0.1) { // 10% trend threshold
                triggerTrendAlert(patientId, currentReading, trend);
            }
            
        } catch (Exception e) {
            logger.error("Error checking trend alerts: {}", e.getMessage(), e);
        }
    }
    
    private BigDecimal calculateTrend(List<VitalReading> readings) {
        if (readings.size() < 2) return BigDecimal.ZERO;
        
        // Simple linear trend calculation
        BigDecimal firstValue = readings.get(0).getValue();
        BigDecimal lastValue = readings.get(readings.size() - 1).getValue();
        
        return lastValue.subtract(firstValue).divide(new BigDecimal(readings.size() - 1), 2, BigDecimal.ROUND_HALF_UP);
    }
    
    // Alert triggering methods
    private void triggerAbnormalAlert(Long patientId, VitalReading vitalReading) {
        try {
            String alertMessage = String.format("Abnormal %s reading: %s (Status: %s)", 
                vitalReading.getVitalType(), vitalReading.getDisplayValue(), vitalReading.getVitalStatus());
            
            AlertMessage alert = new AlertMessage(
                patientId, 
                "ABNORMAL_VITAL", 
                alertMessage, 
                "MEDIUM",
                vitalReading.getVitalType(),
                vitalReading.getReadingTimestamp(),
                vitalReading.getNurseId(),
                vitalReading.getDepartment()
            );
            
            publishAlert(alert);
            logger.info("Triggered abnormal alert: patientId={}, vitalType={}, value={}", 
                       patientId, vitalReading.getVitalType(), vitalReading.getDisplayValue());
        } catch (Exception e) {
            logger.error("Error triggering abnormal alert: {}", e.getMessage(), e);
        }
    }
    
    private void triggerCriticalAlert(Long patientId, VitalReading vitalReading) {
        try {
            String alertMessage = String.format("CRITICAL %s reading: %s", 
                vitalReading.getVitalType(), vitalReading.getDisplayValue());
            
            AlertMessage alert = new AlertMessage(
                patientId, 
                "CRITICAL_VITAL", 
                alertMessage, 
                "CRITICAL",
                vitalReading.getVitalType(),
                vitalReading.getReadingTimestamp(),
                vitalReading.getNurseId(),
                vitalReading.getDepartment()
            );
            
            publishAlert(alert);
            logger.warn("Triggered critical alert: patientId={}, vitalType={}, value={}", 
                       patientId, vitalReading.getVitalType(), vitalReading.getDisplayValue());
        } catch (Exception e) {
            logger.error("Error triggering critical alert: {}", e.getMessage(), e);
        }
    }
    
    private void triggerRapidChangeAlert(Long patientId, VitalReadingMessage current, VitalReading previous) {
        try {
            String alertMessage = String.format("Rapid change in %s: %s → %s", 
                current.getVitalType(), previous.getDisplayValue(), current.getDisplayValue());
            
            AlertMessage alert = new AlertMessage(
                patientId, 
                "RAPID_CHANGE", 
                alertMessage, 
                "HIGH",
                current.getVitalType(),
                current.getReadingTimestamp(),
                current.getNurseId(),
                current.getDepartment()
            );
            
            publishAlert(alert);
            logger.warn("Triggered rapid change alert: patientId={}, vitalType={}, change={}", 
                       patientId, current.getVitalType(), alertMessage);
        } catch (Exception e) {
            logger.error("Error triggering rapid change alert: {}", e.getMessage(), e);
        }
    }
    
    private void triggerTrendAlert(Long patientId, VitalReading vitalReading, BigDecimal trend) {
        try {
            String trendDirection = trend.compareTo(BigDecimal.ZERO) > 0 ? "increasing" : "decreasing";
            String alertMessage = String.format("Trending %s in %s: %s", 
                trendDirection, vitalReading.getVitalType(), vitalReading.getDisplayValue());
            
            AlertMessage alert = new AlertMessage(
                patientId, 
                "TREND_ALERT", 
                alertMessage, 
                "MEDIUM",
                vitalReading.getVitalType(),
                vitalReading.getReadingTimestamp(),
                vitalReading.getNurseId(),
                vitalReading.getDepartment()
            );
            
            publishAlert(alert);
            logger.info("Triggered trend alert: patientId={}, vitalType={}, trend={}", 
                       patientId, vitalReading.getVitalType(), trendDirection);
        } catch (Exception e) {
            logger.error("Error triggering trend alert: {}", e.getMessage(), e);
        }
    }
    
    // Kafka publishing methods
    private ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
            objectMapper.findAndRegisterModules();
        }
        return objectMapper;
    }

    private void publishVitalEvent(VitalReading vitalReading) {
        try {
            VitalReadingEvent event = new VitalReadingEvent();
            event.setEventId(java.util.UUID.randomUUID().toString());
            event.setEventType("VITAL_RECORDED");
            event.setTimestamp(vitalReading.getReadingTimestamp());
            event.setPatientId(String.valueOf(vitalReading.getPatient().getId()));
            event.setVitalType(vitalReading.getVitalType());
            event.setValue(vitalReading.getValue());
            event.setUnit(vitalReading.getUnit());
            event.setSystolic(vitalReading.getSystolic() != null ? vitalReading.getSystolic().toPlainString() : null);
            event.setDiastolic(vitalReading.getDiastolic() != null ? vitalReading.getDiastolic().toPlainString() : null);
            event.setNurseId(vitalReading.getNurseId());
            event.setDepartment(vitalReading.getDepartment());
            event.setCreatedBy(vitalReading.getNurseId());
            event.setRole("NURSE");
            event.setMessage(vitalReading.getNotes());
            event.setSeverity(vitalReading.getVitalStatus());
            event.setRiskLevel(vitalReading.getVitalStatus());

            String eventJson = getObjectMapper().writeValueAsString(event);
            kafkaTemplate.send("patient-vitals", event.getPatientId(), eventJson);
            kafkaTemplate.send("vital-events", eventJson);
            logger.debug("Published vital event: {}", eventJson);
        } catch (Exception e) {
            logger.error("Error publishing vital event: {}", e.getMessage(), e);
        }
    }
    
    private void publishAlert(AlertMessage alert) {
        try {
            VitalReadingEvent alertEvent = new VitalReadingEvent();
            alertEvent.setEventId(java.util.UUID.randomUUID().toString());
            alertEvent.setEventType("ALERT_GENERATED");
            alertEvent.setTimestamp(alert.getTimestamp());
            alertEvent.setPatientId(String.valueOf(alert.getPatientId()));
            alertEvent.setVitalType(alert.getVitalType());
            alertEvent.setSeverity(alert.getSeverity());
            alertEvent.setMessage(alert.getMessage());
            alertEvent.setNurseId(alert.getNurseId());
            alertEvent.setDepartment(alert.getDepartment());

            String alertJson = getObjectMapper().writeValueAsString(alertEvent);
            kafkaTemplate.send("patient-alerts", String.valueOf(alert.getPatientId()), alertJson);
            kafkaTemplate.send("vital-alerts", alertJson);
            logger.debug("Published alert: {}", alertJson);
        } catch (Exception e) {
            logger.error("Error publishing alert: {}", e.getMessage(), e);
        }
    }
    
    // Utility methods
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
        vitalReading.setNurseId(message.getNurseId());
        vitalReading.setDepartment(message.getDepartment());
        
        // Handle blood pressure
        if ("BLOOD_PRESSURE".equals(message.getVitalType())) {
            vitalReading.setSystolic(message.getSystolic());
            vitalReading.setDiastolic(message.getDiastolic());
        }
        
        return vitalReading;
    }
    
    private PatientVitalSummary parseVitalSummary(String json) {
        // Simple implementation - in production, use proper JSON parsing
        return new PatientVitalSummary();
    }
    
    // Query methods
    public Page<VitalReading> getVitalReadings(Long patientId, String vitalType, 
                                               LocalDateTime startTime, LocalDateTime endTime, 
                                               int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "readingTimestamp"));
        
        if (startTime != null && endTime != null) {
            List<VitalReading> readings = vitalType != null
                ? vitalReadingRepository.findByPatientIdAndVitalTypeAndTimeRange(patientId, vitalType, startTime, endTime)
                : vitalReadingRepository.findByPatientIdAndTimeRange(patientId, startTime, endTime);
            readings.sort(Comparator.comparing(VitalReading::getReadingTimestamp,
                Comparator.nullsLast(Comparator.naturalOrder())).reversed());
            return toPage(readings, pageable);
        } else if (vitalType != null) {
            return vitalReadingRepository.findByPatientIdAndVitalType(patientId, vitalType, pageable);
        } else {
            return vitalReadingRepository.findByPatientId(patientId, pageable);
        }
    }
    
    public Map<String, Object> getPatientVitalAnalysis(Long patientId, LocalDateTime since) {
        LocalDateTime effectiveSince = since != null ? since : LocalDateTime.now().minusHours(24);

        List<VitalReading> recentReadings = vitalReadingRepository.findRecentByPatientId(patientId, effectiveSince);
        List<VitalReading> abnormalReadings = vitalReadingRepository.findAbnormalReadingsByPatientId(patientId, effectiveSince);
        List<VitalReading> criticalReadings = vitalReadingRepository.findCriticalReadingsByPatientId(patientId, effectiveSince);
        List<VitalReading> latestVitals = getLatestVitals(patientId);

        Map<String, Object> analysis = new LinkedHashMap<>();
        analysis.put("patientId", patientId);
        analysis.put("generatedAt", LocalDateTime.now());
        analysis.put("windowStart", effectiveSince);
        analysis.put("latestVitals", latestVitals);
        analysis.put("recentReadingCount", recentReadings.size());
        analysis.put("abnormalCount", abnormalReadings.size());
        analysis.put("criticalCount", criticalReadings.size());
        analysis.put("overallStatus", determineOverallStatus(abnormalReadings.size(), criticalReadings.size(), recentReadings.size()));
        analysis.put("riskLevel", determineRiskLevel(abnormalReadings.size(), criticalReadings.size()));
        analysis.put("trendSummary", buildTrendSummary(recentReadings));
        analysis.put("vitalCount", latestVitals.size());

        return analysis;
    }

    public List<VitalReading> getLatestVitals(Long patientId) {
        List<VitalReading> latestVitals = new ArrayList<>();
        
        String[] vitalTypes = {"HEART_RATE", "BLOOD_PRESSURE", "TEMPERATURE", "SPO2", "RESPIRATORY_RATE"};
        
        for (String vitalType : vitalTypes) {
            VitalReading latest = getLatestVitalFromCache(patientId, vitalType);
            if (latest == null) {
                latest = vitalReadingRepository.findLatestByPatientIdAndVitalType(patientId, vitalType);
                if (latest != null) {
                    cacheLatestVital(patientId, latest);
                }
            }
            if (latest != null) {
                latestVitals.add(latest);
            }
        }
        
        return latestVitals;
    }

    private String determineOverallStatus(int abnormalCount, int criticalCount, int recentReadingCount) {
        if (criticalCount > 0) {
            return "ALERT";
        }
        if (abnormalCount > 0) {
            return recentReadingCount > 0 ? "WATCH" : "NO_DATA";
        }
        return recentReadingCount > 0 ? "STABLE" : "NO_DATA";
    }

    private String determineRiskLevel(int abnormalCount, int criticalCount) {
        if (criticalCount > 0) {
            return "CRITICAL";
        }
        if (abnormalCount >= 3) {
            return "HIGH";
        }
        if (abnormalCount > 0) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private Map<String, Object> buildTrendSummary(List<VitalReading> recentReadings) {
        List<Map<String, Object>> items = new ArrayList<>();
        String[] vitalTypes = {"HEART_RATE", "BLOOD_PRESSURE", "TEMPERATURE", "SPO2", "RESPIRATORY_RATE"};

        for (String vitalType : vitalTypes) {
            List<VitalReading> typeReadings = recentReadings.stream()
                .filter(reading -> vitalType.equals(reading.getVitalType()))
                .sorted(Comparator.comparing(VitalReading::getReadingTimestamp, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

            if (typeReadings.size() < 2) {
                continue;
            }

            BigDecimal firstValue = getTrendValue(typeReadings.get(0));
            BigDecimal latestValue = getTrendValue(typeReadings.get(typeReadings.size() - 1));
            BigDecimal delta = latestValue.subtract(firstValue);
            String direction = delta.compareTo(BigDecimal.ZERO) > 0 ? "INCREASING" : delta.compareTo(BigDecimal.ZERO) < 0 ? "DECREASING" : "STABLE";

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("vitalType", vitalType);
            item.put("startValue", firstValue);
            item.put("latestValue", latestValue);
            item.put("delta", delta);
            item.put("direction", direction);
            items.add(item);
        }

        Map<String, Object> trendSummary = new LinkedHashMap<>();
        trendSummary.put("items", items);
        trendSummary.put("hasTrendData", !items.isEmpty());
        trendSummary.put("summary", items.isEmpty() ? "Insufficient recent data for trend analysis" : "Trend analysis available for recent measurements");
        return trendSummary;
    }

    private BigDecimal getTrendValue(VitalReading reading) {
        if ("BLOOD_PRESSURE".equals(reading.getVitalType()) && reading.getSystolic() != null) {
            return reading.getSystolic();
        }
        return reading.getValue() != null ? reading.getValue() : BigDecimal.ZERO;
    }
    
    public List<VitalReading> getAbnormalReadings(Long patientId, LocalDateTime since) {
        return vitalReadingRepository.findAbnormalReadingsByPatientId(patientId, since);
    }
    
    public List<VitalReading> getCriticalReadings(Long patientId, LocalDateTime since) {
        return vitalReadingRepository.findCriticalReadingsByPatientId(patientId, since);
    }

    public boolean isHealthy() {
        return redisCacheService.isHealthy();
    }

    public Map<String, Object> getCacheStats() {
        return redisCacheService.getCacheStats();
    }

    private Page<VitalReading> toPage(List<VitalReading> readings, Pageable pageable) {
        int start = (int) pageable.getOffset();
        if (start >= readings.size()) {
            return Page.empty(pageable);
        }

        int end = Math.min(start + pageable.getPageSize(), readings.size());
        return new PageImpl<>(readings.subList(start, end), pageable, readings.size());
    }
    
    // Inner classes
    public static class PatientVitalSummary {
        private Long patientId;
        private LocalDateTime lastUpdated;
        private String latestHeartRate;
        private String latestBloodPressure;
        private String latestTemperature;
        private String latestSpo2;
        private String latestRespiratoryRate;
        
        public void updateVital(VitalReading vitalReading) {
            switch (vitalReading.getVitalType()) {
                case "HEART_RATE":
                    latestHeartRate = vitalReading.getDisplayValue();
                    break;
                case "BLOOD_PRESSURE":
                    latestBloodPressure = vitalReading.getDisplayValue();
                    break;
                case "TEMPERATURE":
                    latestTemperature = vitalReading.getDisplayValue();
                    break;
                case "SPO2":
                    latestSpo2 = vitalReading.getDisplayValue();
                    break;
                case "RESPIRATORY_RATE":
                    latestRespiratoryRate = vitalReading.getDisplayValue();
                    break;
            }
        }
        
        // Getters and setters
        public Long getPatientId() { return patientId; }
        public void setPatientId(Long patientId) { this.patientId = patientId; }
        
        public LocalDateTime getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
        
        public String getLatestHeartRate() { return latestHeartRate; }
        public void setLatestHeartRate(String latestHeartRate) { this.latestHeartRate = latestHeartRate; }
        
        public String getLatestBloodPressure() { return latestBloodPressure; }
        public void setLatestBloodPressure(String latestBloodPressure) { this.latestBloodPressure = latestBloodPressure; }
        
        public String getLatestTemperature() { return latestTemperature; }
        public void setLatestTemperature(String latestTemperature) { this.latestTemperature = latestTemperature; }
        
        public String getLatestSpo2() { return latestSpo2; }
        public void setLatestSpo2(String latestSpo2) { this.latestSpo2 = latestSpo2; }
        
        public String getLatestRespiratoryRate() { return latestRespiratoryRate; }
        public void setLatestRespiratoryRate(String latestRespiratoryRate) { this.latestRespiratoryRate = latestRespiratoryRate; }
    }
    
    public static class AlertMessage {
        private Long patientId;
        private String alertType;
        private String message;
        private String severity;
        private String vitalType;
        private LocalDateTime timestamp;
        private String nurseId;
        private String department;
        
        public AlertMessage(Long patientId, String alertType, String message, 
                         String severity, String vitalType, LocalDateTime timestamp,
                         String nurseId, String department) {
            this.patientId = patientId;
            this.alertType = alertType;
            this.message = message;
            this.severity = severity;
            this.vitalType = vitalType;
            this.timestamp = timestamp;
            this.nurseId = nurseId;
            this.department = department;
        }
        
        // Getters
        public Long getPatientId() { return patientId; }
        public String getAlertType() { return alertType; }
        public String getMessage() { return message; }
        public String getSeverity() { return severity; }
        public String getVitalType() { return vitalType; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getNurseId() { return nurseId; }
        public String getDepartment() { return department; }
    }
    
    // Process threshold updates
    public void processThresholdUpdate(String thresholdMessage) {
        try {
            // Parse threshold update and update Redis
            logger.info("Processing threshold update: {}", thresholdMessage);
            
            // Implementation would parse JSON and update thresholds in Redis
            redisCacheService.set("vital_thresholds:" + thresholdMessage, thresholdMessage, 86400);
            
        } catch (Exception e) {
            logger.error("Error processing threshold update: {}", e.getMessage(), e);
        }
    }
}
