package com.meditrack.vitals.service;

import com.meditrack.vitals.dto.VitalReadingMessage;
import com.meditrack.vitals.entity.Patient;
import com.meditrack.vitals.entity.VitalReading;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VitalValidationServiceTest {

    private final VitalValidationService service = new VitalValidationService();

    @Test
    void validateAndNormalizeNormalizesFieldsAndAcceptsBloodPressure() {
        VitalReadingMessage message = new VitalReadingMessage();
        message.setPatientIdentifier(" pat-001 ");
        message.setVitalType("BLOOD_PRESSURE");
        message.setValue(new BigDecimal("120"));
        message.setUnit("MMHG");
        message.setSystolic(new BigDecimal("120"));
        message.setDiastolic(new BigDecimal("80"));
        message.setReadingTimestamp(LocalDateTime.now().minusMinutes(1));
        message.setSource("sensor");
        message.setQualityScore(new BigDecimal("1.2"));
        message.setDeviceId("device-1");
        message.setLocation(" ward-a ");
        message.setNurseId("nurse-sarah");
        message.setDepartment("Cardiology");
        message.setNotes("  stable reading  ");

        VitalValidationService.ValidationResult result = service.validateAndNormalize(message);

        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
        assertEquals("PAT-001", result.getNormalizedMessage().getPatientIdentifier());
        assertEquals("BLOOD_PRESSURE", result.getNormalizedMessage().getVitalType());
        assertEquals("mmHg", result.getNormalizedMessage().getUnit());
        assertEquals("DEVICE", result.getNormalizedMessage().getSource());
        assertEquals(new BigDecimal("1.00"), result.getNormalizedMessage().getQualityScore());
        assertNotNull(result.getNormalizedMessage().getReadingTimestamp());
    }

    @Test
    void validateAndNormalizeCollectsErrorsForMissingRequiredFields() {
        VitalReadingMessage message = new VitalReadingMessage();
        message.setPatientIdentifier(" ");
        message.setVitalType(" ");
        message.setUnit(" ");
        message.setReadingTimestamp(null);
        message.setNurseId(" ");
        message.setDepartment(" ");
        message.setNotes(" ");

        VitalValidationService.ValidationResult result = service.validateAndNormalize(message);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Patient identifier is required"));
        assertTrue(result.getErrors().contains("Vital type is required"));
        assertTrue(result.getErrors().contains("Unit is required"));
        assertTrue(result.getErrors().contains("Reading timestamp is required"));
    }

    @Test
    void validateVitalReadingRejectsStaleReading() {
        Patient patient = new Patient();
        patient.setId(1L);
        patient.setPatientIdentifier("PAT-001");
        patient.setFirstName("Alice");
        patient.setLastName("Smith");
        patient.setDateOfBirth(LocalDate.now().minusYears(30));
        patient.setGender("FEMALE");

        VitalReading reading = new VitalReading();
        reading.setPatient(patient);
        reading.setVitalType("HEART_RATE");
        reading.setValue(new BigDecimal("72"));
        reading.setUnit("bpm");
        reading.setReadingTimestamp(LocalDateTime.now().minusDays(2));

        assertFalse(service.validateVitalReading(reading));
    }

    @Test
    void isDuplicateReturnsTrueForCloseReadingsWithSameValue() {
        Patient patient = new Patient();
        patient.setId(1L);

        VitalReading existing = new VitalReading();
        existing.setPatient(patient);
        existing.setVitalType("HEART_RATE");
        existing.setValue(new BigDecimal("72"));
        existing.setUnit("bpm");
        existing.setReadingTimestamp(LocalDateTime.now().minusSeconds(10));

        VitalReadingMessage message = new VitalReadingMessage();
        message.setPatientIdentifier("PAT-001");
        message.setVitalType("HEART_RATE");
        message.setValue(new BigDecimal("72"));
        message.setUnit("bpm");
        message.setReadingTimestamp(LocalDateTime.now());
        message.setNurseId("nurse-sarah");
        message.setDepartment("Cardiology");
        message.setNotes("Routine reading");

        assertTrue(service.isDuplicate(message, existing));
    }

    @Test
    void hasRapidChangeDetectsLargeHeartRateSpike() {
        VitalReading previous = new VitalReading();
        previous.setVitalType("HEART_RATE");
        previous.setValue(new BigDecimal("70"));
        previous.setUnit("bpm");
        previous.setReadingTimestamp(LocalDateTime.now().minusMinutes(3));

        VitalReading current = new VitalReading();
        current.setVitalType("HEART_RATE");
        current.setValue(new BigDecimal("120"));
        current.setUnit("bpm");
        current.setReadingTimestamp(LocalDateTime.now());

        assertTrue(service.hasRapidChange(current, previous));
    }
}
