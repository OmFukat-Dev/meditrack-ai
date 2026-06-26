package com.meditrack.simulator.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VitalDataGeneratorTest {

    private final VitalDataGenerator generator = new VitalDataGenerator();
    private final LocalDateTime timestamp = LocalDateTime.of(2026, 4, 23, 10, 15);

    @Test
    void generatesHeartRateWithinExpectedBounds() {
        VitalDataGenerator.VitalProfile profile = new VitalDataGenerator.VitalProfile(
            34, 72, 120, 80, 36.6, 98, 16, VitalDataGenerator.ActivityLevel.RESTING
        );

        VitalDataGenerator.VitalReading reading = generator.generateVitalReading(
            "patient-1",
            VitalDataGenerator.HEART_RATE,
            profile,
            timestamp
        );

        assertEquals("patient-1", reading.getPatientId());
        assertEquals(VitalDataGenerator.HEART_RATE, reading.getVitalType());
        assertEquals("bpm", reading.getUnit());
        assertNotNull(reading.getValue());
        assertTrue(reading.getValue().doubleValue() >= 40);
        assertTrue(reading.getValue().doubleValue() <= 180);
        assertNotNull(reading.getQualityScore());
        assertTrue(reading.getQualityScore().doubleValue() >= 0.3);
        assertTrue(reading.getQualityScore().doubleValue() <= 1.0);
        assertTrue(reading.getDisplayValue().contains("bpm"));
    }

    @Test
    void generatesBloodPressureWithSystolicAboveDiastolic() {
        VitalDataGenerator.VitalProfile profile = new VitalDataGenerator.VitalProfile(
            62, 68, 126, 78, 36.4, 97, 18, VitalDataGenerator.ActivityLevel.LIGHT_ACTIVITY
        );

        VitalDataGenerator.VitalReading reading = generator.generateVitalReading(
            "patient-2",
            VitalDataGenerator.BLOOD_PRESSURE,
            profile,
            timestamp
        );

        assertNotNull(reading.getSystolic());
        assertNotNull(reading.getDiastolic());
        assertTrue(reading.getSystolic().doubleValue() > reading.getDiastolic().doubleValue());
        assertTrue(reading.getDisplayValue().contains("/"));
        assertTrue(reading.getDisplayValue().contains("mmHg"));
    }

    @Test
    void generatesCriticalLowAbnormalReading() {
        VitalDataGenerator.VitalReading reading = generator.generateAbnormalVitalReading(
            "patient-3",
            VitalDataGenerator.SPO2,
            VitalDataGenerator.AbnormalType.CRITICAL_LOW,
            timestamp
        );

        assertEquals("patient-3", reading.getPatientId());
        assertEquals(VitalDataGenerator.SPO2, reading.getVitalType());
        assertTrue(reading.getValue().doubleValue() < 90);
        assertTrue(reading.getNotes().contains("CRITICAL_LOW"));
    }

    @Test
    void rejectsUnknownVitalType() {
        VitalDataGenerator.VitalProfile profile = new VitalDataGenerator.VitalProfile(
            30, 70, 120, 80, 36.5, 98, 16, VitalDataGenerator.ActivityLevel.RESTING
        );

        assertThrows(IllegalArgumentException.class, () ->
            generator.generateVitalReading("patient-4", "UNKNOWN", profile, timestamp)
        );
    }
}
