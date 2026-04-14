package com.meditrack.patient.service;

import com.meditrack.patient.dto.PatientRequest;
import com.meditrack.patient.dto.PatientResponse;
import com.meditrack.patient.entity.Patient;
import com.meditrack.patient.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class PatientService {
    
    @Autowired
    private PatientRepository patientRepository;
    
    // CRUD Operations
    public PatientResponse createPatient(PatientRequest request, String createdBy) {
        // Check if patient identifier already exists
        if (patientRepository.existsByPatientIdentifier(request.getPatientIdentifier())) {
            throw new IllegalArgumentException("Patient with identifier " + request.getPatientIdentifier() + " already exists");
        }
        
        Patient patient = convertToEntity(request);
        patient.setCreatedBy(createdBy);
        patient.setUpdatedBy(createdBy);
        
        Patient savedPatient = patientRepository.save(patient);
        return convertToResponse(savedPatient);
    }
    
    public PatientResponse getPatientById(Long id) {
        Patient patient = patientRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Patient not found with id: " + id));
        return convertToResponse(patient);
    }
    
    public PatientResponse getPatientByIdentifier(String patientIdentifier) {
        Patient patient = patientRepository.findByPatientIdentifier(patientIdentifier)
            .orElseThrow(() -> new IllegalArgumentException("Patient not found with identifier: " + patientIdentifier));
        return convertToResponse(patient);
    }
    
    public Page<PatientResponse> getAllPatients(int page, int size, String sortBy, String sortDir) {
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<Patient> patients = patientRepository.findByIsActive(true, pageable);
        return patients.map(this::convertToResponse);
    }
    
    public PatientResponse updatePatient(Long id, PatientRequest request, String updatedBy) {
        Patient existingPatient = patientRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Patient not found with id: " + id));
        
        // Check if patient identifier is being changed and if new one already exists
        if (!existingPatient.getPatientIdentifier().equals(request.getPatientIdentifier()) &&
            patientRepository.existsByPatientIdentifier(request.getPatientIdentifier())) {
            throw new IllegalArgumentException("Patient with identifier " + request.getPatientIdentifier() + " already exists");
        }
        
        updateEntityFromRequest(existingPatient, request);
        existingPatient.setUpdatedBy(updatedBy);
        
        Patient updatedPatient = patientRepository.save(existingPatient);
        return convertToResponse(updatedPatient);
    }
    
    public void deletePatient(Long id) {
        Patient patient = patientRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Patient not found with id: " + id));
        
        // Soft delete
        patient.setIsActive(false);
        patientRepository.save(patient);
    }
    
    public void activatePatient(Long id) {
        Patient patient = patientRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Patient not found with id: " + id));
        
        patient.setIsActive(true);
        patientRepository.save(patient);
    }
    
    // Search Operations
    public Page<PatientResponse> searchPatients(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "lastName"));
        Page<Patient> patients = patientRepository.searchByNameOrIdentifier(query, pageable);
        return patients.map(this::convertToResponse);
    }
    
    public Page<PatientResponse> searchPatientsByFirstName(String firstName, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "firstName"));
        Page<Patient> patients = patientRepository.findByFirstNameContainingIgnoreCase(firstName, pageable);
        return patients.map(this::convertToResponse);
    }
    
    public Page<PatientResponse> searchPatientsByLastName(String lastName, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "lastName"));
        Page<Patient> patients = patientRepository.findByLastNameContainingIgnoreCase(lastName, pageable);
        return patients.map(this::convertToResponse);
    }
    
    // Filter Operations
    public Page<PatientResponse> getPatientsByGender(String gender, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "lastName"));
        Page<Patient> patients = patientRepository.findByGenderAndIsActive(gender, true, pageable);
        return patients.map(this::convertToResponse);
    }
    
    public Page<PatientResponse> getPatientsByBloodType(String bloodType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "lastName"));
        Page<Patient> patients = patientRepository.findByBloodTypeAndIsActive(bloodType, true, pageable);
        return patients.map(this::convertToResponse);
    }
    
    public Page<PatientResponse> getPatientsByAgeRange(int minAge, int maxAge, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "lastName"));
        List<Patient> patients = patientRepository.findByAgeRange(minAge, maxAge);
        
        // Convert to page for consistency
        int start = page * size;
        int end = Math.min(start + size, patients.size());
        List<Patient> pageContent = start < patients.size() ? patients.subList(start, end) : List.of();
        
        return new org.springframework.data.domain.PageImpl<>(pageContent.stream().map(this::convertToResponse).collect(Collectors.toList()),
            pageable, patients.size());
    }
    
    // Utility Methods
    private Patient convertToEntity(PatientRequest request) {
        Patient patient = new Patient();
        patient.setPatientIdentifier(request.getPatientIdentifier());
        patient.setFirstName(request.getFirstName());
        patient.setLastName(request.getLastName());
        patient.setDateOfBirth(request.getDateOfBirth());
        patient.setGender(request.getGender());
        patient.setBloodType(request.getBloodType());
        patient.setPhoneNumber(request.getPhoneNumber());
        patient.setEmail(request.getEmail());
        patient.setAddress(request.getAddress());
        patient.setEmergencyContactName(request.getEmergencyContactName());
        patient.setEmergencyContactPhone(request.getEmergencyContactPhone());
        patient.setIsActive(true);
        return patient;
    }
    
    private void updateEntityFromRequest(Patient patient, PatientRequest request) {
        patient.setPatientIdentifier(request.getPatientIdentifier());
        patient.setFirstName(request.getFirstName());
        patient.setLastName(request.getLastName());
        patient.setDateOfBirth(request.getDateOfBirth());
        patient.setGender(request.getGender());
        patient.setBloodType(request.getBloodType());
        patient.setPhoneNumber(request.getPhoneNumber());
        patient.setEmail(request.getEmail());
        patient.setAddress(request.getAddress());
        patient.setEmergencyContactName(request.getEmergencyContactName());
        patient.setEmergencyContactPhone(request.getEmergencyContactPhone());
    }
    
    private PatientResponse convertToResponse(Patient patient) {
        PatientResponse response = new PatientResponse();
        response.setId(patient.getId());
        response.setPatientIdentifier(patient.getPatientIdentifier());
        response.setFirstName(patient.getFirstName());
        response.setLastName(patient.getLastName());
        response.setDateOfBirth(patient.getDateOfBirth());
        response.setGender(patient.getGender());
        response.setBloodType(patient.getBloodType());
        response.setPhoneNumber(patient.getPhoneNumber());
        response.setEmail(patient.getEmail());
        response.setAddress(patient.getAddress());
        response.setEmergencyContactName(patient.getEmergencyContactName());
        response.setEmergencyContactPhone(patient.getEmergencyContactPhone());
        response.setCreatedBy(patient.getCreatedBy());
        response.setUpdatedBy(patient.getUpdatedBy());
        response.setIsActive(patient.getIsActive());
        response.setCreatedAt(patient.getCreatedAt());
        response.setUpdatedAt(patient.getUpdatedAt());
        
        // Computed fields
        response.setFullName(patient.getFullName());
        response.setAge(patient.getAge());
        
        return response;
    }
    
    // Business logic methods
    public boolean isPatientEligibleForVitalMonitoring(Long patientId) {
        Optional<Patient> patient = patientRepository.findById(patientId);
        return patient.map(p -> p.getIsActive() && p.getAge() >= 0).orElse(false);
    }
    
    public List<PatientResponse> getPatientsByEmergencyContact(String emergencyContactPhone) {
        List<Patient> patients = patientRepository.findByEmergencyContactPhone(emergencyContactPhone);
        return patients.stream().map(this::convertToResponse).collect(Collectors.toList());
    }
    
    // Statistics and reporting
    public long getTotalActivePatients() {
        return patientRepository.countByIsActive(true);
    }
    
    public long getPatientsByGender(String gender) {
        return patientRepository.countByGender(gender);
    }
    
    public long getPatientsByBloodType(String bloodType) {
        return patientRepository.countByBloodType(bloodType);
    }
}
