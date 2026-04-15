package com.meditrack.vitals.repository;

import com.meditrack.vitals.entity.VitalReading;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VitalReadingRepository extends JpaRepository<VitalReading, Long> {
    
    // Basic CRUD operations
    List<VitalReading> findByPatientId(Long patientId);
    List<VitalReading> findByPatientIdOrderByReadingTimestampDesc(Long patientId);
    Page<VitalReading> findByPatientId(Long patientId, Pageable pageable);
    
    // Vital type queries
    List<VitalReading> findByPatientIdAndVitalType(Long patientId, String vitalType);
    List<VitalReading> findByPatientIdAndVitalTypeOrderByReadingTimestampDesc(Long patientId, String vitalType);
    Page<VitalReading> findByPatientIdAndVitalType(Long patientId, String vitalType, Pageable pageable);
    
    // Time range queries
    @Query("SELECT vr FROM VitalReading vr WHERE " +
           "vr.patient.id = :patientId AND " +
           "vr.readingTimestamp BETWEEN :startTime AND :endTime")
    List<VitalReading> findByPatientIdAndTimeRange(@Param("patientId") Long patientId, 
                                                   @Param("startTime") LocalDateTime startTime, 
                                                   @Param("endTime") LocalDateTime endTime);
    
    @Query("SELECT vr FROM VitalReading vr WHERE " +
           "vr.patient.id = :patientId AND " +
           "vr.vitalType = :vitalType AND " +
           "vr.readingTimestamp BETWEEN :startTime AND :endTime")
    List<VitalReading> findByPatientIdAndVitalTypeAndTimeRange(@Param("patientId") Long patientId, 
                                                             @Param("vitalType") String vitalType,
                                                             @Param("startTime") LocalDateTime startTime, 
                                                             @Param("endTime") LocalDateTime endTime);
    
    // Latest readings
    @Query("SELECT vr FROM VitalReading vr WHERE " +
           "vr.patient.id = :patientId AND " +
           "vr.vitalType = :vitalType AND " +
           "vr.readingTimestamp = (SELECT MAX(vr2.readingTimestamp) FROM VitalReading vr2 WHERE " +
           "vr2.patient.id = :patientId AND vr2.vitalType = :vitalType)")
    VitalReading findLatestByPatientIdAndVitalType(@Param("patientId") Long patientId, @Param("vitalType") String vitalType);
    
    @Query("SELECT vr FROM VitalReading vr WHERE " +
           "vr.patient.id = :patientId AND " +
           "vr.readingTimestamp >= :cutoffDate ORDER BY vr.readingTimestamp DESC")
    List<VitalReading> findRecentByPatientId(@Param("patientId") Long patientId, @Param("cutoffDate") LocalDateTime cutoffDate);
    
    // Quality-based queries
    List<VitalReading> findByPatientIdAndQualityScoreGreaterThanEqual(Long patientId, BigDecimal qualityScore);
    List<VitalReading> findByPatientIdAndVitalTypeAndQualityScoreGreaterThanEqual(Long patientId, String vitalType, BigDecimal qualityScore);
    
    // Device-based queries
    List<VitalReading> findByPatientIdAndDeviceId(Long patientId, String deviceId);
    List<VitalReading> findByPatientIdAndSource(Long patientId, String source);
    List<VitalReading> findByDeviceId(String deviceId);
    
    // Location-based queries
    List<VitalReading> findByPatientIdAndLocation(Long patientId, String location);
    List<VitalReading> findByLocation(String location);
    
    // Status-based queries (using vital status logic)
    @Query("SELECT vr FROM VitalReading vr WHERE " +
           "vr.patient.id = :patientId AND " +
           "vr.vitalType = :vitalType AND " +
           "CASE " +
           "  WHEN vr.vitalType = 'HEART_RATE' THEN " +
           "    CASE " +
           "      WHEN vr.value < 60 THEN 'LOW' " +
           "      WHEN vr.value > 100 THEN 'HIGH' " +
           "      ELSE 'NORMAL' " +
           "    END " +
           "  WHEN vr.vitalType = 'TEMPERATURE' THEN " +
           "    CASE " +
           "      WHEN vr.value < 36.0 THEN 'LOW' " +
           "      WHEN vr.value > 37.5 THEN 'HIGH' " +
           "      ELSE 'NORMAL' " +
           "    END " +
           "  WHEN vr.vitalType = 'SPO2' THEN " +
           "    CASE " +
           "      WHEN vr.value < 95 THEN 'LOW' " +
           "      ELSE 'NORMAL' " +
           "    END " +
           "  WHEN vr.vitalType = 'RESPIRATORY_RATE' THEN " +
           "    CASE " +
           "      WHEN vr.value < 12 THEN 'LOW' " +
           "      WHEN vr.value > 20 THEN 'HIGH' " +
           "      ELSE 'NORMAL' " +
           "    END " +
           "  ELSE 'UNKNOWN' " +
           "END = :status")
    List<VitalReading> findByPatientIdAndVitalTypeAndStatus(@Param("patientId") Long patientId, 
                                                           @Param("vitalType") String vitalType,
                                                           @Param("status") String status);
    
    // Abnormal readings
    @Query("SELECT vr FROM VitalReading vr WHERE " +
           "vr.patient.id = :patientId AND " +
           "vr.readingTimestamp >= :cutoffDate AND " +
           "( " +
           "  (vr.vitalType = 'HEART_RATE' AND (vr.value < 60 OR vr.value > 100)) OR " +
           "  (vr.vitalType = 'TEMPERATURE' AND (vr.value < 36.0 OR vr.value > 37.5)) OR " +
           "  (vr.vitalType = 'SPO2' AND vr.value < 95) OR " +
           "  (vr.vitalType = 'RESPIRATORY_RATE' AND (vr.value < 12 OR vr.value > 20)) OR " +
           "  (vr.vitalType = 'BLOOD_PRESSURE' AND " +
           "   (vr.systolic < 90 OR vr.systolic > 140 OR vr.diastolic < 60 OR vr.diastolic > 90)) " +
           ")")
    List<VitalReading> findAbnormalReadingsByPatientId(@Param("patientId") Long patientId, @Param("cutoffDate") LocalDateTime cutoffDate);
    
    // Critical readings
    @Query("SELECT vr FROM VitalReading vr WHERE " +
           "vr.patient.id = :patientId AND " +
           "vr.readingTimestamp >= :cutoffDate AND " +
           "( " +
           "  (vr.vitalType = 'HEART_RATE' AND (vr.value < 40 OR vr.value > 180)) OR " +
           "  (vr.vitalType = 'TEMPERATURE' AND (vr.value < 34.0 OR vr.value > 41.0)) OR " +
           "  (vr.vitalType = 'SPO2' AND vr.value < 85) OR " +
           "  (vr.vitalType = 'RESPIRATORY_RATE' AND (vr.value < 8 OR vr.value > 35)) OR " +
           "  (vr.vitalType = 'BLOOD_PRESSURE' AND " +
           "   (vr.systolic < 70 OR vr.systolic > 200 OR vr.diastolic < 40 OR vr.diastolic > 120)) " +
           ")")
    List<VitalReading> findCriticalReadingsByPatientId(@Param("patientId") Long patientId, @Param("cutoffDate") LocalDateTime cutoffDate);
    
    // Trend analysis queries
    @Query("SELECT vr FROM VitalReading vr WHERE " +
           "vr.patient.id = :patientId AND " +
           "vr.vitalType = :vitalType AND " +
           "vr.readingTimestamp >= :startTime AND " +
           "vr.readingTimestamp <= :endTime " +
           "ORDER BY vr.readingTimestamp")
    List<VitalReading> findForTrendAnalysis(@Param("patientId") Long patientId, 
                                          @Param("vitalType") String vitalType,
                                          @Param("startTime") LocalDateTime startTime, 
                                          @Param("endTime") LocalDateTime endTime);
    
    // Count operations for reporting
    long countByPatientId(Long patientId);
    long countByPatientIdAndVitalType(Long patientId, String vitalType);
    long countByPatientIdAndTimeRange(Long patientId, LocalDateTime startTime, LocalDateTime endTime);
    
    // Device statistics
    @Query("SELECT vr.deviceId, COUNT(vr) FROM VitalReading vr WHERE " +
           "vr.patient.id = :patientId GROUP BY vr.deviceId")
    List<Object[]> getDeviceUsageStats(@Param("patientId") Long patientId);
    
    // Quality statistics
    @Query("SELECT vr.vitalType, AVG(vr.qualityScore), COUNT(vr) FROM VitalReading vr WHERE " +
           "vr.patient.id = :patientId AND vr.qualityScore IS NOT NULL GROUP BY vr.vitalType")
    List<Object[]> getQualityStatsByPatientId(@Param("patientId") Long patientId);
    
    // Batch operations
    @Query("SELECT vr FROM VitalReading vr WHERE vr.readingTimestamp < :cutoffDate")
    List<VitalReading> findOldReadings(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    void deleteByPatientId(Long patientId);
    void deleteByReadingTimestampBefore(LocalDateTime cutoffDate);
    
    // Complex search for advanced filtering
    @Query("SELECT vr FROM VitalReading vr WHERE " +
           "(:patientId IS NULL OR vr.patient.id = :patientId) AND " +
           "(:vitalType IS NULL OR vr.vitalType = :vitalType) AND " +
           "(:source IS NULL OR vr.source = :source) AND " +
           "(:deviceId IS NULL OR vr.deviceId = :deviceId) AND " +
           "(:location IS NULL OR vr.location = :location) AND " +
           "(:startTime IS NULL OR vr.readingTimestamp >= :startTime) AND " +
           "(:endTime IS NULL OR vr.readingTimestamp <= :endTime) AND " +
           "(:minQuality IS NULL OR vr.qualityScore >= :minQuality)")
    Page<VitalReading> advancedSearch(
        @Param("patientId") Long patientId,
        @Param("vitalType") String vitalType,
        @Param("source") String source,
        @Param("deviceId") String deviceId,
        @Param("location") String location,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime,
        @Param("minQuality") BigDecimal minQuality,
        Pageable pageable
    );
}
