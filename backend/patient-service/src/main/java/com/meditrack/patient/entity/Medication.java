package com.meditrack.patient.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "medications")
@EntityListeners(AuditingEntityListener.class)
public class Medication {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    
    @Column(name = "medication_name", nullable = false, length = 200)
    @NotBlank(message = "Medication name is required")
    @Size(max = 200, message = "Medication name must not exceed 200 characters")
    private String medicationName;
    
    @Column(name = "dosage", nullable = false, length = 100)
    @NotBlank(message = "Dosage is required")
    @Size(max = 100, message = "Dosage must not exceed 100 characters")
    private String dosage;
    
    @Column(name = "frequency", nullable = false, length = 100)
    @NotBlank(message = "Frequency is required")
    @Size(max = 100, message = "Frequency must not exceed 100 characters")
    private String frequency;
    
    @Column(name = "route", nullable = false, length = 50)
    @NotBlank(message = "Route is required")
    @Pattern(regexp = "^(ORAL|IV|IM|SC|TOPICAL|INHALATION|RECTAL|TRANSDERMAL|SUBCUTANEOUS)$", 
             message = "Route must be ORAL, IV, IM, SC, TOPICAL, INHALATION, RECTAL, TRANSDERMAL, SUBCUTANEOUS")
    private String route;
    
    @Column(name = "start_date", nullable = false)
    @NotNull(message = "Start date is required")
    @PastOrPresent(message = "Start date must be in the past or present")
    private LocalDate startDate;
    
    @Column(name = "end_date")
    @PastOrPresent(message = "End date must be in the past or present")
    private LocalDate endDate;
    
    @Column(name = "prescribed_by", nullable = false, length = 100)
    @NotBlank(message = "Prescribed by is required")
    @Size(max = 100, message = "Prescribed by must not exceed 100 characters")
    private String prescribedBy;
    
    @Column(name = "purpose", columnDefinition = "TEXT")
    @Size(max = 1000, message = "Purpose must not exceed 1000 characters")
    private String purpose;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
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
    public Medication() {}
    
    public Medication(Patient patient, String medicationName, String dosage, String frequency, String route, String prescribedBy) {
        this.patient = patient;
        this.medicationName = medicationName;
        this.dosage = dosage;
        this.frequency = frequency;
        this.route = route;
        this.prescribedBy = prescribedBy;
        this.startDate = LocalDate.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }
    
    public String getMedicationName() { return medicationName; }
    public void setMedicationName(String medicationName) { this.medicationName = medicationName; }
    
    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }
    
    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }
    
    public String getRoute() { return route; }
    public void setRoute(String route) { this.route = route; }
    
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    
    public String getPrescribedBy() { return prescribedBy; }
    public void setPrescribedBy(String prescribedBy) { this.prescribedBy = prescribedBy; }
    
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
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
        Medication medication = (Medication) o;
        return Objects.equals(id, medication.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Medication{" +
               "id=" + id +
               ", medicationName='" + medicationName + '\'' +
               ", dosage='" + dosage + '\'' +
               ", frequency='" + frequency + '\'' +
               ", route='" + route + '\'' +
               ", isActive=" + isActive +
               '}';
    }
    
    // Business logic methods
    public boolean isOral() {
        return "ORAL".equals(route);
    }
    
    public boolean isIntravenous() {
        return "IV".equals(route);
    }
    
    public boolean isIntramuscular() {
        return "IM".equals(route);
    }
    
    public boolean isTopical() {
        return "TOPICAL".equals(route);
    }
    
    public boolean isCurrentlyActive() {
        return isActive != null && isActive && 
               (endDate == null || !endDate.isBefore(LocalDate.now()));
    }
    
    public long getDurationDays() {
        if (startDate == null || endDate == null) return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
    }
    
    public String getFullDescription() {
        return medicationName + " " + dosage + " (" + frequency + ") - " + route;
    }
}
