package com.meditrack.patient.health;

import com.meditrack.patient.repository.AllergyRepository;
import com.meditrack.patient.repository.FhirResourceRepository;
import com.meditrack.patient.repository.MedicalHistoryRepository;
import com.meditrack.patient.repository.MedicationRepository;
import com.meditrack.patient.repository.PatientRepository;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class PatientServiceHealthIndicator implements HealthIndicator {

    private final PatientRepository patientRepository;
    private final FhirResourceRepository fhirResourceRepository;
    private final AllergyRepository allergyRepository;
    private final MedicalHistoryRepository medicalHistoryRepository;
    private final MedicationRepository medicationRepository;

    public PatientServiceHealthIndicator(
            PatientRepository patientRepository,
            FhirResourceRepository fhirResourceRepository,
            AllergyRepository allergyRepository,
            MedicalHistoryRepository medicalHistoryRepository,
            MedicationRepository medicationRepository) {
        this.patientRepository = patientRepository;
        this.fhirResourceRepository = fhirResourceRepository;
        this.allergyRepository = allergyRepository;
        this.medicalHistoryRepository = medicalHistoryRepository;
        this.medicationRepository = medicationRepository;
    }

    @Override
    public Health health() {
        try {
            long patientCount = patientRepository.count();
            long fhirResourceCount = fhirResourceRepository.count();
            long allergyCount = allergyRepository.count();
            long medicalHistoryCount = medicalHistoryRepository.count();
            long medicationCount = medicationRepository.count();

            return Health.up()
                    .withDetail("timestamp", LocalDateTime.now())
                    .withDetail("patients", patientCount)
                    .withDetail("fhirResources", fhirResourceCount)
                    .withDetail("allergies", allergyCount)
                    .withDetail("medicalHistories", medicalHistoryCount)
                    .withDetail("medications", medicationCount)
                    .build();
        } catch (Exception ex) {
            return Health.down(ex)
                    .withDetail("component", "patient-service-health")
                    .build();
        }
    }
}
