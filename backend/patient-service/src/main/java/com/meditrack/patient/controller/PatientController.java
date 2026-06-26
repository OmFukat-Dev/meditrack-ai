package com.meditrack.patient.controller;

import com.meditrack.patient.dto.PatientRequest;
import com.meditrack.patient.dto.PatientResponse;
import com.meditrack.patient.service.PatientService;
import com.meditrack.patient.service.AuditService;
import com.meditrack.patient.entity.AuditLog;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
@CrossOrigin(origins = "*")
public class PatientController {
    
    @Autowired
    private PatientService patientService;
    
    @Autowired
    private AuditService auditService;
    
    // CRUD Operations
    @PostMapping
    @Counted(value = "patient.create", description = "Number of patients created")
    @Timed(value = "patient.create.time", description = "Time taken to create patient")
    public ResponseEntity<PatientResponse> createPatient(@Valid @RequestBody PatientRequest request,
                                                 @RequestHeader(value = "X-User-Email", required = false) String userEmail,
                                                 @RequestHeader(value = "X-User-Id", required = false) String userId) {
        String actor = firstNonBlank(userEmail, userId, "system");
        PatientResponse response = patientService.createPatient(request, actor);
        auditService.logAction(response.getId(), "CREATE", "Patient created", actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{id}")
    @Counted(value = "patient.read", description = "Number of patients read")
    @Timed(value = "patient.read.time", description = "Time taken to read patient")
    public ResponseEntity<PatientResponse> getPatient(@PathVariable Long id) {
        PatientResponse response = patientService.getPatientById(id);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/identifier/{patientIdentifier}")
    @Counted(value = "patient.read.identifier", description = "Number of patients read by identifier")
    @Timed(value = "patient.read.identifier.time", description = "Time taken to read patient by identifier")
    public ResponseEntity<PatientResponse> getPatientByIdentifier(@PathVariable String patientIdentifier) {
        PatientResponse response = patientService.getPatientByIdentifier(patientIdentifier);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping
    @Counted(value = "patient.read.all", description = "Number of times all patients read")
    @Timed(value = "patient.read.all.time", description = "Time taken to read all patients")
    public ResponseEntity<Page<PatientResponse>> getAllPatients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "lastName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        Page<PatientResponse> patients = patientService.getAllPatients(page, size, sortBy, sortDir);
        return ResponseEntity.ok(patients);
    }
    
    @PutMapping("/{id}")
    @Counted(value = "patient.update", description = "Number of patients updated")
    @Timed(value = "patient.update.time", description = "Time taken to update patient")
    public ResponseEntity<PatientResponse> updatePatient(@PathVariable Long id,
                                                   @Valid @RequestBody PatientRequest request,
                                                   @RequestHeader(value = "X-User-Email", required = false) String userEmail,
                                                   @RequestHeader(value = "X-User-Id", required = false) String userId) {
        String actor = firstNonBlank(userEmail, userId, "system");
        PatientResponse response = patientService.updatePatient(id, request, actor);
        auditService.logAction(id, "UPDATE", "Patient updated: " + request.getDepartment(), actor);
        return ResponseEntity.ok(response);
    }
    
    @PatchMapping("/{id}/condition")
    @Counted(value = "patient.update.condition", description = "Number of patient condition updates")
    @Timed(value = "patient.update.condition.time", description = "Time taken to update patient condition")
    public ResponseEntity<PatientResponse> updatePatientCondition(@PathVariable Long id,
                                                   @RequestBody java.util.Map<String, String> payload,
                                                   @RequestHeader(value = "X-User-Email", required = false) String userEmail,
                                                   @RequestHeader(value = "X-User-Id", required = false) String userId) {
        String actor = firstNonBlank(userEmail, userId, "system");
        String condition = payload.get("condition");
        if (condition == null || condition.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        PatientResponse response = patientService.updatePatientCondition(id, condition, actor);
        auditService.logAction(id, "UPDATE_CONDITION", "Patient condition updated to: " + condition, actor);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    @Counted(value = "patient.delete", description = "Number of patients deleted")
    @Timed(value = "patient.delete.time", description = "Time taken to delete patient")
    public ResponseEntity<Void> deletePatient(@PathVariable Long id) {
        patientService.deletePatient(id);
        return ResponseEntity.noContent().build();
    }
    
    @PutMapping("/{id}/activate")
    @Counted(value = "patient.activate", description = "Number of patients activated")
    @Timed(value = "patient.activate.time", description = "Time taken to activate patient")
    public ResponseEntity<Void> activatePatient(@PathVariable Long id) {
        patientService.activatePatient(id);
        return ResponseEntity.ok().build();
    }
    
    // Search Operations
    @GetMapping("/search")
    @Counted(value = "patient.search", description = "Number of patient searches")
    @Timed(value = "patient.search.time", description = "Time taken to search patients")
    public ResponseEntity<Page<PatientResponse>> searchPatients(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Page<PatientResponse> patients = patientService.searchPatients(query, page, size);
        return ResponseEntity.ok(patients);
    }
    
    @GetMapping("/search/firstName")
    @Counted(value = "patient.search.firstName", description = "Number of patient first name searches")
    @Timed(value = "patient.search.firstName.time", description = "Time taken to search patients by first name")
    public ResponseEntity<Page<PatientResponse>> searchPatientsByFirstName(
            @RequestParam String firstName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Page<PatientResponse> patients = patientService.searchPatientsByFirstName(firstName, page, size);
        return ResponseEntity.ok(patients);
    }
    
    @GetMapping("/search/lastName")
    @Counted(value = "patient.search.lastName", description = "Number of patient last name searches")
    @Timed(value = "patient.search.lastName.time", description = "Time taken to search patients by last name")
    public ResponseEntity<Page<PatientResponse>> searchPatientsByLastName(
            @RequestParam String lastName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Page<PatientResponse> patients = patientService.searchPatientsByLastName(lastName, page, size);
        return ResponseEntity.ok(patients);
    }
    
    // Filter Operations
    @GetMapping("/filter/gender/{gender}")
    @Counted(value = "patient.filter.gender", description = "Number of patients filtered by gender")
    @Timed(value = "patient.filter.gender.time", description = "Time taken to filter patients by gender")
    public ResponseEntity<Page<PatientResponse>> getPatientsByGender(
            @PathVariable String gender,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Page<PatientResponse> patients = patientService.getPatientsByGender(gender, page, size);
        return ResponseEntity.ok(patients);
    }
    
    @GetMapping("/filter/blood-type/{bloodType}")
    @Counted(value = "patient.filter.bloodType", description = "Number of patients filtered by blood type")
    @Timed(value = "patient.filter.bloodType.time", description = "Time taken to filter patients by blood type")
    public ResponseEntity<Page<PatientResponse>> getPatientsByBloodType(
            @PathVariable String bloodType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Page<PatientResponse> patients = patientService.getPatientsByBloodType(bloodType, page, size);
        return ResponseEntity.ok(patients);
    }
    
    @GetMapping("/filter/age-range")
    @Counted(value = "patient.filter.ageRange", description = "Number of patients filtered by age range")
    @Timed(value = "patient.filter.ageRange.time", description = "Time taken to filter patients by age range")
    public ResponseEntity<Page<PatientResponse>> getPatientsByAgeRange(
            @RequestParam int minAge,
            @RequestParam int maxAge,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Page<PatientResponse> patients = patientService.getPatientsByAgeRange(minAge, maxAge, page, size);
        return ResponseEntity.ok(patients);
    }
    
    // Business Logic Endpoints
    @GetMapping("/{id}/vital-monitoring-eligible")
    @Counted(value = "patient.check.vitalMonitoring", description = "Number of vital monitoring eligibility checks")
    @Timed(value = "patient.check.vitalMonitoring.time", description = "Time taken to check vital monitoring eligibility")
    public ResponseEntity<Boolean> checkVitalMonitoringEligibility(@PathVariable Long id) {
        boolean eligible = patientService.isPatientEligibleForVitalMonitoring(id);
        return ResponseEntity.ok(eligible);
    }
    
    @GetMapping("/emergency-contact/{emergencyContactPhone}")
    @Counted(value = "patient.search.emergencyContact", description = "Number of emergency contact searches")
    @Timed(value = "patient.search.emergencyContact.time", description = "Time taken to search by emergency contact")
    public ResponseEntity<List<PatientResponse>> getPatientsByEmergencyContact(
            @PathVariable String emergencyContactPhone) {
        
        List<PatientResponse> patients = patientService.getPatientsByEmergencyContact(emergencyContactPhone);
        return ResponseEntity.ok(patients);
    }
    
    // Statistics and Reporting Endpoints
    @GetMapping("/statistics/total-active")
    @Counted(value = "patient.stats.totalActive", description = "Number of total active patient requests")
    @Timed(value = "patient.stats.totalActive.time", description = "Time taken to get total active patients")
    public ResponseEntity<Long> getTotalActivePatients() {
        long total = patientService.getTotalActivePatients();
        return ResponseEntity.ok(total);
    }
    
    @GetMapping("/statistics/gender/{gender}")
    @Counted(value = "patient.stats.gender", description = "Number of gender statistics requests")
    @Timed(value = "patient.stats.gender.time", description = "Time taken to get gender statistics")
    public ResponseEntity<Long> getPatientsByGender(@PathVariable String gender) {
        long count = patientService.getPatientsByGender(gender);
        return ResponseEntity.ok(count);
    }
    
    @GetMapping("/statistics/blood-type/{bloodType}")
    @Counted(value = "patient.stats.bloodType", description = "Number of blood type statistics requests")
    @Timed(value = "patient.stats.bloodType.time", description = "Time taken to get blood type statistics")
    public ResponseEntity<Long> getPatientsByBloodType(@PathVariable String bloodType) {
        long count = patientService.getPatientsByBloodType(bloodType);
        return ResponseEntity.ok(count);
    }
    
    // Audit Endpoints
    @GetMapping("/{id}/audit")
    public ResponseEntity<List<AuditLog>> getPatientAuditLogs(@PathVariable Long id) {
        List<AuditLog> logs = auditService.getLogsForPatient(id);
        return ResponseEntity.ok(logs);
    }
    
    // Exception Handler
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<String> handleAccessDeniedException(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "system";
    }
}
