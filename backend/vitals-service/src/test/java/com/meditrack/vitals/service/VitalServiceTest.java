package com.meditrack.vitals.service;

import com.meditrack.vitals.dto.VitalReadingMessage;
import com.meditrack.vitals.entity.Patient;
import com.meditrack.vitals.entity.VitalReading;
import com.meditrack.vitals.repository.PatientRepository;
import com.meditrack.vitals.repository.VitalReadingRepository;
import com.meditrack.vitals.service.VitalValidationService.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VitalServiceTest {

    @Mock
    private VitalReadingRepository vitalReadingRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private VitalValidationService validationService;

    @Mock
    private RedisCacheService redisCacheService;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private VitalService vitalService;

    private Patient patient;

    @BeforeEach
    void setUp() {
        patient = new Patient();
        patient.setId(1L);
        patient.setPatientIdentifier("PAT-001");
        patient.setFirstName("Alice");
        patient.setLastName("Smith");
        patient.setDateOfBirth(LocalDate.now().minusYears(30));
        patient.setGender("FEMALE");
        patient.setIsActive(true);
    }

    @Test
    void processVitalReadingPersistsReadingAndPublishesEvent() {
        VitalReadingMessage message = baseMessage("pat-001", "HEART_RATE", new BigDecimal("72"), "bpm");
        VitalReadingMessage normalized = baseMessage("PAT-001", "HEART_RATE", new BigDecimal("72"), "bpm");
        ValidationResult validationResult = new ValidationResult(true, List.of(), normalized);

        when(validationService.validateAndNormalize(any(VitalReadingMessage.class))).thenReturn(validationResult);
        when(patientRepository.findByPatientIdentifier("PAT-001")).thenReturn(Optional.of(patient));
        when(vitalReadingRepository.findLatestByPatientIdAndVitalType(1L, "HEART_RATE")).thenReturn(null);
        when(validationService.isDuplicate(any(VitalReadingMessage.class), isNull())).thenReturn(false);
        when(vitalReadingRepository.findByPatientIdAndVitalTypeAndTimeRange(
            eq(1L), eq("HEART_RATE"), any(LocalDateTime.class), any(LocalDateTime.class))
        ).thenReturn(List.of());
        when(vitalReadingRepository.save(any(VitalReading.class))).thenAnswer(invocation -> {
            VitalReading reading = invocation.getArgument(0);
            reading.setId(99L);
            return reading;
        });

        VitalReading result = vitalService.processVitalReading(message);

        assertEquals(99L, result.getId());
        assertEquals("HEART_RATE", result.getVitalType());
        assertEquals(new BigDecimal("72"), result.getValue());
        assertNotNull(result.getPatient());
        assertEquals("PAT-001", result.getPatient().getPatientIdentifier());

        verify(vitalReadingRepository).save(any(VitalReading.class));
        verify(redisCacheService).set(eq("latest_vital:1:HEART_RATE"), any(VitalReading.class), eq(3600L));
        verify(kafkaTemplate).send(eq("vital-events"), any(String.class));
        assertTrue(result.isHeartRate());
    }

    @Test
    void processVitalReadingReturnsExistingReadingForDuplicate() {
        VitalReadingMessage message = baseMessage("pat-001", "HEART_RATE", new BigDecimal("72"), "bpm");
        VitalReadingMessage normalized = baseMessage("PAT-001", "HEART_RATE", new BigDecimal("72"), "bpm");
        ValidationResult validationResult = new ValidationResult(true, List.of(), normalized);

        VitalReading existing = new VitalReading();
        existing.setId(12L);
        existing.setPatient(patient);
        existing.setVitalType("HEART_RATE");
        existing.setValue(new BigDecimal("72"));
        existing.setUnit("bpm");
        existing.setReadingTimestamp(LocalDateTime.now().minusSeconds(10));

        when(validationService.validateAndNormalize(any(VitalReadingMessage.class))).thenReturn(validationResult);
        when(patientRepository.findByPatientIdentifier("PAT-001")).thenReturn(Optional.of(patient));
        when(vitalReadingRepository.findLatestByPatientIdAndVitalType(1L, "HEART_RATE")).thenReturn(existing);
        when(validationService.isDuplicate(any(VitalReadingMessage.class), eq(existing))).thenReturn(true);

        VitalReading result = vitalService.processVitalReading(message);

        assertSame(existing, result);
        verify(vitalReadingRepository, never()).save(any());
        verify(redisCacheService, never()).set(any(), any(), anyLong());
        verify(kafkaTemplate, never()).send(any(String.class), any(String.class));
    }

    @Test
    void getLatestVitalsReturnsRepositoryReadingsWhenCacheMisses() {
        VitalReading heartRate = new VitalReading();
        heartRate.setId(20L);
        heartRate.setPatient(patient);
        heartRate.setVitalType("HEART_RATE");
        heartRate.setValue(new BigDecimal("70"));
        heartRate.setUnit("bpm");
        heartRate.setReadingTimestamp(LocalDateTime.now().minusMinutes(5));

        when(redisCacheService.get(any(String.class), eq(VitalReading.class))).thenReturn(null);
        when(vitalReadingRepository.findLatestByPatientIdAndVitalType(1L, "HEART_RATE")).thenReturn(heartRate);
        when(vitalReadingRepository.findLatestByPatientIdAndVitalType(1L, "BLOOD_PRESSURE")).thenReturn(null);
        when(vitalReadingRepository.findLatestByPatientIdAndVitalType(1L, "TEMPERATURE")).thenReturn(null);
        when(vitalReadingRepository.findLatestByPatientIdAndVitalType(1L, "SPO2")).thenReturn(null);
        when(vitalReadingRepository.findLatestByPatientIdAndVitalType(1L, "RESPIRATORY_RATE")).thenReturn(null);

        List<VitalReading> results = vitalService.getLatestVitals(1L);

        assertEquals(1, results.size());
        assertEquals("HEART_RATE", results.get(0).getVitalType());
        verify(redisCacheService).set(eq("latest_vital:1:HEART_RATE"), eq(heartRate), eq(3600L));
    }

    @Test
    void getPatientVitalAnalysisBuildsRiskSummary() {
        LocalDateTime since = LocalDateTime.now().minusHours(6);
        VitalReading heartRate = new VitalReading();
        heartRate.setPatient(patient);
        heartRate.setVitalType("HEART_RATE");
        heartRate.setValue(new BigDecimal("110"));
        heartRate.setUnit("bpm");
        heartRate.setReadingTimestamp(since.plusMinutes(5));

        VitalReading temperature = new VitalReading();
        temperature.setPatient(patient);
        temperature.setVitalType("TEMPERATURE");
        temperature.setValue(new BigDecimal("38.5"));
        temperature.setUnit("C");
        temperature.setReadingTimestamp(since.plusMinutes(10));

        VitalReading spo2 = new VitalReading();
        spo2.setPatient(patient);
        spo2.setVitalType("SPO2");
        spo2.setValue(new BigDecimal("91"));
        spo2.setUnit("%");
        spo2.setReadingTimestamp(since.plusMinutes(15));

        when(redisCacheService.get(any(String.class), eq(VitalReading.class))).thenReturn(null);
        when(vitalReadingRepository.findLatestByPatientIdAndVitalType(1L, "HEART_RATE")).thenReturn(heartRate);
        when(vitalReadingRepository.findLatestByPatientIdAndVitalType(1L, "BLOOD_PRESSURE")).thenReturn(null);
        when(vitalReadingRepository.findLatestByPatientIdAndVitalType(1L, "TEMPERATURE")).thenReturn(temperature);
        when(vitalReadingRepository.findLatestByPatientIdAndVitalType(1L, "SPO2")).thenReturn(spo2);
        when(vitalReadingRepository.findLatestByPatientIdAndVitalType(1L, "RESPIRATORY_RATE")).thenReturn(null);
        when(vitalReadingRepository.findRecentByPatientId(1L, since)).thenReturn(List.of(heartRate, temperature, spo2));
        when(vitalReadingRepository.findAbnormalReadingsByPatientId(1L, since)).thenReturn(List.of(heartRate, temperature, spo2));
        when(vitalReadingRepository.findCriticalReadingsByPatientId(1L, since)).thenReturn(List.of(heartRate));

        Map<String, Object> analysis = vitalService.getPatientVitalAnalysis(1L, since);

        assertEquals(1L, analysis.get("patientId"));
        assertEquals(3, analysis.get("recentReadingCount"));
        assertEquals(3, analysis.get("abnormalCount"));
        assertEquals(1, analysis.get("criticalCount"));
        assertEquals("ALERT", analysis.get("overallStatus"));
        assertEquals("CRITICAL", analysis.get("riskLevel"));
        assertTrue(analysis.containsKey("trendSummary"));
    }

    private VitalReadingMessage baseMessage(String patientIdentifier, String vitalType, BigDecimal value, String unit) {
        VitalReadingMessage message = new VitalReadingMessage();
        message.setPatientIdentifier(patientIdentifier);
        message.setVitalType(vitalType);
        message.setValue(value);
        message.setUnit(unit);
        message.setReadingTimestamp(LocalDateTime.now().minusMinutes(1));
        message.setSource("API");
        message.setQualityScore(new BigDecimal("0.95"));
        message.setNurseId("nurse-sarah");
        message.setDepartment("Cardiology");
        message.setNotes("Routine bedside assessment");
        return message;
    }
}
