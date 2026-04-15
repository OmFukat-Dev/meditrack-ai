package com.meditrack.vitals.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class VitalReadingMessage {
    
    @NotBlank(message = "Patient identifier is required")
    private String patientIdentifier;
    
    @NotBlank(message = "Vital type is required")
    @Pattern(regexp = "^(HEART_RATE|BLOOD_PRESSURE|TEMPERATURE|SPO2|RESPIRATORY_RATE)$", 
             message = "Vital type must be HEART_RATE, BLOOD_PRESSURE, TEMPERATURE, SPO2, or RESPIRATORY_RATE")
    private String vitalType;
    
    @NotNull(message = "Value is required")
    @DecimalMin(value = "0.0", message = "Value must be positive")
    private BigDecimal value;
    
    @NotBlank(message = "Unit is required")
    private String unit;
    
    // For blood pressure
    private BigDecimal systolic;
    private BigDecimal diastolic;
    
    @NotNull(message = "Reading timestamp is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime readingTimestamp;
    
    @Pattern(regexp = "^(DEVICE|MANUAL|SIMULATOR|API)$", 
             message = "Source must be DEVICE, MANUAL, SIMULATOR, or API")
    private String source;
    
    private String deviceId;
    
    private String location;
    
    @DecimalMin(value = "0.0", message = "Quality score must be at least 0.0")
    @DecimalMax(value = "1.0", message = "Quality score must be at most 1.0")
    private BigDecimal qualityScore;
    
    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
    
    // Additional metadata for processing
    private String messageId;
    private String correlationId;
    private String processingSource;
    
    // Constructors
    public VitalReadingMessage() {}
    
    public VitalReadingMessage(String patientIdentifier, String vitalType, BigDecimal value, 
                           String unit, LocalDateTime readingTimestamp) {
        this.patientIdentifier = patientIdentifier;
        this.vitalType = vitalType;
        this.value = value;
        this.unit = unit;
        this.readingTimestamp = readingTimestamp;
    }
    
    // Getters and Setters
    public String getPatientIdentifier() { return patientIdentifier; }
    public void setPatientIdentifier(String patientIdentifier) { this.patientIdentifier = patientIdentifier; }
    
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
    
    public LocalDateTime getReadingTimestamp() { return readingTimestamp; }
    public void setReadingTimestamp(LocalDateTime readingTimestamp) { this.readingTimestamp = readingTimestamp; }
    
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
    
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    
    public String getProcessingSource() { return processingSource; }
    public void setProcessingSource(String processingSource) { this.processingSource = processingSource; }
    
    // Utility methods
    public boolean isBloodPressure() { return "BLOOD_PRESSURE".equals(vitalType); }
    public boolean isHeartRate() { return "HEART_RATE".equals(vitalType); }
    public boolean isTemperature() { return "TEMPERATURE".equals(vitalType); }
    public boolean isSpo2() { return "SPO2".equals(vitalType); }
    public boolean isRespiratoryRate() { return "RESPIRATORY_RATE".equals(vitalType); }
    
    public boolean hasValidBloodPressure() {
        return isBloodPressure() && systolic != null && diastolic != null;
    }
    
    public String getDisplayValue() {
        if (isBloodPressure() && hasValidBloodPressure()) {
            return systolic + "/" + diastolic + " " + unit;
        }
        return value + " " + unit;
    }
    
    @Override
    public String toString() {
        return "VitalReadingMessage{" +
               "patientIdentifier='" + patientIdentifier + '\'' +
               ", vitalType='" + vitalType + '\'' +
               ", value=" + value +
               ", unit='" + unit + '\'' +
               ", readingTimestamp=" + readingTimestamp +
               ", source='" + source + '\'' +
               ", deviceId='" + deviceId + '\'' +
               ", location='" + location + '\'' +
               ", qualityScore=" + qualityScore +
               '}';
    }
}
