package com.meditrack.patient.repository;

import com.meditrack.patient.entity.MedicalHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MedicalHistoryRepository extends JpaRepository<MedicalHistory, Long> {
    
    // Basic CRUD operations
    List<MedicalHistory> findByPatientId(Long patientId);
    List<MedicalHistory> findByPatientIdOrderByDiagnosisDateDesc(Long patientId);
    Page<MedicalHistory> findByPatientId(Long patientId, Pageable pageable);
    
    // Search operations
    @Query("SELECT mh FROM MedicalHistory mh WHERE " +
           "mh.patient.id = :patientId AND " +
           "(LOWER(mh.conditionName) LIKE LOWER(CONCAT('%', :conditionName, '%')) OR " +
           "LOWER(mh.doctorName) LIKE LOWER(CONCAT('%', :doctorName, '%')))")
    List<MedicalHistory> searchByPatientId(@Param("patientId") Long patientId,
                                              @Param("conditionName") String conditionName,
                                              @Param("doctorName") String doctorName);
    
    // Filter operations
    List<MedicalHistory> findByPatientIdAndStatus(Long patientId, String status);
    List<MedicalHistory> findByPatientIdAndSeverity(Long patientId, String severity);
    List<MedicalHistory> findByPatientIdAndDiagnosisDateBetween(Long patientId, LocalDate startDate, LocalDate endDate);
    
    // Date range operations
    @Query("SELECT mh FROM MedicalHistory mh WHERE mh.diagnosisDate BETWEEN :startDate AND :endDate")
    Page<MedicalHistory> findByDiagnosisDateRange(@Param("startDate") LocalDate startDate, 
                                                 @Param("endDate") LocalDate endDate, Pageable pageable);
    
    // Status-based queries
    List<MedicalHistory> findByStatus(String status);
    @Query("SELECT mh FROM MedicalHistory mh WHERE mh.status = :status AND mh.diagnosisDate < :cutoffDate")
    List<MedicalHistory> findByStatusAndDiagnosisDateBefore(@Param("status") String status, 
                                                          @Param("cutoffDate") LocalDate cutoffDate);
    
    // Severity-based queries
    List<MedicalHistory> findBySeverity(String severity);
    List<MedicalHistory> findBySeverityAndStatus(String severity, String status);
    
    // Doctor and hospital queries
    List<MedicalHistory> findByDoctorName(String doctorName);
    List<MedicalHistory> findByHospitalName(String hospitalName);
    List<MedicalHistory> findByPatientIdAndDoctorName(Long patientId, String doctorName);
    
    // ICD code queries
    List<MedicalHistory> findByDiagnosisCode(String diagnosisCode);
    List<MedicalHistory> findByPatientIdAndDiagnosisCode(Long patientId, String diagnosisCode);
    
    // Complex queries for reporting
    @Query("SELECT mh FROM MedicalHistory mh WHERE " +
           "(:patientId IS NULL OR mh.patient.id = :patientId) AND " +
           "(:status IS NULL OR mh.status = :status) AND " +
           "(:severity IS NULL OR mh.severity = :severity) AND " +
           "(:doctorName IS NULL OR LOWER(mh.doctorName) LIKE LOWER(CONCAT('%', :doctorName, '%'))) AND " +
           "(:hospitalName IS NULL OR LOWER(mh.hospitalName) LIKE LOWER(CONCAT('%', :hospitalName, '%')))")
    Page<MedicalHistory> advancedSearch(
        @Param("patientId") Long patientId,
        @Param("status") String status,
        @Param("severity") String severity,
        @Param("doctorName") String doctorName,
        @Param("hospitalName") String hospitalName,
        Pageable pageable
    );
    
    // Count operations for reporting
    long countByPatientId(Long patientId);
    long countByStatus(String status);
    long countBySeverity(String severity);
    long countByDiagnosisDateBetween(LocalDate startDate, LocalDate endDate);
    
    // Recent conditions
    @Query("SELECT mh FROM MedicalHistory mh WHERE mh.patient.id = :patientId AND mh.diagnosisDate >= :cutoffDate ORDER BY mh.diagnosisDate DESC")
    List<MedicalHistory> findRecentConditions(@Param("patientId") Long patientId, @Param("cutoffDate") LocalDate cutoffDate);
    
    // Chronic conditions
    @Query("SELECT mh FROM MedicalHistory mh WHERE mh.patient.id = :patientId AND mh.status = 'CHRONIC'")
    List<MedicalHistory> findChronicConditions(@Param("patientId") Long patientId);
    
    // Active conditions
    @Query("SELECT mh FROM MedicalHistory mh WHERE mh.patient.id = :patientId AND mh.status = 'ACTIVE'")
    List<MedicalHistory> findActiveConditions(@Param("patientId") Long patientId);
}
