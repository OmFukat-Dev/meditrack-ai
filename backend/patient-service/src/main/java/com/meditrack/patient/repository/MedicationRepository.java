package com.meditrack.patient.repository;

import com.meditrack.patient.entity.Medication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MedicationRepository extends JpaRepository<Medication, Long> {
    
    // Basic CRUD operations
    List<Medication> findByPatientId(Long patientId);
    List<Medication> findByPatientIdOrderByStartDateDesc(Long patientId);
    Page<Medication> findByPatientId(Long patientId, Pageable pageable);
    
    // Search operations
    @Query("SELECT m FROM Medication m WHERE " +
           "m.patient.id = :patientId AND " +
           "(LOWER(m.medicationName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(m.prescribedBy) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(m.purpose) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<Medication> searchByPatientId(@Param("patientId") Long patientId, @Param("searchTerm") String searchTerm);
    
    // Filter operations
    List<Medication> findByPatientIdAndIsActive(Long patientId, Boolean isActive);
    List<Medication> findByPatientIdAndRoute(Long patientId, String route);
    List<Medication> findByPatientIdAndRouteAndIsActive(Long patientId, String route, Boolean isActive);
    
    // Date range operations
    @Query("SELECT m FROM Medication m WHERE " +
           "m.patient.id = :patientId AND " +
           "m.startDate BETWEEN :startDate AND :endDate")
    List<Medication> findByPatientIdAndStartDateBetween(@Param("patientId") Long patientId, 
                                                     @Param("startDate") LocalDate startDate, 
                                                     @Param("endDate") LocalDate endDate);
    
    // Current medications (active and not ended)
    @Query("SELECT m FROM Medication m WHERE " +
           "m.patient.id = :patientId AND " +
           "m.isActive = true AND " +
           "(m.endDate IS NULL OR m.endDate >= CURRENT_DATE)")
    List<Medication> findCurrentMedicationsByPatientId(@Param("patientId") Long patientId);
    
    // Expired medications
    @Query("SELECT m FROM Medication m WHERE " +
           "m.patient.id = :patientId AND " +
           "m.endDate < CURRENT_DATE")
    List<Medication> findExpiredMedicationsByPatientId(@Param("patientId") Long patientId);
    
    // Medications ending soon
    @Query("SELECT m FROM Medication m WHERE " +
           "m.patient.id = :patientId AND " +
           "m.endDate BETWEEN CURRENT_DATE AND :endDate")
    List<Medication> findMedicationsEndingSoonByPatientId(@Param("patientId") Long patientId, @Param("endDate") LocalDate endDate);
    
    // Route-specific queries
    List<Medication> findByRoute(String route);
    List<Medication> findByPatientIdAndRoute(Long patientId, String route);
    
    // Prescriber queries
    List<Medication> findByPrescribedBy(String prescribedBy);
    List<Medication> findByPatientIdAndPrescribedBy(Long patientId, String prescribedBy);
    
    // Medication name queries
    List<Medication> findByMedicationNameContainingIgnoreCase(String medicationName);
    List<Medication> findByPatientIdAndMedicationNameContainingIgnoreCase(Long patientId, String medicationName);
    
    // Complex search for advanced filtering
    @Query("SELECT m FROM Medication m WHERE " +
           "(:patientId IS NULL OR m.patient.id = :patientId) AND " +
           "(:isActive IS NULL OR m.isActive = :isActive) AND " +
           "(:route IS NULL OR m.route = :route) AND " +
           "(:prescribedBy IS NULL OR LOWER(m.prescribedBy) LIKE LOWER(CONCAT('%', :prescribedBy, '%')))")
    Page<Medication> advancedSearch(
        @Param("patientId") Long patientId,
        @Param("isActive") Boolean isActive,
        @Param("route") String route,
        @Param("prescribedBy") String prescribedBy,
        Pageable pageable
    );
    
    // Count operations for reporting
    long countByPatientId(Long patientId);
    long countByPatientIdAndIsActive(Long patientId, Boolean isActive);
    long countByRoute(String route);
    long countByPrescribedBy(String prescribedBy);
    
    // Duration and usage statistics
    @Query("SELECT COUNT(m) FROM Medication m WHERE m.patient.id = :patientId AND m.isActive = true")
    long countActiveMedicationsByPatientId(@Param("patientId") Long patientId);
    
    @Query("SELECT COUNT(m) FROM Medication m WHERE m.patient.id = :patientId AND m.endDate < CURRENT_DATE")
    long countExpiredMedicationsByPatientId(@Param("patientId") Long patientId);
    
    // Medication interactions (basic implementation)
    @Query("SELECT DISTINCT m1 FROM Medication m1 " +
           "JOIN m1.patient p " +
           "JOIN Medication m2 ON m1.patient.id = m2.patient.id " +
           "WHERE p.id = :patientId AND " +
           "m1.medicationName != m2.medicationName AND " +
           "m1.isActive = true AND m2.isActive = true")
    List<Medication> findPotentialInteractionsByPatientId(@Param("patientId") Long patientId);
    
    // Most recent medications
    @Query("SELECT m FROM Medication m WHERE m.patient.id = :patientId ORDER BY m.startDate DESC")
    List<Medication> findMostRecentMedicationsByPatientId(@Param("patientId") Long patientId);
    
    // Medications by purpose
    @Query("SELECT m FROM Medication m WHERE " +
           "m.patient.id = :patientId AND " +
           "LOWER(m.purpose) LIKE LOWER(CONCAT('%', :purpose, '%'))")
    List<Medication> findByPatientIdAndPurposeContainingIgnoreCase(@Param("patientId") Long patientId, @Param("purpose") String purpose);
}
