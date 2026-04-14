package com.meditrack.patient.fhir;

import ca.uhn.fhir.model.api.Bundle;
import ca.uhn.fhir.model.api.Patient;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/fhir/patients")
@CrossOrigin(origins = "*")
public class FhirPatientController {
    
    @Autowired
    private FhirPatientService fhirPatientService;
    
    // FHIR Patient Operations
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
    public ResponseEntity<Bundle> getAllFhirPatients() {
        Bundle bundle = fhirPatientService.getAllFhirPatients();
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
    
    // FHIR Bundle Operations
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
    
    // FHIR Search Operations
    @GetMapping("/search")
    @Counted(value = "fhir.patient.search", description = "Number of FHIR patient searches")
    @Timed(value = "fhir.patient.search.time", description = "Time taken to search FHIR patients")
    public ResponseEntity<Bundle> searchFhirPatients(
            @RequestParam(required = false) String identifier,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String birthdate) {
        
        try {
            LocalDate birthDate = birthdate != null ? LocalDate.parse(birthdate) : null;
            Bundle bundle = fhirPatientService.searchPatients(identifier, name, gender, birthDate);
            return ResponseEntity.ok(bundle);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    // FHIR Validation Operations
    @PostMapping("/validate")
    @Counted(value = "fhir.patient.validate", description = "Number of FHIR patient validations")
    @Timed(value = "fhir.patient.validate.time", description = "Time taken to validate FHIR patient")
    public ResponseEntity<Boolean> validateFhirPatient(@RequestBody Patient fhirPatient) {
        boolean isValid = fhirPatientService.validateFhirPatient(fhirPatient);
        return ResponseEntity.ok(isValid);
    }
    
    // FHIR Metadata Operations
    @GetMapping("/metadata")
    @Counted(value = "fhir.patient.metadata", description = "Number of FHIR patient metadata requests")
    @Timed(value = "fhir.patient.metadata.time", description = "Time taken to get FHIR patient metadata")
    public ResponseEntity<String> getPatientMetadata() {
        // Return FHIR Patient StructureDefinition metadata
        String metadata = "{\n" +
                "  \"resourceType\": \"Patient\",\n" +
                "  \"profile\": \"http://meditrack.ai/fhir/StructureDefinition/patient\",\n" +
                "  \"version\": \"4.0.1\",\n" +
                "  \"name\": \"MediTrack Patient\",\n" +
                "  \"status\": \"active\",\n" +
                "  \"experimental\": false,\n" +
                "  \"publisher\": \"MediTrack AI\",\n" +
                "  \"description\": \"MediTrack AI Patient profile with healthcare-specific extensions\"\",\n" +
                "  \"elements\": [\n" +
                "    {\n" +
                "      \"path\": \"identifier\",\n" +
                "      \"min\": 1,\n" +
                "      \"max\": 1\n" +
                "    },\n" +
                "    {\n" +
                "      \"path\": \"name\",\n" +
                "      \"min\": 1,\n" +
                "      \"max\": 1\n" +
                "    },\n" +
                "    {\n" +
                "      \"path\": \"gender\",\n" +
                "      \"min\": 1,\n" +
                "      \"max\": 1\n" +
                "    },\n" +
                "    {\n" +
                "      \"path\": \"birthDate\",\n" +
                "      \"min\": 1,\n" +
                "      \"max\": 1\n" +
                "    },\n" +
                "    {\n" +
                "      \"path\": \"telecom\",\n" +
                "      \"min\": 0,\n" +
                "      \"max\": -1\n" +
                "    },\n" +
                "    {\n" +
                "      \"path\": \"address\",\n" +
                "      \"min\": 0,\n" +
                "      \"max\": -1\n" +
                "    },\n" +
                "    {\n" +
                "      \"path\": \"extension\",\n" +
                "      \"min\": 0,\n" +
                "      \"max\": -1\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        
        return ResponseEntity.ok().header("Content-Type", "application/fhir+json").body(metadata);
    }
    
    // FHIR Capability Statement
    @GetMapping("/metadata/capability")
    @Counted(value = "fhir.patient.capability", description = "Number of FHIR patient capability requests")
    @Timed(value = "fhir.patient.capability.time", description = "Time taken to get FHIR patient capability")
    public ResponseEntity<String> getCapabilityStatement() {
        String capabilityStatement = "{\n" +
                "  \"resourceType\": \"CapabilityStatement\",\n" +
                "  \"status\": \"active\",\n" +
                "  \"date\": \"" + java.time.LocalDateTime.now().toString() + "\",\n" +
                "  \"publisher\": \"MediTrack AI\",\n" +
                "  \"software\": {\n" +
                "    \"name\": \"MediTrack Patient Service\",\n" +
                "    \"version\": \"1.0.0\"\n" +
                "  },\n" +
                "  \"implementation\": {\n" +
                "    \"description\": \"MediTrack AI Patient Service FHIR Implementation\",\n" +
                "    \"url\": \"http://localhost:8081/api/fhir/patients\"\n" +
                "  },\n" +
                "  \"fhirVersion\": \"4.0.1\",\n" +
                "  \"format\": [\"application/fhir+json\"],\n" +
                "  \"rest\": {\n" +
                "    \"security\": [\n" +
                "      {\n" +
                "        \"cors\": true,\n" +
                "        \"service\": [\n" +
                "          {\n" +
                "            \"coding\": [\n" +
                "              {\n" +
                "                \"system\": \"http://terminology.hl7.org/CodeSystem/restful-security-service\",\n" +
                "                \"code\": \"patients\",\n" +
                "                \"display\": \"Patients\"\n" +
                "              }\n" +
                "            ]\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    ],\n" +
                "    \"resource\": [\n" +
                "      {\n" +
                "        \"type\": \"Patient\",\n" +
                "        \"profile\": \"http://meditrack.ai/fhir/StructureDefinition/patient\",\n" +
                "        \"interaction\": [\n" +
                "          {\n" +
                "            \"code\": \"read\",\n" +
                "            \"documentation\": \"Implemented\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"code\": \"create\",\n" +
                "            \"documentation\": \"Implemented\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"code\": \"update\",\n" +
                "            \"documentation\": \"Implemented\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"code\": \"delete\",\n" +
                "            \"documentation\": \"Implemented\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"code\": \"search-type\",\n" +
                "            \"documentation\": \"Implemented\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"searchParam\": [\n" +
                "          {\n" +
                "            \"name\": \"identifier\",\n" +
                "            \"type\": \"token\",\n" +
                "            \"documentation\": \"Patient identifier\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"name\": \"name\",\n" +
                "            \"type\": \"string\",\n" +
                "            \"documentation\": \"Patient name\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"name\": \"gender\",\n" +
                "            \"type\": \"token\",\n" +
                "            \"documentation\": \"Patient gender\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"name\": \"birthdate\",\n" +
                "            \"type\": \"date\",\n" +
                "            \"documentation\": \"Patient birth date\"\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";
        
        return ResponseEntity.ok().header("Content-Type", "application/fhir+json").body(capabilityStatement);
    }
    
    // Exception Handler
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleFhirException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .header("Content-Type", "application/fhir+json")
            .body("{\"resourceType\": \"OperationOutcome\", \"issue\": [{\"severity\": \"error\", \"code\": \"processing\", \"diagnostics\": \"" + e.getMessage() + "\"}]}");
    }
}
