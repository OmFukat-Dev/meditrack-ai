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
@Table(name = "medical_history")
@EntityListeners(AuditingEntityListener.class)
public class MedicalHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    
    @Column(name = "condition_name", nullable = false, length = 200)
    @NotBlank(message = "Condition name is required")
    @Size(max = 200, message = "Condition name must not exceed 200 characters")
    private String conditionName;
    
    @Column(name = "diagnosis_date")
    @PastOrPresent(message = "Diagnosis date must be in the past or present")
    private LocalDate diagnosisDate;
    
    @Column(name = "diagnosis_code", length = 20)
    @Pattern(regexp = "^[A-Z0-9.]+$", message = "Invalid ICD-10 code format")
    private String diagnosisCode;
    
    @Column(name = "severity", nullable = false, length = 20)
    @NotBlank(message = "Severity is required")
    @Pattern(regexp = "^(MILD|MODERATE|SEVERE)$", message = "Severity must be MILD, MODERATE, or SEVERE")
    private String severity;
    
    @Column(name = "status", nullable = false, length = 20)
    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^(ACTIVE|RESOLVED|CHRONIC)$", message = "Status must be ACTIVE, RESOLVED, or CHRONIC")
    private String status;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    @Size(max = 2000, message = "Notes must not exceed 2000 characters")
    private String notes;
    
    @Column(name = "doctor_name", length = 100)
    @Size(max = 100, message = "Doctor name must not exceed 100 characters")
    private String doctorName;
    
    @Column(name = "hospital_name", length = 100)
    @Size(max = 100, message = "Hospital name must not exceed 100 characters")
    private String hospitalName;
    
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
    public MedicalHistory() {}
    
    public MedicalHistory(Patient patient, String conditionName, String severity, String status) {
        this.patient = patient;
        this.conditionName = conditionName;
        this.severity = severity;
        this.status = status;
        this.diagnosisDate = LocalDate.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }
    
    public String getConditionName() { return conditionName; }
    public void setConditionName(String conditionName) { this.conditionName = conditionName; }
    
    public LocalDate getDiagnosisDate() { return diagnosisDate; }
    public void setDiagnosisDate(LocalDate diagnosisDate) { this.diagnosisDate = diagnosisDate; }
    
    public String getDiagnosisCode() { return diagnosisCode; }
    public void setDiagnosisCode(String diagnosisCode) { this.diagnosisCode = diagnosisCode; }
    
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }
    
    public String getHospitalName() { return hospitalName; }
    public void setHospitalName(String hospitalName) { this.hospitalName = hospitalName; }
    
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
        MedicalHistory that = (MedicalHistory) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "MedicalHistory{" +
               "id=" + id +
               ", conditionName='" + conditionName + '\'' +
               ", severity='" + severity + '\'' +
               ", status='" + status + '\'' +
               ", diagnosisDate=" + diagnosisDate +
               '}';
    }
    
    // Business logic methods
    public boolean isActive() {
        return "ACTIVE".equals(status);
    }
    
    public boolean isResolved() {
        return "RESOLVED".equals(status);
    }
    
    public boolean isChronic() {
        return "CHRONIC".equals(status);
    }
    
    public boolean isHighSeverity() {
        return "SEVERE".equals(severity);
    }
    
    public long getDaysSinceDiagnosis() {
        return diagnosisDate != null ? 
            java.time.temporal.ChronoUnit.DAYS.between(diagnosisDate, LocalDate.now()) : 0;
    }
}
