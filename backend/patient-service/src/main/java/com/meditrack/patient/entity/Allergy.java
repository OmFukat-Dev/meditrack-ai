package com.meditrack.patient.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "allergies")
@EntityListeners(AuditingEntityListener.class)
public class Allergy {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    
    @Column(name = "allergen", nullable = false, length = 100)
    @NotBlank(message = "Allergen is required")
    @Size(max = 100, message = "Allergen must not exceed 100 characters")
    private String allergen;
    
    @Column(name = "allergy_type", nullable = false, length = 50)
    @NotBlank(message = "Allergy type is required")
    @Pattern(regexp = "^(FOOD|MEDICATION|ENVIRONMENTAL)$", message = "Allergy type must be FOOD, MEDICATION, or ENVIRONMENTAL")
    private String allergyType;
    
    @Column(name = "severity", nullable = false, length = 20)
    @NotBlank(message = "Severity is required")
    @Pattern(regexp = "^(MILD|MODERATE|SEVERE)$", message = "Severity must be MILD, MODERATE, or SEVERE")
    private String severity;
    
    @Column(name = "reaction", columnDefinition = "TEXT")
    @Size(max = 500, message = "Reaction must not exceed 500 characters")
    private String reaction;
    
    @Column(name = "created_by", length = 50)
    private String createdBy;
    
    @Column(name = "updated_by", length = 50)
    private String updatedBy;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Constructors
    public Allergy() {}
    
    public Allergy(Patient patient, String allergen, String allergyType, String severity) {
        this.patient = patient;
        this.allergen = allergen;
        this.allergyType = allergyType;
        this.severity = severity;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }
    
    public String getAllergen() { return allergen; }
    public void setAllergen(String allergen) { this.allergen = allergen; }
    
    public String getAllergyType() { return allergyType; }
    public void setAllergyType(String allergyType) { this.allergyType = allergyType; }
    
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    
    public String getReaction() { return reaction; }
    public void setReaction(String reaction) { this.reaction = reaction; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    // Utility methods
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Allergy allergy = (Allergy) o;
        return Objects.equals(id, allergy.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Allergy{" +
               "id=" + id +
               ", allergen='" + allergen + '\'' +
               ", allergyType='" + allergyType + '\'' +
               ", severity='" + severity + '\'' +
               '}';
    }
    
    // Business logic methods
    public boolean isSevere() {
        return "SEVERE".equals(severity);
    }
    
    public boolean isFoodAllergy() {
        return "FOOD".equals(allergyType);
    }
    
    public boolean isMedicationAllergy() {
        return "MEDICATION".equals(allergyType);
    }
    
    public boolean isEnvironmentalAllergy() {
        return "ENVIRONMENTAL".equals(allergyType);
    }
}
