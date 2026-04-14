package com.meditrack.patient.repository;

import com.meditrack.patient.entity.FhirResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FhirResourceRepository extends JpaRepository<FhirResource, Long> {
    
    // Basic CRUD operations
    List<FhirResource> findByPatientId(Long patientId);
    List<FhirResource> findByPatientIdOrderByCreatedAtDesc(Long patientId);
    
    // Resource type queries
    List<FhirResource> findByPatientIdAndResourceType(Long patientId, FhirResource.FhirResourceType resourceType);
    List<FhirResource> findByResourceType(FhirResource.FhirResourceType resourceType);
    
    // Resource ID queries
    List<FhirResource> findByResourceId(String resourceId);
    List<FhirResource> findByPatientIdAndResourceId(Long patientId, String resourceId);
    
    // Version queries
    List<FhirResource> findByPatientIdAndResourceIdAndResourceVersion(Long patientId, String resourceId, String resourceVersion);
    List<FhirResource> findByResourceIdOrderByResourceVersionDesc(String resourceId);
    
    // Latest version queries
    @Query("SELECT fr FROM FhirResource fr WHERE " +
           "fr.patient.id = :patientId AND " +
           "fr.resourceType = :resourceType AND " +
           "fr.createdAt = (SELECT MAX(fr2.createdAt) FROM FhirResource fr2 WHERE " +
           "fr2.patient.id = fr.patient.id AND " +
           "fr2.resourceType = fr.resourceType AND " +
           "fr2.resourceId = fr.resourceId)")
    List<FhirResource> findLatestVersionByPatientIdAndResourceTypeAndResourceId(
        @Param("patientId") Long patientId,
        @Param("resourceType") FhirResource.FhirResourceType resourceType,
        @Param("resourceId") String resourceId
    );
    
    // Date range queries
    @Query("SELECT fr FROM FhirResource fr WHERE " +
           "fr.patient.id = :patientId AND " +
           "fr.createdAt BETWEEN :startDate AND :endDate")
    List<FhirResource> findByPatientIdAndCreatedAtBetween(
        @Param("patientId") Long patientId,
        @Param("startDate") java.time.LocalDateTime startDate,
        @Param("endDate") java.time.LocalDateTime endDate
    );
    
    // Complex search for advanced filtering
    @Query("SELECT fr FROM FhirResource fr WHERE " +
           "(:patientId IS NULL OR fr.patient.id = :patientId) AND " +
           "(:resourceType IS NULL OR fr.resourceType = :resourceType) AND " +
           "(:resourceId IS NULL OR fr.resourceId LIKE CONCAT('%', :resourceId, '%')) AND " +
           "(:resourceVersion IS NULL OR fr.resourceVersion = :resourceVersion)")
    List<FhirResource> advancedSearch(
        @Param("patientId") Long patientId,
        @Param("resourceType") FhirResource.FhirResourceType resourceType,
        @Param("resourceId") String resourceId,
        @Param("resourceVersion") String resourceVersion
    );
    
    // Count operations for reporting
    long countByPatientId(Long patientId);
    long countByPatientIdAndResourceType(Long patientId, FhirResource.FhirResourceType resourceType);
    long countByResourceType(FhirResource.FhirResourceType resourceType);
    
    // Existence checks
    boolean existsByPatientIdAndResourceIdAndResourceVersion(Long patientId, String resourceId, String resourceVersion);
    boolean existsByResourceId(String resourceId);
    
    // Resource type statistics
    @Query("SELECT fr.resourceType, COUNT(fr) FROM FhirResource fr GROUP BY fr.resourceType")
    List<Object[]> getResourceTypeStatistics();
    
    @Query("SELECT fr.resourceType, COUNT(fr) FROM FhirResource fr WHERE fr.patient.id = :patientId GROUP BY fr.resourceType")
    List<Object[]> getResourceTypeStatisticsByPatientId(@Param("patientId") Long patientId);
    
    // Recent resources
    @Query("SELECT fr FROM FhirResource fr WHERE " +
           "fr.patient.id = :patientId AND " +
           "fr.createdAt >= :cutoffDate ORDER BY fr.createdAt DESC")
    List<FhirResource> findRecentResourcesByPatientId(
        @Param("patientId") Long patientId, 
        @Param("cutoffDate") java.time.LocalDateTime cutoffDate
    );
    
    // Delete operations for cleanup
    void deleteByPatientId(Long patientId);
    void deleteByPatientIdAndResourceType(Long patientId, FhirResource.FhirResourceType resourceType);
    void deleteByPatientIdAndCreatedAtBefore(java.time.LocalDateTime cutoffDate);
}
