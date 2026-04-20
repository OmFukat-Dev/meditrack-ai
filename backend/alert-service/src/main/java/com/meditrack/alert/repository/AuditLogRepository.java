package com.meditrack.alert.repository;

import com.meditrack.alert.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, String> {
    
    @Query("SELECT a FROM AuditLog a WHERE a.timestamp BETWEEN :startTime AND :endTime ORDER BY a.timestamp DESC")
    List<AuditLog> findByTimestampBetweenOrderByTimestampDesc(
        @Param("startTime") LocalDateTime startTime, 
        @Param("endTime") LocalDateTime endTime);
    
    @Query("SELECT a FROM AuditLog a WHERE a.entityType = :entityType AND a.entityId = :entityId ORDER BY a.timestamp DESC")
    List<AuditLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(
        @Param("entityType") String entityType, 
        @Param("entityId") String entityId);
    
    @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId ORDER BY a.timestamp DESC")
    List<AuditLog> findByUserIdOrderByTimestampDesc(@Param("userId") String userId);
    
    @Query("SELECT a FROM AuditLog a WHERE a.eventType = :eventType ORDER BY a.timestamp DESC")
    List<AuditLog> findByEventTypeOrderByTimestampDesc(@Param("eventType") AuditLog.AuditEventType eventType);
    
    @Query("SELECT a FROM AuditLog a WHERE a.severity = :severity ORDER BY a.timestamp DESC")
    List<AuditLog> findBySeverityOrderByTimestampDesc(@Param("severity") String severity);
    
    @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId AND a.timestamp >= :startTime ORDER BY a.timestamp DESC")
    List<AuditLog> findByUserIdAndTimestampAfterOrderByTimestampDesc(
        @Param("userId") String userId, 
        @Param("startTime") LocalDateTime startTime);
    
    @Query("SELECT a FROM AuditLog a WHERE a.entityType = :entityType AND a.timestamp >= :startTime ORDER BY a.timestamp DESC")
    List<AuditLog> findByEntityTypeAndTimestampAfterOrderByTimestampDesc(
        @Param("entityType") String entityType, 
        @Param("startTime") LocalDateTime startTime);
    
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.timestamp >= :startTime")
    long countAuditLogsSince(@Param("startTime") LocalDateTime startTime);
    
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.eventType = :eventType AND a.timestamp >= :startTime")
    long countAuditLogsByEventTypeSince(@Param("eventType") AuditLog.AuditEventType eventType, @Param("startTime") LocalDateTime startTime);
    
    @Query("SELECT a FROM AuditLog a ORDER BY a.timestamp DESC")
    List<AuditLog> findTop1000ByOrderByTimestampDesc();
}
