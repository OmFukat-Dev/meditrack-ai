package com.meditrack.user.service;

import com.meditrack.user.entity.AuditLog;
import com.meditrack.user.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class AuditService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
    
    @Autowired
    private AuditLogRepository auditLogRepository;
    
    public void logAction(String userId, String action, String entityType, String entityId, 
                         Object oldValues, Object newValues, String ipAddress, String userAgent) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(userId);
            auditLog.setAction(action);
            auditLog.setEntityType(entityType);
            auditLog.setEntityId(entityId);
            auditLog.setIpAddress(ipAddress);
            auditLog.setUserAgent(userAgent);
            auditLog.setCreatedAt(LocalDateTime.now());
            
            // Convert old and new values to JSON strings
            if (oldValues != null) {
                auditLog.setOldValues(convertToJson(oldValues));
            }
            if (newValues != null) {
                auditLog.setNewValues(convertToJson(newValues));
            }
            
            auditLogRepository.save(auditLog);
            logger.debug("Audit log created: {} {} for user: {}", action, entityType, userId);
            
        } catch (Exception e) {
            logger.error("Error creating audit log: {}", e.getMessage(), e);
        }
    }
    
    public void logUserCreation(String userId, String createdBy, Object userDetails) {
        logAction(createdBy, "CREATE", "user", userId, null, userDetails, null, null);
    }
    
    public void logUserUpdate(String userId, String updatedBy, Object oldValues, Object newValues) {
        logAction(updatedBy, "UPDATE", "user", userId, oldValues, newValues, null, null);
    }
    
    public void logUserDeletion(String userId, String deletedBy, Object userDetails) {
        logAction(deletedBy, "DELETE", "user", userId, userDetails, null, null, null);
    }
    
    public void logPatientAccess(String userId, String patientId, String action) {
        logAction(userId, action, "patient", patientId, null, null, null, null);
    }
    
    public void logSystemConfigChange(String userId, String configKey, Object oldValue, Object newValue) {
        logAction(userId, "UPDATE", "system_config", configKey, oldValue, newValue, null, null);
    }
    
    private String convertToJson(Object object) {
        try {
            if (object instanceof String) {
                return (String) object;
            }
            // In a real implementation, you would use a JSON library like Jackson
            // For now, we'll use toString() as a placeholder
            return object != null ? object.toString() : null;
        } catch (Exception e) {
            logger.error("Error converting object to JSON: {}", e.getMessage(), e);
            return null;
        }
    }
}
