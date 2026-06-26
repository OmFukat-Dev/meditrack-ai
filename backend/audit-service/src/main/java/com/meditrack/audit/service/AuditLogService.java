package com.meditrack.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meditrack.audit.entity.AuditLog;
import com.meditrack.audit.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AuditLogService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);
    
    @Autowired
    private AuditLogRepository auditLogRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Log user action
     */
    @Transactional
    public AuditLog logUserAction(String userId, String username, String action, String details, String ipAddress) {
        AuditLog auditLog = new AuditLog();
        auditLog.setId(UUID.randomUUID().toString());
        auditLog.setUserId(userId);
        auditLog.setUsername(username);
        auditLog.setAction(action);
        auditLog.setDetails(details);
        auditLog.setIpAddress(ipAddress);
        auditLog.setTimestamp(LocalDateTime.now());
        auditLog.setEntityType("USER");
        
        AuditLog saved = auditLogRepository.save(auditLog);
        logger.info("Audit log created: User={}, Action={}", username, action);
        
        return saved;
    }
    
    /**
     * Log patient action
     */
    @Transactional
    public AuditLog logPatientAction(String userId, String username, String action, String patientId, String details) {
        AuditLog auditLog = new AuditLog();
        auditLog.setId(UUID.randomUUID().toString());
        auditLog.setUserId(userId);
        auditLog.setUsername(username);
        auditLog.setAction(action);
        auditLog.setPatientId(patientId);
        auditLog.setDetails(details);
        auditLog.setTimestamp(LocalDateTime.now());
        auditLog.setEntityType("PATIENT");
        
        AuditLog saved = auditLogRepository.save(auditLog);
        logger.info("Audit log created: User={}, Action={}, Patient={}", username, action, patientId);
        
        return saved;
    }
    
    /**
     * Log vital reading
     */
    @Transactional
    public AuditLog logVitalReading(String nurseId, String nurseName, String patientId, String vitalType, Object value) {
        AuditLog auditLog = new AuditLog();
        auditLog.setId(UUID.randomUUID().toString());
        auditLog.setUserId(nurseId);
        auditLog.setUsername(nurseName);
        auditLog.setAction("VITAL_RECORDED");
        auditLog.setPatientId(patientId);
        auditLog.setDetails(String.format("Vital Type: %s, Value: %s", vitalType, value));
        auditLog.setTimestamp(LocalDateTime.now());
        auditLog.setEntityType("VITAL");
        
        AuditLog saved = auditLogRepository.save(auditLog);
        logger.info("Audit log created: Nurse={}, Vital={}, Patient={}", nurseName, vitalType, patientId);
        
        return saved;
    }
    
    /**
     * Log alert action
     */
    @Transactional
    public AuditLog logAlertAction(String userId, String username, String action, String patientId, String alertSeverity) {
        AuditLog auditLog = new AuditLog();
        auditLog.setId(UUID.randomUUID().toString());
        auditLog.setUserId(userId);
        auditLog.setUsername(username);
        auditLog.setAction(action);
        auditLog.setPatientId(patientId);
        auditLog.setDetails(String.format("Alert Severity: %s", alertSeverity));
        auditLog.setTimestamp(LocalDateTime.now());
        auditLog.setEntityType("ALERT");
        
        AuditLog saved = auditLogRepository.save(auditLog);
        logger.info("Audit log created: User={}, Action={}, Alert={}", username, action, alertSeverity);
        
        return saved;
    }
    
    /**
     * Log prediction action
     */
    @Transactional
    public AuditLog logPredictionAction(String patientId, String riskLevel, String confidence) {
        AuditLog auditLog = new AuditLog();
        auditLog.setId(UUID.randomUUID().toString());
        auditLog.setUserId("AI_SERVICE");
        auditLog.setUsername("AI_PREDICTION");
        auditLog.setAction("PREDICTION_GENERATED");
        auditLog.setPatientId(patientId);
        auditLog.setDetails(String.format("Risk Level: %s, Confidence: %s", riskLevel, confidence));
        auditLog.setTimestamp(LocalDateTime.now());
        auditLog.setEntityType("PREDICTION");
        
        AuditLog saved = auditLogRepository.save(auditLog);
        logger.info("Audit log created: Prediction={}, Patient={}, Risk={}", 
            auditLog.getId(), patientId, riskLevel);
        
        return saved;
    }
    
    /**
     * Get audit logs by user
     */
    public List<AuditLog> getAuditLogsByUser(String userId, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "timestamp"));
        return auditLogRepository.findByUserId(userId, pageable);
    }
    
    /**
     * Get audit logs by patient
     */
    public List<AuditLog> getAuditLogsByPatient(String patientId, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "timestamp"));
        return auditLogRepository.findByPatientId(patientId, pageable);
    }
    
    /**
     * Get audit logs by entity type
     */
    public List<AuditLog> getAuditLogsByEntityType(String entityType, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "timestamp"));
        return auditLogRepository.findByEntityType(entityType, pageable);
    }
    
    /**
     * Get audit logs by action
     */
    public List<AuditLog> getAuditLogsByAction(String action, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "timestamp"));
        return auditLogRepository.findByAction(action, pageable);
    }
    
    /**
     * Get recent audit logs
     */
    public List<AuditLog> getRecentAuditLogs(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "timestamp"));
        return auditLogRepository.findAll(pageable).getContent();
    }
    
    /**
     * Get audit logs by date range
     */
    public List<AuditLog> getAuditLogsByDateRange(LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.findByTimestampBetween(start, end);
    }
    
    /**
     * Listen for audit events from Kafka
     */
    @KafkaListener(topics = "audit-events", groupId = "audit-service-group")
    public void handleAuditEvent(String auditJson) {
        try {
            AuditLog auditLog = objectMapper.readValue(auditJson, AuditLog.class);
            
            // Ensure timestamp is set
            if (auditLog.getTimestamp() == null) {
                auditLog.setTimestamp(LocalDateTime.now());
            }
            
            // Generate ID if not present
            if (auditLog.getId() == null) {
                auditLog.setId(UUID.randomUUID().toString());
            }
            
            auditLogRepository.save(auditLog);
            
            logger.info("Audit event received and logged: Action={}, User={}", 
                auditLog.getAction(), auditLog.getUsername());
                
        } catch (Exception e) {
            logger.error("Failed to process audit event: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Get audit statistics
     */
    public AuditStatistics getAuditStatistics(LocalDateTime since) {
        List<AuditLog> logs = auditLogRepository.findByTimestampAfter(since);
        
        AuditStatistics stats = new AuditStatistics();
        stats.setTotalLogs(logs.size());
        stats.setUserActions((int) logs.stream().filter(l -> "USER".equals(l.getEntityType())).count());
        stats.setPatientActions((int) logs.stream().filter(l -> "PATIENT".equals(l.getEntityType())).count());
        stats.setVitalReadings((int) logs.stream().filter(l -> "VITAL".equals(l.getEntityType())).count());
        stats.setAlertActions((int) logs.stream().filter(l -> "ALERT".equals(l.getEntityType())).count());
        stats.setPredictionActions((int) logs.stream().filter(l -> "PREDICTION".equals(l.getEntityType())).count());
        
        return stats;
    }
    
    /**
     * Audit statistics DTO
     */
    public static class AuditStatistics {
        private int totalLogs;
        private int userActions;
        private int patientActions;
        private int vitalReadings;
        private int alertActions;
        private int predictionActions;
        
        // Getters and Setters
        public int getTotalLogs() { return totalLogs; }
        public void setTotalLogs(int totalLogs) { this.totalLogs = totalLogs; }
        
        public int getUserActions() { return userActions; }
        public void setUserActions(int userActions) { this.userActions = userActions; }
        
        public int getPatientActions() { return patientActions; }
        public void setPatientActions(int patientActions) { this.patientActions = patientActions; }
        
        public int getVitalReadings() { return vitalReadings; }
        public void setVitalReadings(int vitalReadings) { this.vitalReadings = vitalReadings; }
        
        public int getAlertActions() { return alertActions; }
        public void setAlertActions(int alertActions) { this.alertActions = alertActions; }
        
        public int getPredictionActions() { return predictionActions; }
        public void setPredictionActions(int predictionActions) { this.predictionActions = predictionActions; }
    }
}
