package com.meditrack.patient.service;

import com.meditrack.patient.entity.AuditLog;
import com.meditrack.patient.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AuditService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    public AuditLog logAction(Long patientId, String action, String details, String assignedBy) {
        AuditLog log = new AuditLog(patientId, action, details, assignedBy);
        return auditLogRepository.save(log);
    }

    public List<AuditLog> getLogsForPatient(Long patientId) {
        return auditLogRepository.findByPatientIdOrderByTimestampDesc(patientId);
    }
}
