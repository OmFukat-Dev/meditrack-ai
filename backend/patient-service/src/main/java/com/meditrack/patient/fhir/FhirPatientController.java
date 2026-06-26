package com.meditrack.patient.fhir;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/fhir/patients")
@CrossOrigin(origins = "*")
public class FhirPatientController {

    @Autowired
    private FhirPatientService fhirPatientService;

    @PostMapping("/{patientId}")
    @Counted(value = "fhir.patient.create", description = "Number of FHIR patients created")
    @Timed(value = "fhir.patient.create.time", description = "Time taken to create FHIR patient")
    public ResponseEntity<Patient> createFhirPatient(@PathVariable Long patientId,
                                                     @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            Patient fhirPatient = fhirPatientService.createFhirPatient(patientId, userId != null ? userId : "system");
            return ResponseEntity.status(HttpStatus.CREATED).body(fhirPatient);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{patientId}")
    @Counted(value = "fhir.patient.update", description = "Number of FHIR patients updated")
    @Timed(value = "fhir.patient.update.time", description = "Time taken to update FHIR patient")
    public ResponseEntity<Patient> updateFhirPatient(@PathVariable Long patientId,
                                                     @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            Patient fhirPatient = fhirPatientService.updateFhirPatient(patientId, userId != null ? userId : "system");
            return ResponseEntity.ok(fhirPatient);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{patientId}")
    @Counted(value = "fhir.patient.read", description = "Number of FHIR patients read")
    @Timed(value = "fhir.patient.read.time", description = "Time taken to read FHIR patient")
    public ResponseEntity<Patient> getFhirPatient(@PathVariable Long patientId) {
        try {
            Patient fhirPatient = fhirPatientService.getFhirPatient(patientId);
            return ResponseEntity.ok(fhirPatient);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    @Counted(value = "fhir.patient.read.all", description = "Number of FHIR patients read all")
    @Timed(value = "fhir.patient.read.all.time", description = "Time taken to read all FHIR patients")
    public ResponseEntity<Bundle> getAllFhirPatients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        Bundle bundle = fhirPatientService.getAllFhirPatients(page, size);
        return ResponseEntity.ok(bundle);
    }

    @DeleteMapping("/{patientId}")
    @Counted(value = "fhir.patient.delete", description = "Number of FHIR patients deleted")
    @Timed(value = "fhir.patient.delete.time", description = "Time taken to delete FHIR patient")
    public ResponseEntity<Void> deleteFhirPatient(@PathVariable Long patientId) {
        try {
            fhirPatientService.deleteFhirPatient(patientId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{patientId}/bundle")
    @Counted(value = "fhir.patient.bundle.read", description = "Number of FHIR patient bundles read")
    @Timed(value = "fhir.patient.bundle.read.time", description = "Time taken to read FHIR patient bundle")
    public ResponseEntity<Bundle> getPatientBundle(@PathVariable Long patientId) {
        try {
            Bundle bundle = fhirPatientService.getPatientBundle(patientId);
            return ResponseEntity.ok(bundle);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/search")
    @Counted(value = "fhir.patient.search", description = "Number of FHIR patient searches")
    @Timed(value = "fhir.patient.search.time", description = "Time taken to search FHIR patients")
    public ResponseEntity<Bundle> searchFhirPatients(
            @RequestParam(required = false) String identifier,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String birthdate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {

        try {
            LocalDate birthDate = birthdate != null ? LocalDate.parse(birthdate) : null;
            Bundle bundle = fhirPatientService.searchPatients(identifier, name, gender, birthDate, page, size);
            return ResponseEntity.ok(bundle);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/validate")
    @Counted(value = "fhir.patient.validate", description = "Number of FHIR patient validations")
    @Timed(value = "fhir.patient.validate.time", description = "Time taken to validate FHIR patient")
    public ResponseEntity<Boolean> validateFhirPatient(@RequestBody Patient fhirPatient) {
        boolean isValid = fhirPatientService.validateFhirPatient(fhirPatient);
        return ResponseEntity.ok(isValid);
    }

    @GetMapping("/metadata")
    @Counted(value = "fhir.patient.metadata", description = "Number of FHIR patient metadata requests")
    @Timed(value = "fhir.patient.metadata.time", description = "Time taken to get FHIR patient metadata")
    public ResponseEntity<String> getPatientMetadata() {
        String metadata = "{\n" +
                "  \"resourceType\": \"Patient\",\n" +
                "  \"profile\": \"http://meditrack.ai/fhir/StructureDefinition/patient\",\n" +
                "  \"version\": \"4.0.1\",\n" +
                "  \"name\": \"MediTrack Patient\",\n" +
                "  \"status\": \"active\",\n" +
                "  \"experimental\": false,\n" +
                "  \"publisher\": \"MediTrack AI\",\n" +
                "  \"description\": \"MediTrack AI Patient profile with healthcare-specific extensions\",\n" +
                "  \"elements\": [\n" +
                "    {\"path\": \"identifier\", \"min\": 1, \"max\": 1},\n" +
                "    {\"path\": \"name\", \"min\": 1, \"max\": 1},\n" +
                "    {\"path\": \"gender\", \"min\": 1, \"max\": 1},\n" +
                "    {\"path\": \"birthDate\", \"min\": 1, \"max\": 1},\n" +
                "    {\"path\": \"telecom\", \"min\": 0, \"max\": -1},\n" +
                "    {\"path\": \"address\", \"min\": 0, \"max\": -1},\n" +
                "    {\"path\": \"extension\", \"min\": 0, \"max\": -1}\n" +
                "  ]\n" +
                "}";

        return ResponseEntity.ok().header("Content-Type", "application/fhir+json").body(metadata);
    }

    @GetMapping("/metadata/capability")
    @Counted(value = "fhir.patient.capability", description = "Number of FHIR patient capability requests")
    @Timed(value = "fhir.patient.capability.time", description = "Time taken to get FHIR patient capability")
    public ResponseEntity<String> getCapabilityStatement() {
        String capabilityStatement = "{\n" +
                "  \"resourceType\": \"CapabilityStatement\",\n" +
                "  \"status\": \"active\",\n" +
                "  \"date\": \"" + java.time.LocalDateTime.now() + "\",\n" +
                "  \"publisher\": \"MediTrack AI\",\n" +
                "  \"software\": {\n" +
                "    \"name\": \"MediTrack Patient Service\",\n" +
                "    \"version\": \"1.0.0\"\n" +
                "  },\n" +
                "  \"implementation\": {\n" +
                "    \"description\": \"MediTrack AI Patient Service FHIR Implementation\",\n" +
                "    \"url\": \"http://localhost:8082/api/fhir/patients\"\n" +
                "  },\n" +
                "  \"fhirVersion\": \"4.0.1\",\n" +
                "  \"format\": [\"application/fhir+json\"],\n" +
                "  \"rest\": {\n" +
                "    \"security\": [{\"cors\": true, \"service\": [{\"coding\": [{\"system\": \"http://terminology.hl7.org/CodeSystem/restful-security-service\", \"code\": \"patients\", \"display\": \"Patients\"}]}]}],\n" +
                "    \"resource\": [{\n" +
                "      \"type\": \"Patient\",\n" +
                "      \"profile\": \"http://meditrack.ai/fhir/StructureDefinition/patient\",\n" +
                "      \"interaction\": [\n" +
                "        {\"code\": \"read\", \"documentation\": \"Implemented\"},\n" +
                "        {\"code\": \"create\", \"documentation\": \"Implemented\"},\n" +
                "        {\"code\": \"update\", \"documentation\": \"Implemented\"},\n" +
                "        {\"code\": \"delete\", \"documentation\": \"Implemented\"},\n" +
                "        {\"code\": \"search-type\", \"documentation\": \"Implemented\"}\n" +
                "      ],\n" +
                "      \"searchParam\": [\n" +
                "        {\"name\": \"identifier\", \"type\": \"token\", \"documentation\": \"Patient identifier\"},\n" +
                "        {\"name\": \"name\", \"type\": \"string\", \"documentation\": \"Patient name\"},\n" +
                "        {\"name\": \"gender\", \"type\": \"token\", \"documentation\": \"Patient gender\"},\n" +
                "        {\"name\": \"birthdate\", \"type\": \"date\", \"documentation\": \"Patient birth date\"}\n" +
                "      ]\n" +
                "    }]\n" +
                "  }\n" +
                "}";

        return ResponseEntity.ok().header("Content-Type", "application/fhir+json").body(capabilityStatement);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleFhirException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .header("Content-Type", "application/fhir+json")
            .body("{\"resourceType\": \"OperationOutcome\", \"issue\": [{\"severity\": \"error\", \"code\": \"processing\", \"diagnostics\": \"" + e.getMessage() + "\"}]}");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<String> handleAccessDeniedException(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .header("Content-Type", "application/fhir+json")
            .body("{\"resourceType\": \"OperationOutcome\", \"issue\": [{\"severity\": \"error\", \"code\": \"forbidden\", \"diagnostics\": \"" + e.getMessage() + "\"}]}");
    }
}
