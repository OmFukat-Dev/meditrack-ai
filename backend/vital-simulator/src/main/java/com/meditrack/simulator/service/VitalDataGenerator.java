package com.meditrack.simulator.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class VitalDataGenerator {
    
    private final Random random = new Random();
    
    // Vital type constants
    public static final String HEART_RATE = "HEART_RATE";
    public static final String BLOOD_PRESSURE = "BLOOD_PRESSURE";
    public static final String TEMPERATURE = "TEMPERATURE";
    public static final String SPO2 = "SPO2";
    public static final String RESPIRATORY_RATE = "RESPIRATORY_RATE";
    
    // Normal vital ranges
    private static final VitalRange HEART_RATE_RANGE = new VitalRange(60, 100, 40, 180);
    private static final VitalRange BLOOD_PRESSURE_SYSTOLIC = new VitalRange(90, 140, 70, 200);
    private static final VitalRange BLOOD_PRESSURE_DIASTOLIC = new VitalRange(60, 90, 40, 120);
    private static final VitalRange TEMPERATURE_RANGE = new VitalRange(36.0, 37.5, 34.0, 41.0);
    private static final VitalRange SPO2_RANGE = new VitalRange(95, 100, 85, 100);
    private static final VitalRange RESPIRATORY_RATE_RANGE = new VitalRange(12, 20, 5, 35);
    
    // Vital range class
    private static class VitalRange {
        final double minNormal;
        final double maxNormal;
        final double minPossible;
        final double maxPossible;
        
        VitalRange(double minNormal, double maxNormal, double minPossible, double maxPossible) {
            this.minNormal = minNormal;
            this.maxNormal = maxNormal;
            this.minPossible = minPossible;
            this.maxPossible = maxPossible;
        }
    }
    
    // Generate realistic vital reading
    public VitalReading generateVitalReading(String patientId, String vitalType, 
                                          VitalProfile profile, LocalDateTime timestamp) {
        switch (vitalType) {
            case HEART_RATE:
                return generateHeartRate(patientId, profile, timestamp);
            case BLOOD_PRESSURE:
                return generateBloodPressure(patientId, profile, timestamp);
            case TEMPERATURE:
                return generateTemperature(patientId, profile, timestamp);
            case SPO2:
                return generateSpo2(patientId, profile, timestamp);
            case RESPIRATORY_RATE:
                return generateRespiratoryRate(patientId, profile, timestamp);
            default:
                throw new IllegalArgumentException("Unknown vital type: " + vitalType);
        }
    }
    
    // Generate heart rate with realistic patterns
    private VitalReading generateHeartRate(String patientId, VitalProfile profile, LocalDateTime timestamp) {
        double baseRate = profile.getBaseHeartRate();
        
        // Add realistic variations
        double variation = calculateRealisticVariation(baseRate, profile.getActivityLevel(), timestamp);
        double heartRate = baseRate + variation;
        
        // Apply age-related adjustments
        heartRate = applyAgeAdjustment(heartRate, profile.getAge());
        
        // Add some noise for realism
        heartRate += random.nextGaussian() * 2;
        
        // Ensure within realistic bounds
        heartRate = clamp(heartRate, HEART_RATE_RANGE.minPossible, HEART_RATE_RANGE.maxPossible);
        
        return new VitalReading(
            patientId,
            HEART_RATE,
            new BigDecimal(Math.round(heartRate)),
            "bpm",
            timestamp,
            "SIMULATOR",
            "DEVICE_" + patientId,
            "ICU_BED_" + patientId,
            calculateQualityScore(heartRate, HEART_RATE_RANGE),
            "Generated heart rate reading"
        );
    }
    
    // Generate blood pressure with realistic patterns
    private VitalReading generateBloodPressure(String patientId, VitalProfile profile, LocalDateTime timestamp) {
        double baseSystolic = profile.getBaseSystolic();
        double baseDiastolic = profile.getBaseDiastolic();
        
        // Add realistic variations
        double systolicVariation = calculateRealisticVariation(baseSystolic, profile.getActivityLevel(), timestamp);
        double diastolicVariation = calculateRealisticVariation(baseDiastolic, profile.getActivityLevel(), timestamp);
        
        double systolic = baseSystolic + systolicVariation;
        double diastolic = baseDiastolic + diastolicVariation;
        
        // Apply age-related adjustments
        systolic = applyAgeAdjustment(systolic, profile.getAge());
        diastolic = applyAgeAdjustment(diastolic, profile.getAge());
        
        // Add noise
        systolic += random.nextGaussian() * 3;
        diastolic += random.nextGaussian() * 2;
        
        // Ensure realistic bounds
        systolic = clamp(systolic, BLOOD_PRESSURE_SYSTOLIC.minPossible, BLOOD_PRESSURE_SYSTOLIC.maxPossible);
        diastolic = clamp(diastolic, BLOOD_PRESSURE_DIASTOLIC.minPossible, BLOOD_PRESSURE_DIASTOLIC.maxPossible);
        
        // Ensure systolic > diastolic
        if (systolic <= diastolic) {
            systolic = diastolic + 20;
        }
        
        return new VitalReading(
            patientId,
            BLOOD_PRESSURE,
            new BigDecimal(Math.round(systolic)),
            "mmHg",
            timestamp,
            "SIMULATOR",
            "DEVICE_" + patientId,
            "ICU_BED_" + patientId,
            calculateQualityScore(systolic, BLOOD_PRESSURE_SYSTOLIC),
            "Generated blood pressure reading",
            new BigDecimal(Math.round(systolic)),
            new BigDecimal(Math.round(diastolic))
        );
    }
    
    // Generate temperature with realistic patterns
    private VitalReading generateTemperature(String patientId, VitalProfile profile, LocalDateTime timestamp) {
        double baseTemp = profile.getBaseTemperature();
        
        // Add realistic variations
        double variation = calculateRealisticVariation(baseTemp, profile.getActivityLevel(), timestamp);
        double temperature = baseTemp + variation;
        
        // Add circadian rhythm effect
        temperature += calculateCircadianEffect(timestamp);
        
        // Add noise
        temperature += random.nextGaussian() * 0.1;
        
        // Ensure realistic bounds
        temperature = clamp(temperature, TEMPERATURE_RANGE.minPossible, TEMPERATURE_RANGE.maxPossible);
        
        return new VitalReading(
            patientId,
            TEMPERATURE,
            new BigDecimal(temperature).setScale(1, RoundingMode.HALF_UP),
            "°C",
            timestamp,
            "SIMULATOR",
            "DEVICE_" + patientId,
            "ICU_BED_" + patientId,
            calculateQualityScore(temperature, TEMPERATURE_RANGE),
            "Generated temperature reading"
        );
    }
    
    // Generate SpO2 with realistic patterns
    private VitalReading generateSpo2(String patientId, VitalProfile profile, LocalDateTime timestamp) {
        double baseSpo2 = profile.getBaseSpo2();
        
        // Add realistic variations
        double variation = calculateRealisticVariation(baseSpo2, profile.getActivityLevel(), timestamp);
        double spo2 = baseSpo2 + variation;
        
        // Add noise
        spo2 += random.nextGaussian() * 0.5;
        
        // Ensure realistic bounds
        spo2 = clamp(spo2, SPO2_RANGE.minPossible, SPO2_RANGE.maxPossible);
        
        return new VitalReading(
            patientId,
            SPO2,
            new BigDecimal(Math.round(spo2)),
            "%",
            timestamp,
            "SIMULATOR",
            "DEVICE_" + patientId,
            "ICU_BED_" + patientId,
            calculateQualityScore(spo2, SPO2_RANGE),
            "Generated SpO2 reading"
        );
    }
    
    // Generate respiratory rate with realistic patterns
    private VitalReading generateRespiratoryRate(String patientId, VitalProfile profile, LocalDateTime timestamp) {
        double baseRate = profile.getBaseRespiratoryRate();
        
        // Add realistic variations
        double variation = calculateRealisticVariation(baseRate, profile.getActivityLevel(), timestamp);
        double respiratoryRate = baseRate + variation;
        
        // Add noise
        respiratoryRate += random.nextGaussian() * 1;
        
        // Ensure realistic bounds
        respiratoryRate = clamp(respiratoryRate, RESPIRATORY_RATE_RANGE.minPossible, RESPIRATORY_RATE_RANGE.maxPossible);
        
        return new VitalReading(
            patientId,
            RESPIRATORY_RATE,
            new BigDecimal(Math.round(respiratoryRate)),
            "breaths/min",
            timestamp,
            "SIMULATOR",
            "DEVICE_" + patientId,
            "ICU_BED_" + patientId,
            calculateQualityScore(respiratoryRate, RESPIRATORY_RATE_RANGE),
            "Generated respiratory rate reading"
        );
    }
    
    // Calculate realistic variation based on activity level and time
    private double calculateRealisticVariation(double baseValue, ActivityLevel activityLevel, LocalDateTime timestamp) {
        double variation = 0;
        
        // Activity level variations
        switch (activityLevel) {
            case RESTING:
                variation += random.nextGaussian() * 2;
                break;
            case LIGHT_ACTIVITY:
                variation += random.nextGaussian() * 5 + 5;
                break;
            case MODERATE_ACTIVITY:
                variation += random.nextGaussian() * 8 + 15;
                break;
            case INTENSE_ACTIVITY:
                variation += random.nextGaussian() * 12 + 30;
                break;
        }
        
        // Time-based variations (simulating natural fluctuations)
        int minute = timestamp.getMinute();
        int second = timestamp.getSecond();
        
        // Add periodic variations (every few minutes)
        if (minute % 3 == 0 && second < 30) {
            variation += random.nextGaussian() * 3;
        }
        
        // Add occasional spikes
        if (random.nextDouble() < 0.05) { // 5% chance
            variation += random.nextGaussian() * 10;
        }
        
        return variation;
    }
    
    // Calculate circadian rhythm effect for temperature
    private double calculateCircadianEffect(LocalDateTime timestamp) {
        int hour = timestamp.getHour();
        
        // Temperature is typically lower in early morning (2-4 AM) and higher in late afternoon (4-6 PM)
        if (hour >= 2 && hour <= 4) {
            return -0.3; // Lower in early morning
        } else if (hour >= 16 && hour <= 18) {
            return 0.2; // Higher in late afternoon
        } else {
            return 0;
        }
    }
    
    // Apply age-related adjustments
    private double applyAgeAdjustment(double value, int age) {
        if (age < 18) {
            // Children typically have higher heart rates
            return value * 1.1;
        } else if (age > 65) {
            // Elderly may have slightly different patterns
            return value * 0.95;
        }
        return value;
    }
    
    // Calculate quality score based on how "normal" the reading is
    private BigDecimal calculateQualityScore(double value, VitalRange range) {
        double quality = 1.0;
        
        if (value < range.minNormal || value > range.maxNormal) {
            // Outside normal range reduces quality
            double deviation = Math.max(range.minNormal - value, value - range.maxNormal);
            quality = Math.max(0.3, 1.0 - (deviation / (range.maxPossible - range.minPossible)));
        }
        
        // Add some random variation
        quality += random.nextGaussian() * 0.1;
        quality = clamp(quality, 0.3, 1.0);
        
        return new BigDecimal(quality).setScale(2, RoundingMode.HALF_UP);
    }
    
    // Generate abnormal vital reading for testing
    public VitalReading generateAbnormalVitalReading(String patientId, String vitalType, 
                                                   AbnormalType abnormalType, LocalDateTime timestamp) {
        VitalReading reading = generateVitalReading(patientId, vitalType, createDefaultProfile(), timestamp);
        
        switch (abnormalType) {
            case HIGH:
                reading.setValue(generateHighValue(vitalType));
                break;
            case LOW:
                reading.setValue(generateLowValue(vitalType));
                break;
            case CRITICAL_HIGH:
                reading.setValue(generateCriticalHighValue(vitalType));
                break;
            case CRITICAL_LOW:
                reading.setValue(generateCriticalLowValue(vitalType));
                break;
        }
        
        reading.setNotes("Simulated abnormal reading - " + abnormalType);
        return reading;
    }
    
    // Generate high values
    private BigDecimal generateHighValue(String vitalType) {
        switch (vitalType) {
            case HEART_RATE:
                return new BigDecimal(110 + random.nextInt(40));
            case BLOOD_PRESSURE:
                return new BigDecimal(150 + random.nextInt(30));
            case TEMPERATURE:
                return new BigDecimal(38.0 + random.nextDouble() * 2);
            case SPO2:
                return new BigDecimal(90 + random.nextInt(4));
            case RESPIRATORY_RATE:
                return new BigDecimal(22 + random.nextInt(8));
            default:
                return new BigDecimal(100);
        }
    }
    
    // Generate low values
    private BigDecimal generateLowValue(String vitalType) {
        switch (vitalType) {
            case HEART_RATE:
                return new BigDecimal(50 + random.nextInt(10));
            case BLOOD_PRESSURE:
                return new BigDecimal(80 + random.nextInt(10));
            case TEMPERATURE:
                return new BigDecimal(35.5 + random.nextDouble() * 0.5);
            case SPO2:
                return new BigDecimal(91 + random.nextInt(4));
            case RESPIRATORY_RATE:
                return new BigDecimal(10 + random.nextInt(2));
            default:
                return new BigDecimal(50);
        }
    }
    
    // Generate critical high values
    private BigDecimal generateCriticalHighValue(String vitalType) {
        switch (vitalType) {
            case HEART_RATE:
                return new BigDecimal(150 + random.nextInt(30));
            case BLOOD_PRESSURE:
                return new BigDecimal(180 + random.nextInt(20));
            case TEMPERATURE:
                return new BigDecimal(39.5 + random.nextDouble() * 1.5);
            case SPO2:
                return new BigDecimal(85 + random.nextInt(5));
            case RESPIRATORY_RATE:
                return new BigDecimal(30 + random.nextInt(5));
            default:
                return new BigDecimal(200);
        }
    }
    
    // Generate critical low values
    private BigDecimal generateCriticalLowValue(String vitalType) {
        switch (vitalType) {
            case HEART_RATE:
                return new BigDecimal(35 + random.nextInt(5));
            case BLOOD_PRESSURE:
                return new BigDecimal(60 + random.nextInt(10));
            case TEMPERATURE:
                return new BigDecimal(34.5 + random.nextDouble() * 0.5);
            case SPO2:
                return new BigDecimal(80 + random.nextInt(5));
            case RESPIRATORY_RATE:
                return new BigDecimal(6 + random.nextInt(2));
            default:
                return new BigDecimal(30);
        }
    }
    
    // Utility methods
    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
    
    // Create default vital profile
    private VitalProfile createDefaultProfile() {
        return new VitalProfile(30, 70, 120, 80, 36.5, 98, 16, ActivityLevel.RESTING);
    }
    
    // Vital reading class
    public static class VitalReading {
        private String patientId;
        private String vitalType;
        private BigDecimal value;
        private String unit;
        private LocalDateTime timestamp;
        private String source;
        private String deviceId;
        private String location;
        private BigDecimal qualityScore;
        private String notes;
        private BigDecimal systolic;
        private BigDecimal diastolic;
        
        public VitalReading(String patientId, String vitalType, BigDecimal value, String unit,
                          LocalDateTime timestamp, String source, String deviceId, String location,
                          BigDecimal qualityScore, String notes) {
            this.patientId = patientId;
            this.vitalType = vitalType;
            this.value = value;
            this.unit = unit;
            this.timestamp = timestamp;
            this.source = source;
            this.deviceId = deviceId;
            this.location = location;
            this.qualityScore = qualityScore;
            this.notes = notes;
        }
        
        public VitalReading(String patientId, String vitalType, BigDecimal value, String unit,
                          LocalDateTime timestamp, String source, String deviceId, String location,
                          BigDecimal qualityScore, String notes, BigDecimal systolic, BigDecimal diastolic) {
            this(patientId, vitalType, value, unit, timestamp, source, deviceId, location, qualityScore, notes);
            this.systolic = systolic;
            this.diastolic = diastolic;
        }
        
        // Getters and setters
        public String getPatientId() { return patientId; }
        public void setPatientId(String patientId) { this.patientId = patientId; }
        
        public String getVitalType() { return vitalType; }
        public void setVitalType(String vitalType) { this.vitalType = vitalType; }
        
        public BigDecimal getValue() { return value; }
        public void setValue(BigDecimal value) { this.value = value; }
        
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        
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
        
        public BigDecimal getSystolic() { return systolic; }
        public void setSystolic(BigDecimal systolic) { this.systolic = systolic; }
        
        public BigDecimal getDiastolic() { return diastolic; }
        public void setDiastolic(BigDecimal diastolic) { this.diastolic = diastolic; }
        
        public String getDisplayValue() {
            if (BLOOD_PRESSURE.equals(vitalType) && systolic != null && diastolic != null) {
                return systolic + "/" + diastolic + " " + unit;
            }
            return value + " " + unit;
        }
    }
    
    // Vital profile class
    public static class VitalProfile {
        private int age;
        private double baseHeartRate;
        private double baseSystolic;
        private double baseDiastolic;
        private double baseTemperature;
        private double baseSpo2;
        private double baseRespiratoryRate;
        private ActivityLevel activityLevel;
        
        public VitalProfile(int age, double baseHeartRate, double baseSystolic, double baseDiastolic,
                          double baseTemperature, double baseSpo2, double baseRespiratoryRate,
                          ActivityLevel activityLevel) {
            this.age = age;
            this.baseHeartRate = baseHeartRate;
            this.baseSystolic = baseSystolic;
            this.baseDiastolic = baseDiastolic;
            this.baseTemperature = baseTemperature;
            this.baseSpo2 = baseSpo2;
            this.baseRespiratoryRate = baseRespiratoryRate;
            this.activityLevel = activityLevel;
        }
        
        // Getters
        public int getAge() { return age; }
        public double getBaseHeartRate() { return baseHeartRate; }
        public double getBaseSystolic() { return baseSystolic; }
        public double getBaseDiastolic() { return baseDiastolic; }
        public double getBaseTemperature() { return baseTemperature; }
        public double getBaseSpo2() { return baseSpo2; }
        public double getBaseRespiratoryRate() { return baseRespiratoryRate; }
        public ActivityLevel getActivityLevel() { return activityLevel; }
    }
    
    // Activity level enum
    public enum ActivityLevel {
        RESTING, LIGHT_ACTIVITY, MODERATE_ACTIVITY, INTENSE_ACTIVITY
    }
    
    // Abnormal type enum
    public enum AbnormalType {
        HIGH, LOW, CRITICAL_HIGH, CRITICAL_LOW
    }
}
