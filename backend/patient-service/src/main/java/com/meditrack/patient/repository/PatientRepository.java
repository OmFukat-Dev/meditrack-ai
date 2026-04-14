package com.meditrack.patient.repository;

import com.meditrack.patient.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
    
    // Basic CRUD operations
    Optional<Patient> findByPatientIdentifier(String patientIdentifier);
    List<Patient> findByPatientIdentifierIn(List<String> patientIdentifiers);
    boolean existsByPatientIdentifier(String patientIdentifier);
    
    // Search operations
    Page<Patient> findByFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCase(
        String firstName, String lastName, Pageable pageable);
    
    Page<Patient> findByFirstNameContainingIgnoreCase(String firstName, Pageable pageable);
    Page<Patient> findByLastNameContainingIgnoreCase(String lastName, Pageable pageable);
    
    @Query("SELECT p FROM Patient p WHERE " +
           "LOWER(p.firstName) LIKE LOWER(CONCAT('%', :name, '%')) OR " +
           "LOWER(p.lastName) LIKE LOWER(CONCAT('%', :name, '%')) OR " +
           "LOWER(p.patientIdentifier) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Patient> searchByNameOrIdentifier(@Param("name") String name, Pageable pageable);
    
    // Filter operations
    Page<Patient> findByGenderAndIsActive(String gender, Boolean isActive, Pageable pageable);
    Page<Patient> findByBloodTypeAndIsActive(String bloodType, Boolean isActive, Pageable pageable);
    Page<Patient> findByIsActive(Boolean isActive, Pageable pageable);
    
    // Date range operations
    @Query("SELECT p FROM Patient p WHERE p.dateOfBirth BETWEEN :startDate AND :endDate")
    Page<Patient> findByDateOfBirthRange(@Param("startDate") LocalDate startDate, 
                                       @Param("endDate") LocalDate endDate, Pageable pageable);
    
    // Age-based queries
    @Query("SELECT p FROM Patient p WHERE FUNCTION('YEAR', CURRENT_DATE) - FUNCTION('YEAR', p.dateOfBirth) = :age")
    List<Patient> findByAge(@Param("age") int age);
    
    @Query("SELECT p FROM Patient p WHERE FUNCTION('YEAR', CURRENT_DATE) - FUNCTION('YEAR', p.dateOfBirth) BETWEEN :minAge AND :maxAge")
    List<Patient> findByAgeRange(@Param("minAge") int minAge, @Param("maxAge") int maxAge);
    
    // Contact information queries
    List<Patient> findByEmailAndIsActive(String email, Boolean isActive);
    List<Patient> findByPhoneNumberAndIsActive(String phoneNumber, Boolean isActive);
    
    // Emergency contact queries
    List<Patient> findByEmergencyContactNameContainingIgnoreCase(String emergencyContactName);
    List<Patient> findByEmergencyContactPhone(String emergencyContactPhone);
    
    // Audit and tracking queries
    List<Patient> findByCreatedByAndCreatedAtBetween(String createdBy, java.time.LocalDateTime startDate, 
                                                          java.time.LocalDateTime endDate);
    
    // Count operations for reporting
    long countByGender(String gender);
    long countByBloodType(String bloodType);
    long countByAgeGreaterThan(int age);
    long countByIsActive(Boolean isActive);
    
    // Complex search for advanced filtering
    @Query("SELECT p FROM Patient p WHERE " +
           "(:firstName IS NULL OR LOWER(p.firstName) LIKE LOWER(CONCAT('%', :firstName, '%'))) AND " +
           "(:lastName IS NULL OR LOWER(p.lastName) LIKE LOWER(CONCAT('%', :lastName, '%'))) AND " +
           "(:gender IS NULL OR p.gender = :gender) AND " +
           "(:bloodType IS NULL OR p.bloodType = :bloodType) AND " +
           "(:isActive IS NULL OR p.isActive = :isActive) AND " +
           "(:minAge IS NULL OR FUNCTION('YEAR', CURRENT_DATE) - FUNCTION('YEAR', p.dateOfBirth) >= :minAge) AND " +
           "(:maxAge IS NULL OR FUNCTION('YEAR', CURRENT_DATE) - FUNCTION('YEAR', p.dateOfBirth) <= :maxAge)")
    Page<Patient> advancedSearch(
        @Param("firstName") String firstName,
        @Param("lastName") String lastName,
        @Param("gender") String gender,
        @Param("bloodType") String bloodType,
        @Param("isActive") Boolean isActive,
        @Param("minAge") Integer minAge,
        @Param("maxAge") Integer maxAge,
        Pageable pageable
    );
    
    // FHIR related queries
    @Query("SELECT DISTINCT p FROM Patient p JOIN p.fhirResources f WHERE f.resourceType = :resourceType")
    List<Patient> findByFhirResourceType(@Param("resourceType") String resourceType);
}
