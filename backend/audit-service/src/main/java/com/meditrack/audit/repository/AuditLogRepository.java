package com.meditrack.audit.repository;

import com.meditrack.audit.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, String> {
    
    List<AuditLog> findByUserId(String userId, org.springframework.data.domain.Pageable pageable);
    
    List<AuditLog> findByPatientId(String patientId, org.springframework.data.domain.Pageable pageable);
    
    List<AuditLog> findByEntityType(String entityType, org.springframework.data.domain.Pageable pageable);
    
    List<AuditLog> findByAction(String action, org.springframework.data.domain.Pageable pageable);
    
    List<AuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
    
    List<AuditLog> findByTimestampAfter(LocalDateTime timestamp);
    
    @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId AND a.timestamp BETWEEN :start AND :end")
    List<AuditLog> findByUserIdAndDateRange(
        @Param("userId") String userId,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );
    
    @Query("SELECT a FROM AuditLog a WHERE a.patientId = :patientId AND a.timestamp BETWEEN :start AND :end")
    List<AuditLog> findByPatientIdAndDateRange(
        @Param("patientId") String patientId,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );
    
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.action = :action")
    long countByAction(@Param("action") String action);
    
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.entityType = :entityType")
    long countByEntityType(@Param("entityType") String entityType);
}
