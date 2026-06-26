package com.meditrack.patient.repository;

import com.meditrack.patient.entity.Patient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PatientRepositoryTestcontainersTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.2.0")
        .withDatabaseName("patient_service_test")
        .withUsername("patient")
        .withPassword("patient");

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @Autowired
    private PatientRepository patientRepository;

    @Test
    void saveAndSearchPatientInRealDatabase() {
        Patient patient = new Patient();
        patient.setPatientIdentifier("PT-100");
        patient.setFirstName("Alice");
        patient.setLastName("Wong");
        patient.setDateOfBirth(LocalDate.now().minusYears(34));
        patient.setGender("FEMALE");
        patient.setBloodType("O+");
        patient.setPhoneNumber("+1-555-0100");
        patient.setEmail("alice.wong@example.com");
        patient.setAddress("123 Main Street");
        patient.setEmergencyContactName("Bob Wong");
        patient.setEmergencyContactPhone("+1-555-0111");
        patient.setCreatedBy("integration-test");
        patient.setUpdatedBy("integration-test");
        patient.setIsActive(true);
        patient.setCreatedAt(LocalDateTime.now());
        patient.setUpdatedAt(LocalDateTime.now());

        patientRepository.saveAndFlush(patient);

        assertThat(patientRepository.findByPatientIdentifier("PT-100")).isPresent();
        assertThat(patientRepository.existsByPatientIdentifier("PT-100")).isTrue();
        assertThat(patientRepository.countByIsActive(true)).isEqualTo(1);
        assertThat(patientRepository.searchByNameOrIdentifier("alice", PageRequest.of(0, 10)).getTotalElements())
            .isEqualTo(1L);
    }
}
