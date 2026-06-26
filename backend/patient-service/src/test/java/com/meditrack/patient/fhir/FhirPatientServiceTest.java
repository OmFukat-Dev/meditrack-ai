package com.meditrack.patient.fhir;

import com.meditrack.patient.entity.FhirResource;
import com.meditrack.patient.entity.Patient;
import com.meditrack.patient.repository.FhirResourceRepository;
import com.meditrack.patient.repository.PatientRepository;
import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.Bundle;
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
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FhirPatientServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private FhirResourceRepository fhirResourceRepository;

    @InjectMocks
    private FhirPatientService fhirPatientService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(fhirPatientService, "fhirContext", FhirContext.forR4());
    }

    @Test
    void getAllFhirPatientsPagesTheDatabaseRead() {
        Patient first = patient("PT-100", "Alice", "Smith", "FEMALE", LocalDate.of(1990, 1, 1));
        Patient second = patient("PT-101", "Beth", "Jones", "FEMALE", LocalDate.of(1992, 2, 2));
        Page<Patient> page = new PageImpl<>(List.of(first, second), PageRequest.of(0, 2), 14);

        when(patientRepository.findByIsActive(eq(true), any(Pageable.class))).thenReturn(page);

        Bundle bundle = fhirPatientService.getAllFhirPatients(0, 2);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(patientRepository).findByIsActive(eq(true), pageableCaptor.capture());

        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(2);
        assertThat(bundle.getType()).isEqualTo(Bundle.BundleType.SEARCHSET);
        assertThat(bundle.getTotal()).isEqualTo(14);
        assertThat(bundle.getEntry()).hasSize(2);
    }

    @Test
    void searchPatientsPaginatesDatabaseLookupsAndUsesBirthDateFilter() {
        LocalDate birthDate = LocalDate.of(1991, 3, 15);
        Patient patient = patient("PT-200", "Alicia", "Brown", "FEMALE", birthDate);
        Page<Patient> page = new PageImpl<>(List.of(patient), PageRequest.of(1, 5), 6);

        when(patientRepository.advancedSearch(
            eq("Ali"),
            eq("Ali"),
            eq("FEMALE"),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            eq(birthDate),
            isNull(),
            isNull(),
            isNull(),
            any(Pageable.class)
        )).thenReturn(page);

        Bundle bundle = fhirPatientService.searchPatients(null, "Ali", "FEMALE", birthDate, 1, 5);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(patientRepository).advancedSearch(
            eq("Ali"),
            eq("Ali"),
            eq("FEMALE"),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            eq(birthDate),
            isNull(),
            isNull(),
            isNull(),
            pageableCaptor.capture()
        );

        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(5);
        assertThat(bundle.getType()).isEqualTo(Bundle.BundleType.SEARCHSET);
        assertThat(bundle.getTotal()).isEqualTo(6);
        assertThat(bundle.getEntry()).hasSize(1);
    }

    @Test
    void searchPatientsIdentifierLookupRemainsBounded() {
        Patient patient = patient("PT-300", "Clara", "Davis", "FEMALE", LocalDate.of(1988, 6, 20));
        when(patientRepository.findByPatientIdentifier("PT-300")).thenReturn(Optional.of(patient));

        Bundle bundle = fhirPatientService.searchPatients("PT-300", "ignored", "FEMALE", null, 4, 50);

        assertThat(bundle.getType()).isEqualTo(Bundle.BundleType.SEARCHSET);
        assertThat(bundle.getTotal()).isEqualTo(1);
        assertThat(bundle.getEntry()).hasSize(1);
    }

    private Patient patient(String identifier, String firstName, String lastName, String gender, LocalDate dateOfBirth) {
        Patient patient = new Patient();
        patient.setPatientIdentifier(identifier);
        patient.setFirstName(firstName);
        patient.setLastName(lastName);
        patient.setDateOfBirth(dateOfBirth);
        patient.setGender(gender);
        patient.setBloodType("O+");
        patient.setPhoneNumber("+1-555-0100");
        patient.setEmail(firstName.toLowerCase() + "." + lastName.toLowerCase() + "@example.com");
        patient.setAddress("123 Main Street");
        patient.setEmergencyContactName("Emergency Contact");
        patient.setEmergencyContactPhone("+1-555-0111");
        patient.setIsActive(true);
        patient.setCreatedAt(LocalDateTime.of(2026, 4, 23, 8, 0));
        patient.setUpdatedAt(LocalDateTime.of(2026, 4, 23, 8, 0));
        return patient;
    }
}
