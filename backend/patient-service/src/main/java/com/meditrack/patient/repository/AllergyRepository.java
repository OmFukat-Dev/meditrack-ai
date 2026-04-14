package com.meditrack.patient.repository;

import com.meditrack.patient.entity.Allergy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AllergyRepository extends JpaRepository<Allergy, Long> {
    
    // Basic CRUD operations
    List<Allergy> findByPatientId(Long patientId);
    List<Allergy> findByPatientIdOrderByAllergen(Long patientId);
    
    // Search operations
    @Query("SELECT a FROM Allergy a WHERE " +
           "a.patient.id = :patientId AND " +
           "(LOWER(a.allergen) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(a.reaction) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<Allergy> searchByPatientId(@Param("patientId") Long patientId, @Param("searchTerm") String searchTerm);
    
    // Filter operations
    List<Allergy> findByPatientIdAndAllergyType(Long patientId, String allergyType);
    List<Allergy> findByPatientIdAndSeverity(Long patientId, String severity);
    List<Allergy> findByPatientIdAndAllergyTypeAndSeverity(Long patientId, String allergyType, String severity);
    
    // Allergen-specific queries
    List<Allergy> findByAllergenContainingIgnoreCase(String allergen);
    List<Allergy> findByPatientIdAndAllergenContainingIgnoreCase(Long patientId, String allergen);
    
    // Severity-based queries
    List<Allergy> findBySeverity(String severity);
    List<Allergy> findByPatientIdAndSeverity(Long patientId, String severity);
    List<Allergy> findByPatientIdAndIsSevere(Long patientId); // Assuming isSevere() method
    
    // Allergy type queries
    List<Allergy> findByAllergyType(String allergyType);
    List<Allergy> findByPatientIdAndAllergyType(Long patientId, String allergyType);
    
    // Complex search for advanced filtering
    @Query("SELECT a FROM Allergy a WHERE " +
           "(:patientId IS NULL OR a.patient.id = :patientId) AND " +
           "(:allergyType IS NULL OR a.allergyType = :allergyType) AND " +
           "(:severity IS NULL OR a.severity = :severity) AND " +
           "(:searchTerm IS NULL OR LOWER(a.allergen) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(a.reaction) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<Allergy> advancedSearch(
        @Param("patientId") Long patientId,
        @Param("allergyType") String allergyType,
        @Param("severity") String severity,
        @Param("searchTerm") String searchTerm
    );
    
    // Count operations for reporting
    long countByPatientId(Long patientId);
    long countByAllergyType(String allergyType);
    long countBySeverity(String severity);
    long countByPatientIdAndAllergyType(Long patientId, String allergyType);
    long countByPatientIdAndSeverity(Long patientId, String severity);
    
    // Existence checks
    boolean existsByPatientIdAndAllergen(Long patientId, String allergen);
    boolean existsByPatientIdAndAllergyType(Long patientId, String allergyType);
    
    // Severe allergies (business logic)
    @Query("SELECT a FROM Allergy a WHERE a.patient.id = :patientId AND a.severity = 'SEVERE'")
    List<Allergy> findSevereAllergiesByPatientId(@Param("patientId") Long patientId);
    
    // Food allergies
    @Query("SELECT a FROM Allergy a WHERE a.patient.id = :patientId AND a.allergyType = 'FOOD'")
    List<Allergy> findFoodAllergiesByPatientId(@Param("patientId") Long patientId);
    
    // Medication allergies
    @Query("SELECT a FROM Allergy a WHERE a.patient.id = :patientId AND a.allergyType = 'MEDICATION'")
    List<Allergy> findMedicationAllergiesByPatientId(@Param("patientId") Long patientId);
    
    // Environmental allergies
    @Query("SELECT a FROM Allergy a WHERE a.patient.id = :patientId AND a.allergyType = 'ENVIRONMENTAL'")
    List<Allergy> findEnvironmentalAllergiesByPatientId(@Param("patientId") Long patientId);
}
