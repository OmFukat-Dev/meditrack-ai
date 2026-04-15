package com.meditrack.vitals.service;

import com.meditrack.vitals.dto.VitalReadingMessage;
import com.meditrack.vitals.entity.VitalReading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class VitalValidationService {
    
    private static final Logger logger = LoggerFactory.getLogger(VitalValidationService.class);
    
    // Validation result class
    public static class ValidationResult {
        private boolean valid;
        private List<String> errors;
        private VitalReadingMessage normalizedMessage;
        
        public ValidationResult(boolean valid, List<String> errors, VitalReadingMessage normalizedMessage) {
            this.valid = valid;
            this.errors = errors;
            this.normalizedMessage = normalizedMessage;
        }
        
        public boolean isValid() { return valid; }
        public List<String> getErrors() { return errors; }
        public VitalReadingMessage getNormalizedMessage() { return normalizedMessage; }
    }
    
    // Main validation method
    public ValidationResult validateAndNormalize(VitalReadingMessage message) {
        List<String> errors = List.of();
        VitalReadingMessage normalized = new VitalReadingMessage();
        
        // Copy original message
        copyMessage(message, normalized);
        
        // Validate and normalize patient identifier
        if (message.getPatientIdentifier() == null || message.getPatientIdentifier().trim().isEmpty()) {
            errors.add("Patient identifier is required");
        } else {
            normalized.setPatientIdentifier(message.getPatientIdentifier().trim().toUpperCase());
        }
        
        // Validate and normalize vital type
        if (message.getVitalType() == null || message.getVitalType().trim().isEmpty()) {
            errors.add("Vital type is required");
        } else {
            String normalizedType = normalizeVitalType(message.getVitalType());
            if (normalizedType == null) {
                errors.add("Invalid vital type: " + message.getVitalType());
            } else {
                normalized.setVitalType(normalizedType);
            }
        }
        
        // Validate and normalize value
        if (message.getValue() == null) {
            errors.add("Value is required");
        } else {
            BigDecimal normalizedValue = normalizeValue(message.getValue(), message.getVitalType());
            if (normalizedValue == null) {
                errors.add("Invalid value for vital type " + message.getVitalType() + ": " + message.getValue());
            } else {
                normalized.setValue(normalizedValue);
            }
        }
        
        // Validate and normalize unit
        if (message.getUnit() == null || message.getUnit().trim().isEmpty()) {
            errors.add("Unit is required");
        } else {
            String normalizedUnit = normalizeUnit(message.getUnit(), message.getVitalType());
            if (normalizedUnit == null) {
                errors.add("Invalid unit for vital type " + message.getVitalType() + ": " + message.getUnit());
            } else {
                normalized.setUnit(normalizedUnit);
            }
        }
        
        // Validate and normalize timestamp
        if (message.getReadingTimestamp() == null) {
            errors.add("Reading timestamp is required");
        } else {
            LocalDateTime normalizedTimestamp = normalizeTimestamp(message.getReadingTimestamp());
            if (normalizedTimestamp == null) {
                errors.add("Invalid reading timestamp: " + message.getReadingTimestamp());
            } else {
                normalized.setReadingTimestamp(normalizedTimestamp);
            }
        }
        
        // Validate and normalize source
        if (message.getSource() != null) {
            String normalizedSource = normalizeSource(message.getSource());
            if (normalizedSource != null) {
                normalized.setSource(normalizedSource);
            } else {
                errors.add("Invalid source: " + message.getSource());
            }
        }
        
        // Normalize blood pressure specific fields
        if ("BLOOD_PRESSURE".equals(normalized.getVitalType())) {
            ValidationResult bpValidation = validateBloodPressure(normalized);
            if (!bpValidation.isValid()) {
                errors.addAll(bpValidation.getErrors());
            }
            normalized = bpValidation.getNormalizedMessage();
        }
        
        // Validate ranges
        if (errors.isEmpty()) {
            ValidationResult rangeValidation = validateValueRanges(normalized);
            if (!rangeValidation.isValid()) {
                errors.addAll(rangeValidation.getErrors());
            }
            normalized = rangeValidation.getNormalizedMessage();
        }
        
        // Validate quality score
        if (message.getQualityScore() != null) {
            BigDecimal normalizedQuality = normalizeQualityScore(message.getQualityScore());
            if (normalizedQuality != null) {
                normalized.setQualityScore(normalizedQuality);
            } else {
                errors.add("Invalid quality score: " + message.getQualityScore());
            }
        }
        
        // Normalize other fields
        if (message.getDeviceId() != null) {
            normalized.setDeviceId(message.getDeviceId().trim());
        }
        if (message.getLocation() != null) {
            normalized.setLocation(message.getLocation().trim());
        }
        if (message.getNotes() != null) {
            normalized.setNotes(message.getNotes().trim());
        }
        
        boolean isValid = errors.isEmpty();
        logger.debug("Validation result: valid={}, errors={}", isValid, errors);
        
        return new ValidationResult(isValid, errors, normalized);
    }
    
    // Validate vital reading after creation
    public boolean validateVitalReading(VitalReading vitalReading) {
        if (vitalReading == null) {
            logger.warn("Null vital reading provided for validation");
            return false;
        }
        
        // Check if vital reading is valid according to business rules
        if (!vitalReading.isValidVital()) {
            logger.warn("Invalid vital reading: {}", vitalReading);
            return false;
        }
        
        // Check timestamp is not too old or in future
        LocalDateTime now = LocalDateTime.now();
        if (vitalReading.getReadingTimestamp().isAfter(now.plusMinutes(5))) {
            logger.warn("Vital reading timestamp is too far in the future: {}", vitalReading.getReadingTimestamp());
            return false;
        }
        
        if (vitalReading.getReadingTimestamp().isBefore(now.minusHours(24))) {
            logger.warn("Vital reading timestamp is too old: {}", vitalReading.getReadingTimestamp());
            return false;
        }
        
        return true;
    }
    
    // Normalization methods
    private String normalizeVitalType(String vitalType) {
        if (vitalType == null) return null;
        
        String normalized = vitalType.trim().toUpperCase().replace(" ", "_");
        
        switch (normalized) {
            case "HEART_RATE":
            case "HR":
            case "HEARTRATE":
                return "HEART_RATE";
            case "BLOOD_PRESSURE":
            case "BP":
            case "BLOODPRESSURE":
                return "BLOOD_PRESSURE";
            case "TEMPERATURE":
            case "TEMP":
                return "TEMPERATURE";
            case "SPO2":
            case "OXYGEN_SATURATION":
            case "O2_SAT":
                return "SPO2";
            case "RESPIRATORY_RATE":
            case "RR":
            case "RESPIRATORYRATE":
                return "RESPIRATORY_RATE";
            default:
                return null;
        }
    }
    
    private BigDecimal normalizeValue(BigDecimal value, String vitalType) {
        if (value == null) return null;
        
        // Round to appropriate decimal places
        switch (vitalType) {
            case "HEART_RATE":
            case "SPO2":
            case "RESPIRATORY_RATE":
                return value.setScale(0, RoundingMode.HALF_UP);
            case "TEMPERATURE":
                return value.setScale(1, RoundingMode.HALF_UP);
            case "BLOOD_PRESSURE":
                return value.setScale(0, RoundingMode.HALF_UP);
            default:
                return value.setScale(2, RoundingMode.HALF_UP);
        }
    }
    
    private String normalizeUnit(String unit, String vitalType) {
        if (unit == null) return null;
        
        String normalized = unit.trim().toUpperCase();
        
        switch (vitalType) {
            case "HEART_RATE":
            case "RESPIRATORY_RATE":
                if (normalized.equals("BPM") || normalized.equals("BEATS_PER_MINUTE")) {
                    return "bpm";
                }
                break;
            case "BLOOD_PRESSURE":
                if (normalized.equals("MMHG") || normalized.equals("MM_HG")) {
                    return "mmHg";
                }
                break;
            case "TEMPERATURE":
                if (normalized.equals("C") || normalized.equals("CELSIUS")) {
                    return "°C";
                }
                if (normalized.equals("F") || normalized.equals("FAHRENHEIT")) {
                    return "°F";
                }
                break;
            case "SPO2":
                if (normalized.equals("%") || normalized.equals("PERCENT")) {
                    return "%";
                }
                break;
        }
        
        return normalized;
    }
    
    private LocalDateTime normalizeTimestamp(LocalDateTime timestamp) {
        if (timestamp == null) return null;
        
        // Check if timestamp is reasonable
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime minTimestamp = now.minusHours(24);
        LocalDateTime maxTimestamp = now.plusMinutes(5);
        
        if (timestamp.isBefore(minTimestamp)) {
            logger.warn("Timestamp too old, using current time: {}", timestamp);
            return now;
        }
        
        if (timestamp.isAfter(maxTimestamp)) {
            logger.warn("Timestamp too far in future, using current time: {}", timestamp);
            return now;
        }
        
        return timestamp;
    }
    
    private String normalizeSource(String source) {
        if (source == null) return null;
        
        String normalized = source.trim().toUpperCase();
        
        switch (normalized) {
            case "DEVICE":
            case "SENSOR":
            case "MONITOR":
                return "DEVICE";
            case "MANUAL":
            case "MANUAL_ENTRY":
            case "MANUAL_INPUT":
                return "MANUAL";
            case "SIMULATOR":
            case "SIMULATION":
            case "TEST":
                return "SIMULATOR";
            case "API":
            case "SYSTEM":
            case "AUTOMATED":
                return "API";
            default:
                return null;
        }
    }
    
    private BigDecimal normalizeQualityScore(BigDecimal qualityScore) {
        if (qualityScore == null) return null;
        
        // Clamp between 0.0 and 1.0
        BigDecimal clamped = qualityScore;
        if (clamped.compareTo(BigDecimal.ZERO) < 0) {
            clamped = BigDecimal.ZERO;
        } else if (clamped.compareTo(BigDecimal.ONE) > 0) {
            clamped = BigDecimal.ONE;
        }
        
        return clamped.setScale(2, RoundingMode.HALF_UP);
    }
    
    // Blood pressure specific validation
    private ValidationResult validateBloodPressure(VitalReadingMessage message) {
        List<String> errors = List.of();
        VitalReadingMessage normalized = message;
        
        if (message.getSystolic() == null && message.getDiastolic() == null) {
            // If no systolic/diastolic, try to parse from value
            if (message.getValue() != null) {
                String valueStr = message.getValue().toString();
                if (valueStr.contains("/")) {
                    String[] parts = valueStr.split("/");
                    if (parts.length == 2) {
                        try {
                            BigDecimal systolic = new BigDecimal(parts[0].trim());
                            BigDecimal diastolic = new BigDecimal(parts[1].trim());
                            normalized.setSystolic(systolic);
                            normalized.setDiastolic(diastolic);
                            normalized.setValue(systolic); // Use systolic as primary value
                        } catch (NumberFormatException e) {
                            errors.add("Invalid blood pressure format: " + valueStr);
                        }
                    } else {
                        errors.add("Invalid blood pressure format: " + valueStr);
                    }
                } else {
                    errors.add("Blood pressure requires systolic/diastolic values");
                }
            } else {
                errors.add("Blood pressure requires systolic/diastolic values");
            }
        } else {
            // Validate individual values
            if (message.getSystolic() != null) {
                if (message.getSystolic().compareTo(new BigDecimal("50")) < 0 || 
                    message.getSystolic().compareTo(new BigDecimal("300")) > 0) {
                    errors.add("Systolic blood pressure out of range (50-300 mmHg): " + message.getSystolic());
                }
            }
            
            if (message.getDiastolic() != null) {
                if (message.getDiastolic().compareTo(new BigDecimal("30")) < 0 || 
                    message.getDiastolic().compareTo(new BigDecimal("200")) > 0) {
                    errors.add("Diastolic blood pressure out of range (30-200 mmHg): " + message.getDiastolic());
                }
            }
            
            // Validate systolic > diastolic
            if (message.getSystolic() != null && message.getDiastolic() != null) {
                if (message.getSystolic().compareTo(message.getDiastolic()) <= 0) {
                    errors.add("Systolic must be greater than diastolic");
                }
            }
        }
        
        return new ValidationResult(errors.isEmpty(), errors, normalized);
    }
    
    // Value range validation
    private ValidationResult validateValueRanges(VitalReadingMessage message) {
        List<String> errors = List.of();
        VitalReadingMessage normalized = message;
        
        String vitalType = message.getVitalType();
        BigDecimal value = message.getValue();
        
        if (vitalType == null || value == null) {
            return new ValidationResult(false, List.of("Missing vital type or value"), normalized);
        }
        
        switch (vitalType) {
            case "HEART_RATE":
                if (value.compareTo(new BigDecimal("30")) < 0 || value.compareTo(new BigDecimal("250")) > 0) {
                    errors.add("Heart rate out of range (30-250 bpm): " + value);
                }
                break;
            case "TEMPERATURE":
                if (value.compareTo(new BigDecimal("25.0")) < 0 || value.compareTo(new BigDecimal("45.0")) > 0) {
                    errors.add("Temperature out of range (25.0-45.0 °C): " + value);
                }
                break;
            case "SPO2":
                if (value.compareTo(new BigDecimal("70")) < 0 || value.compareTo(new BigDecimal("100")) > 0) {
                    errors.add("SpO2 out of range (70-100%): " + value);
                }
                break;
            case "RESPIRATORY_RATE":
                if (value.compareTo(new BigDecimal("5")) < 0 || value.compareTo(new BigDecimal("60")) > 0) {
                    errors.add("Respiratory rate out of range (5-60 breaths/min): " + value);
                }
                break;
            case "BLOOD_PRESSURE":
                // Blood pressure validation handled in validateBloodPressure
                break;
        }
        
        return new ValidationResult(errors.isEmpty(), errors, normalized);
    }
    
    // Utility methods
    private void copyMessage(VitalReadingMessage source, VitalReadingMessage target) {
        target.setPatientIdentifier(source.getPatientIdentifier());
        target.setVitalType(source.getVitalType());
        target.setValue(source.getValue());
        target.setUnit(source.getUnit());
        target.setSystolic(source.getSystolic());
        target.setDiastolic(source.getDiastolic());
        target.setReadingTimestamp(source.getReadingTimestamp());
        target.setSource(source.getSource());
        target.setDeviceId(source.getDeviceId());
        target.setLocation(source.getLocation());
        target.setQualityScore(source.getQualityScore());
        target.setNotes(source.getNotes());
        target.setMessageId(source.getMessageId());
        target.setCorrelationId(source.getCorrelationId());
        target.setProcessingSource(source.getProcessingSource());
    }
    
    // Check for duplicate readings
    public boolean isDuplicate(VitalReadingMessage message, VitalReading existing) {
        if (existing == null) return false;
        
        // Check if readings are within 30 seconds and have same values
        long timeDiff = ChronoUnit.SECONDS.between(existing.getReadingTimestamp(), message.getReadingTimestamp());
        boolean sameValue = existing.getValue().compareTo(message.getValue()) == 0;
        
        return timeDiff < 30 && sameValue;
    }
    
    // Check for rapid changes
    public boolean hasRapidChange(VitalReading current, VitalReading previous) {
        if (current == null || previous == null) return false;
        
        // Check if same vital type
        if (!current.getVitalType().equals(previous.getVitalType())) return false;
        
        // Check time difference
        long timeDiff = ChronoUnit.MINUTES.between(previous.getReadingTimestamp(), current.getReadingTimestamp());
        if (timeDiff > 5) return false; // Only check for rapid changes within 5 minutes
        
        // Check for rapid changes based on vital type
        BigDecimal currentValue = current.getValue();
        BigDecimal previousValue = previous.getValue();
        BigDecimal change = currentValue.subtract(previousValue).abs();
        BigDecimal percentChange = change.divide(previousValue, 2, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
        
        switch (current.getVitalType()) {
            case "HEART_RATE":
                return percentChange.compareTo(new BigDecimal("50")) > 0; // 50% change
            case "TEMPERATURE":
                return percentChange.compareTo(new BigDecimal("10")) > 0; // 10% change
            case "SPO2":
                return percentChange.compareTo(new BigDecimal("20")) > 0; // 20% change
            case "RESPIRATORY_RATE":
                return percentChange.compareTo(new BigDecimal("40")) > 0; // 40% change
            case "BLOOD_PRESSURE":
                return percentChange.compareTo(new BigDecimal("30")) > 0; // 30% change
            default:
                return false;
        }
    }
}
