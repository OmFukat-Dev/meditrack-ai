package com.meditrack.vitals.repository;

import com.meditrack.vitals.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
    
    Optional<Patient> findByPatientIdentifier(String patientIdentifier);
    
    boolean existsByPatientIdentifier(String patientIdentifier);
}
