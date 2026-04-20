package com.meditrack.alert.repository;

import com.meditrack.alert.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, String> {
    
    @Query("SELECT a FROM Alert a WHERE a.createdAt >= :startTime ORDER BY a.createdAt DESC")
    List<Alert> findRecentAlerts(@Param("startTime") LocalDateTime startTime);
    
    @Query("SELECT a FROM Alert a WHERE a.patientId = :patientId ORDER BY a.createdAt DESC")
    List<Alert> findByPatientIdOrderByCreatedAtDesc(@Param("patientId") String patientId);
    
    @Query("SELECT a FROM Alert a WHERE a.alertType = :alertType ORDER BY a.createdAt DESC")
    List<Alert> findByAlertTypeOrderByCreatedAtDesc(@Param("alertType") String alertType);
    
    @Query("SELECT a FROM Alert a WHERE a.priority = :priority ORDER BY a.createdAt DESC")
    List<Alert> findByPriorityOrderByCreatedAtDesc(@Param("priority") Alert.AlertPriority priority);
    
    @Query("SELECT a FROM Alert a WHERE a.status = :status ORDER BY a.createdAt DESC")
    List<Alert> findByStatusOrderByCreatedAtDesc(@Param("status") Alert.AlertStatus status);
    
    @Query("SELECT a FROM Alert a WHERE a.createdAt BETWEEN :startTime AND :endTime ORDER BY a.createdAt DESC")
    List<Alert> findByCreatedAtBetweenOrderByCreatedAtDesc(
        @Param("startTime") LocalDateTime startTime, 
        @Param("endTime") LocalDateTime endTime);
    
    @Query("SELECT a FROM Alert a WHERE a.escalationLevel IS NOT NULL ORDER BY a.escalatedAt DESC")
    List<Alert> findEscalatedAlertsOrderByEscalatedAtDesc();
    
    @Query("SELECT a FROM Alert a WHERE a.status = 'PENDING' ORDER BY a.createdAt ASC")
    List<Alert> findPendingAlertsOrderByCreatedAtAsc();
    
    @Query("SELECT COUNT(a) FROM Alert a WHERE a.createdAt >= :startTime")
    long countAlertsSince(@Param("startTime") LocalDateTime startTime);
    
    @Query("SELECT COUNT(a) FROM Alert a WHERE a.priority = :priority AND a.createdAt >= :startTime")
    long countAlertsByPrioritySince(@Param("priority") Alert.AlertPriority priority, @Param("startTime") LocalDateTime startTime);
}
