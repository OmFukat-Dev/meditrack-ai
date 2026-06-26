package com.meditrack.patient.service;

import com.meditrack.patient.dto.PatientRequest;
import com.meditrack.patient.dto.PatientResponse;
import com.meditrack.patient.entity.Patient;
import com.meditrack.patient.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @InjectMocks
    private PatientService patientService;

    private PatientRequest request;

    @BeforeEach
    void setUp() {
        request = new PatientRequest();
        request.setPatientIdentifier("pat-001");
        request.setFirstName("Alice");
        request.setLastName("Smith");
        request.setDateOfBirth(LocalDate.now().minusYears(30));
        request.setGender("FEMALE");
        request.setBloodType("O+");
        request.setPhoneNumber("+1-555-0100");
        request.setEmail("alice.smith@example.com");
        request.setAddress("123 Main St");
        request.setEmergencyContactName("Bob Smith");
        request.setEmergencyContactPhone("+1-555-0111");
    }

    @Test
    void createPatientCreatesEntityAndReturnsResponse() {
        when(patientRepository.existsByPatientIdentifier("pat-001")).thenReturn(false);
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> {
            Patient patient = invocation.getArgument(0);
            patient.setId(1L);
            patient.setCreatedAt(LocalDateTime.of(2026, 4, 23, 8, 0));
            patient.setUpdatedAt(LocalDateTime.of(2026, 4, 23, 8, 0));
            return patient;
        });

        PatientResponse response = patientService.createPatient(request, "doctor-1");

        assertEquals(1L, response.getId());
        assertEquals("pat-001", response.getPatientIdentifier());
        assertEquals("Alice Smith", response.getFullName());
        assertEquals(30, response.getAge());
        assertTrue(response.getIsActive());
        assertEquals("doctor-1", response.getCreatedBy());
        assertEquals("doctor-1", response.getUpdatedBy());

        ArgumentCaptor<Patient> patientCaptor = ArgumentCaptor.forClass(Patient.class);
        verify(patientRepository).save(patientCaptor.capture());
        Patient saved = patientCaptor.getValue();
        assertEquals("pat-001", saved.getPatientIdentifier());
        assertEquals("doctor-1", saved.getCreatedBy());
        assertTrue(saved.getIsActive());
    }

    @Test
    void createPatientRejectsDuplicateIdentifier() {
        when(patientRepository.existsByPatientIdentifier("pat-001")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> patientService.createPatient(request, "doctor-1")
        );

        assertTrue(exception.getMessage().contains("already exists"));
        verify(patientRepository, never()).save(any());
    }

    @Test
    void updatePatientUpdatesExistingRecord() {
        Patient existing = basePatient("pat-001", "Alice", "Smith", 30);
        existing.setId(1L);
        existing.setCreatedBy("doctor-1");
        existing.setUpdatedBy("doctor-1");

        PatientRequest update = new PatientRequest();
        update.setPatientIdentifier("pat-002");
        update.setFirstName("Alicia");
        update.setLastName("Smythe");
        update.setDateOfBirth(LocalDate.now().minusYears(29));
        update.setGender("FEMALE");
        update.setBloodType("A+");
        update.setPhoneNumber("+1-555-0200");
        update.setEmail("alicia.smythe@example.com");
        update.setAddress("456 Main St");
        update.setEmergencyContactName("Bob Smith");
        update.setEmergencyContactPhone("+1-555-0111");

        when(patientRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(patientRepository.existsByPatientIdentifier("pat-002")).thenReturn(false);
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PatientResponse response = patientService.updatePatient(1L, update, "doctor-2");

        assertEquals("pat-002", response.getPatientIdentifier());
        assertEquals("Alicia Smythe", response.getFullName());
        assertEquals("doctor-2", response.getUpdatedBy());
        verify(patientRepository).save(existing);
    }

    @Test
    void deletePatientSoftDeletesRecord() {
        Patient existing = basePatient("pat-001", "Alice", "Smith", 30);
        existing.setId(1L);
        existing.setIsActive(true);

        when(patientRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        patientService.deletePatient(1L);

        assertFalse(existing.getIsActive());
        verify(patientRepository).save(existing);
    }

    @Test
    void getPatientsByAgeRangeSlicesResultsIntoPage() {
        Patient first = basePatient("pat-001", "Alice", "Anderson", 30);
        Patient second = basePatient("pat-002", "Beth", "Brown", 35);
        Patient third = basePatient("pat-003", "Cara", "Clark", 40);

        when(patientRepository.findByAgeRange(30, 40)).thenReturn(List.of(first, second, third));

        Page<PatientResponse> page = patientService.getPatientsByAgeRange(30, 40, 1, 2);

        assertEquals(3, page.getTotalElements());
        assertEquals(1, page.getContent().size());
        assertEquals("pat-003", page.getContent().get(0).getPatientIdentifier());
    }

    private Patient basePatient(String identifier, String firstName, String lastName, int age) {
        Patient patient = new Patient();
        patient.setPatientIdentifier(identifier);
        patient.setFirstName(firstName);
        patient.setLastName(lastName);
        patient.setDateOfBirth(LocalDate.now().minusYears(age));
        patient.setGender("FEMALE");
        patient.setBloodType("O+");
        patient.setPhoneNumber("+1-555-0100");
        patient.setEmail(firstName.toLowerCase() + "." + lastName.toLowerCase() + "@example.com");
        patient.setAddress("Test Address");
        patient.setEmergencyContactName("Emergency Contact");
        patient.setEmergencyContactPhone("+1-555-0111");
        patient.setIsActive(true);
        patient.setCreatedAt(LocalDateTime.of(2026, 4, 23, 8, 0));
        patient.setUpdatedAt(LocalDateTime.of(2026, 4, 23, 8, 0));
        return patient;
    }
}
