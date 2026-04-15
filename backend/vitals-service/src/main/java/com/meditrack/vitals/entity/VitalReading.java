package com.meditrack.vitals.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "vital_readings")
@EntityListeners(AuditingEntityListener.class)
public class VitalReading {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    
    @Column(name = "reading_timestamp", nullable = false)
    @NotNull(message = "Reading timestamp is required")
    private LocalDateTime readingTimestamp;
    
    @Column(name = "vital_type", nullable = false, length = 50)
    @NotBlank(message = "Vital type is required")
    @Pattern(regexp = "^(HEART_RATE|BLOOD_PRESSURE|TEMPERATURE|SPO2|RESPIRATORY_RATE)$", 
             message = "Vital type must be HEART_RATE, BLOOD_PRESSURE, TEMPERATURE, SPO2, or RESPIRATORY_RATE")
    private String vitalType;
    
    @Column(name = "value", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Value is required")
    @DecimalMin(value = "0.0", message = "Value must be positive")
    private BigDecimal value;
    
    @Column(name = "unit", nullable = false, length = 20)
    @NotBlank(message = "Unit is required")
    private String unit;
    
    @Column(name = "systolic", precision = 10, scale = 2)
    private BigDecimal systolic; // For blood pressure systolic
    
    @Column(name = "diastolic", precision = 10, scale = 2)
    private BigDecimal diastolic; // For blood pressure diastolic
    
    @Column(name = "source", length = 50)
    @Pattern(regexp = "^(DEVICE|MANUAL|SIMULATOR|API)$", 
             message = "Source must be DEVICE, MANUAL, SIMULATOR, or API")
    private String source;
    
    @Column(name = "device_id", length = 100)
    private String deviceId;
    
    @Column(name = "location", length = 100)
    private String location;
    
    @Column(name = "quality_score", precision = 3, scale = 2)
    @DecimalMin(value = "0.0", message = "Quality score must be at least 0.0")
    @DecimalMax(value = "1.0", message = "Quality score must be at most 1.0")
    private BigDecimal qualityScore;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    // Constructors
    public VitalReading() {}
    
    public VitalReading(Patient patient, String vitalType, BigDecimal value, String unit, LocalDateTime readingTimestamp) {
        this.patient = patient;
        this.vitalType = vitalType;
        this.value = value;
        this.unit = unit;
        this.readingTimestamp = readingTimestamp;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }
    
    public LocalDateTime getReadingTimestamp() { return readingTimestamp; }
    public void setReadingTimestamp(LocalDateTime readingTimestamp) { this.readingTimestamp = readingTimestamp; }
    
    public String getVitalType() { return vitalType; }
    public void setVitalType(String vitalType) { this.vitalType = vitalType; }
    
    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }
    
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    
    public BigDecimal getSystolic() { return systolic; }
    public void setSystolic(BigDecimal systolic) { this.systolic = systolic; }
    
    public BigDecimal getDiastolic() { return diastolic; }
    public void setDiastolic(BigDecimal diastolic) { this.diastolic = diastolic; }
    
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public BigDecimal getQualityScore() { return qualityScore; }
    public void setQualityScore(BigDecimal qualityScore) { this.qualityScore = qualityScore; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    // Utility methods
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VitalReading that = (VitalReading) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "VitalReading{" +
               "id=" + id +
               ", vitalType='" + vitalType + '\'' +
               ", value=" + value +
               ", unit='" + unit + '\'' +
               ", readingTimestamp=" + readingTimestamp +
               ", qualityScore=" + qualityScore +
               '}';
    }
    
    // Business logic methods
    public boolean isHeartRate() { return "HEART_RATE".equals(vitalType); }
    public boolean isBloodPressure() { return "BLOOD_PRESSURE".equals(vitalType); }
    public boolean isTemperature() { return "TEMPERATURE".equals(vitalType); }
    public boolean isSpo2() { return "SPO2".equals(vitalType); }
    public boolean isRespiratoryRate() { return "RESPIRATORY_RATE".equals(vitalType); }
    
    public boolean isHighQuality() {
        return qualityScore != null && qualityScore.compareTo(new BigDecimal("0.8")) >= 0;
    }
    
    public boolean isLowQuality() {
        return qualityScore != null && qualityScore.compareTo(new BigDecimal("0.5")) < 0;
    }
    
    public boolean isFromDevice() { return "DEVICE".equals(source); }
    public boolean isManual() { return "MANUAL".equals(source); }
    public boolean isFromSimulator() { return "SIMULATOR".equals(source); }
    
    // Vital-specific validation methods
    public boolean isValidHeartRate() {
        if (!isHeartRate()) return true;
        return value.compareTo(new BigDecimal("30")) >= 0 && value.compareTo(new BigDecimal("250")) <= 0;
    }
    
    public boolean isValidBloodPressure() {
        if (!isBloodPressure()) return true;
        return (systolic != null && systolic.compareTo(new BigDecimal("50")) >= 0 && systolic.compareTo(new BigDecimal("300")) <= 0) &&
               (diastolic != null && diastolic.compareTo(new BigDecimal("30")) >= 0 && diastolic.compareTo(new BigDecimal("200")) <= 0);
    }
    
    public boolean isValidTemperature() {
        if (!isTemperature()) return true;
        return value.compareTo(new BigDecimal("25.0")) >= 0 && value.compareTo(new BigDecimal("45.0")) <= 0;
    }
    
    public boolean isValidSpo2() {
        if (!isSpo2()) return true;
        return value.compareTo(new BigDecimal("70")) >= 0 && value.compareTo(new BigDecimal("100")) <= 0;
    }
    
    public boolean isValidRespiratoryRate() {
        if (!isRespiratoryRate()) return true;
        return value.compareTo(new BigDecimal("5")) >= 0 && value.compareTo(new BigDecimal("60")) <= 0;
    }
    
    public boolean isValidVital() {
        switch (vitalType) {
            case "HEART_RATE": return isValidHeartRate();
            case "BLOOD_PRESSURE": return isValidBloodPressure();
            case "TEMPERATURE": return isValidTemperature();
            case "SPO2": return isValidSpo2();
            case "RESPIRATORY_RATE": return isValidRespiratoryRate();
            default: return false;
        }
    }
    
    // Get display value
    public String getDisplayValue() {
        if (isBloodPressure() && systolic != null && diastolic != null) {
            return systolic + "/" + diastolic + " " + unit;
        }
        return value + " " + unit;
    }
    
    // Get vital status based on normal ranges
    public String getVitalStatus() {
        switch (vitalType) {
            case "HEART_RATE":
                if (value.compareTo(new BigDecimal("60")) < 0) return "LOW";
                if (value.compareTo(new BigDecimal("100")) > 0) return "HIGH";
                return "NORMAL";
            case "BLOOD_PRESSURE":
                if (systolic == null || diastolic == null) return "UNKNOWN";
                if (systolic.compareTo(new BigDecimal("120")) > 0 || diastolic.compareTo(new BigDecimal("80")) > 0) return "HIGH";
                if (systolic.compareTo(new BigDecimal("90")) < 0 || diastolic.compareTo(new BigDecimal("60")) < 0) return "LOW";
                return "NORMAL";
            case "TEMPERATURE":
                if (value.compareTo(new BigDecimal("36.0")) < 0) return "LOW";
                if (value.compareTo(new BigDecimal("37.5")) > 0) return "HIGH";
                return "NORMAL";
            case "SPO2":
                if (value.compareTo(new BigDecimal("95")) < 0) return "LOW";
                return "NORMAL";
            case "RESPIRATORY_RATE":
                if (value.compareTo(new BigDecimal("12")) < 0) return "LOW";
                if (value.compareTo(new BigDecimal("20")) > 0) return "HIGH";
                return "NORMAL";
            default: return "UNKNOWN";
        }
    }
}
