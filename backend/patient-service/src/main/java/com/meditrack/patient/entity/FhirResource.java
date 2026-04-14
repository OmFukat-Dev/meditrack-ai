package com.meditrack.patient.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "fhir_resources")
@EntityListeners(AuditingEntityListener.class)
public class FhirResource {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    
    @Column(name = "resource_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private FhirResourceType resourceType;
    
    @Column(name = "resource_id", nullable = false, length = 100)
    private String resourceId;
    
    @Column(name = "resource_version", length = 20)
    private String resourceVersion = "1";
    
    @Column(name = "resource_data", nullable = false, columnDefinition = "JSON")
    private String resourceData;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Constructors
    public FhirResource() {}
    
    public FhirResource(Patient patient, FhirResourceType resourceType, String resourceId, String resourceData) {
        this.patient = patient;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.resourceData = resourceData;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }
    
    public FhirResourceType getResourceType() { return resourceType; }
    public void setResourceType(FhirResourceType resourceType) { this.resourceType = resourceType; }
    
    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }
    
    public String getResourceVersion() { return resourceVersion; }
    public void setResourceVersion(String resourceVersion) { this.resourceVersion = resourceVersion; }
    
    public String getResourceData() { return resourceData; }
    public void setResourceData(String resourceData) { this.resourceData = resourceData; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    // Utility methods
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FhirResource that = (FhirResource) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "FhirResource{" +
               "id=" + id +
               ", resourceType=" + resourceType +
               ", resourceId='" + resourceId + '\'' +
               ", resourceVersion='" + resourceVersion + '\'' +
               ", createdAt=" + createdAt +
               '}';
    }
    
    // Business logic methods
    public String getFullResourceId() {
        return resourceId + "/" + resourceVersion;
    }
    
    public boolean isPatientResource() {
        return FhirResourceType.PATIENT.equals(resourceType);
    }
    
    public boolean isObservationResource() {
        return FhirResourceType.OBSERVATION.equals(resourceType);
    }
    
    public boolean isConditionResource() {
        return FhirResourceType.CONDITION.equals(resourceType);
    }
    
    // Enum for FHIR resource types
    public enum FhirResourceType {
        PATIENT("Patient"),
        OBSERVATION("Observation"),
        CONDITION("Condition"),
        MEDICATION("Medication"),
        ALLERGYINTOLERANCE("AllergyIntolerance"),
        DIAGNOSTICREPORT("DiagnosticReport"),
        ENCOUNTER("Encounter");
        
        private final String displayName;
        
        FhirResourceType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}
