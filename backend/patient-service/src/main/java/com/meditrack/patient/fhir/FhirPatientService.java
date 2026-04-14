package com.meditrack.patient.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meditrack.patient.entity.FhirResource;
import com.meditrack.patient.entity.Patient;
import com.meditrack.patient.repository.FhirResourceRepository;
import com.meditrack.patient.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class FhirPatientService {
    
    @Autowired
    private PatientRepository patientRepository;
    
    @Autowired
    private FhirResourceRepository fhirResourceRepository;
    
    @Autowired
    private FhirContext fhirContext;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // FHIR Patient Resource Operations
    public org.hl7.fhir.r4.model.Patient createFhirPatient(Long patientId, String createdBy) {
        Patient patient = patientRepository.findById(patientId)
            .orElseThrow(() -> new IllegalArgumentException("Patient not found with id: " + patientId));
        
        org.hl7.fhir.r4.model.Patient fhirPatient = convertToFhirPatient(patient);
        
        // Save FHIR resource to database
        FhirResource fhirResource = new FhirResource(
            patient, 
            FhirResource.FhirResourceType.PATIENT, 
            fhirPatient.getIdElement().getIdPart(),
            serializeFhirResource(fhirPatient)
        );
        fhirResource.setCreatedBy(createdBy);
        fhirResourceRepository.save(fhirResource);
        
        return fhirPatient;
    }
    
    public org.hl7.fhir.r4.model.Patient updateFhirPatient(Long patientId, String updatedBy) {
        Patient patient = patientRepository.findById(patientId)
            .orElseThrow(() -> new IllegalArgumentException("Patient not found with id: " + patientId));
        
        org.hl7.fhir.r4.model.Patient fhirPatient = convertToFhirPatient(patient);
        
        // Update existing FHIR resource or create new one
        Optional<FhirResource> existingResource = fhirResourceRepository
            .findByPatientIdAndResourceIdAndResourceVersion(patientId, fhirPatient.getIdElement().getIdPart(), "1");
        
        if (existingResource.isPresent()) {
            FhirResource resource = existingResource.get();
            resource.setResourceData(serializeFhirResource(fhirPatient));
            resource.setUpdatedBy(updatedBy);
            fhirResourceRepository.save(resource);
        } else {
            FhirResource fhirResource = new FhirResource(
                patient, 
                FhirResource.FhirResourceType.PATIENT, 
                fhirPatient.getIdElement().getIdPart(),
                serializeFhirResource(fhirPatient)
            );
            fhirResource.setCreatedBy(updatedBy);
            fhirResourceRepository.save(fhirResource);
        }
        
        return fhirPatient;
    }
    
    public org.hl7.fhir.r4.model.Patient getFhirPatient(Long patientId) {
        Patient patient = patientRepository.findById(patientId)
            .orElseThrow(() -> new IllegalArgumentException("Patient not found with id: " + patientId));
        
        // Try to get latest FHIR patient resource
        Optional<FhirResource> fhirResource = fhirResourceRepository
            .findLatestVersionByPatientIdAndResourceTypeAndResourceId(
                patientId, 
                FhirResource.FhirResourceType.PATIENT, 
                patient.getPatientIdentifier()
            );
        
        if (fhirResource.isPresent()) {
            return deserializeFhirResource(fhirResource.get().getResourceData(), org.hl7.fhir.r4.model.Patient.class);
        } else {
            // Create FHIR patient from database entity if no FHIR resource exists
            return convertToFhirPatient(patient);
        }
    }
    
    public List<org.hl7.fhir.r4.model.Patient> getAllFhirPatients() {
        List<Patient> patients = patientRepository.findByIsActive(true);
        return patients.stream()
            .map(this::convertToFhirPatient)
            .toList();
    }
    
    public void deleteFhirPatient(Long patientId) {
        Patient patient = patientRepository.findById(patientId)
            .orElseThrow(() -> new IllegalArgumentException("Patient not found with id: " + patientId));
        
        // Soft delete patient and related FHIR resources
        patient.setIsActive(false);
        patientRepository.save(patient);
        
        fhirResourceRepository.deleteByPatientIdAndResourceType(patientId, FhirResource.FhirResourceType.PATIENT);
    }
    
    // FHIR Observation Resource Operations (for vitals)
    public org.hl7.fhir.r4.model.Observation createFhirObservation(Long patientId, 
                                                                      org.hl7.fhir.r4.model.Observation observation, 
                                                                      String createdBy) {
        Patient patient = patientRepository.findById(patientId)
            .orElseThrow(() -> new IllegalArgumentException("Patient not found with id: " + patientId));
        
        // Set patient reference
        Reference patientReference = new Reference("Patient/" + patient.getPatientIdentifier());
        observation.setSubject(patientReference);
        
        // Save FHIR resource
        FhirResource fhirResource = new FhirResource(
            patient, 
            FhirResource.FhirResourceType.OBSERVATION, 
            observation.getIdElement().getIdPart(),
            serializeFhirResource(observation)
        );
        fhirResource.setCreatedBy(createdBy);
        fhirResourceRepository.save(fhirResource);
        
        return observation;
    }
    
    public List<org.hl7.fhir.r4.model.Observation> getFhirObservations(Long patientId) {
        List<FhirResource> fhirResources = fhirResourceRepository
            .findByPatientIdAndResourceType(patientId, FhirResource.FhirResourceType.OBSERVATION);
        
        return fhirResources.stream()
            .map(resource -> deserializeFhirResource(resource.getResourceData(), org.hl7.fhir.r4.model.Observation.class))
            .toList();
    }
    
    // FHIR Condition Resource Operations (for medical history)
    public org.hl7.fhir.r4.model.Condition createFhirCondition(Long patientId, 
                                                              org.hl7.fhir.r4.model.Condition condition, 
                                                              String createdBy) {
        Patient patient = patientRepository.findById(patientId)
            .orElseThrow(() -> new IllegalArgumentException("Patient not found with id: " + patientId));
        
        // Set patient reference
        Reference patientReference = new Reference("Patient/" + patient.getPatientIdentifier());
        condition.setSubject(patientReference);
        
        // Save FHIR resource
        FhirResource fhirResource = new FhirResource(
            patient, 
            FhirResource.FhirResourceType.CONDITION, 
            condition.getIdElement().getIdPart(),
            serializeFhirResource(condition)
        );
        fhirResource.setCreatedBy(createdBy);
        fhirResourceRepository.save(fhirResource);
        
        return condition;
    }
    
    public List<org.hl7.fhir.r4.model.Condition> getFhirConditions(Long patientId) {
        List<FhirResource> fhirResources = fhirResourceRepository
            .findByPatientIdAndResourceType(patientId, FhirResource.FhirResourceType.CONDITION);
        
        return fhirResources.stream()
            .map(resource -> deserializeFhirResource(resource.getResourceData(), org.hl7.fhir.r4.model.Condition.class))
            .toList();
    }
    
    // Utility Methods
    private org.hl7.fhir.r4.model.Patient convertToFhirPatient(Patient patient) {
        org.hl7.fhir.r4.model.Patient fhirPatient = new org.hl7.fhir.r4.model.Patient();
        
        // Set identifier
        Identifier identifier = new Identifier();
        identifier.setSystem("urn:meditrack:patient:id");
        identifier.setValue(patient.getPatientIdentifier());
        fhirPatient.addIdentifier(identifier);
        
        // Set name
        HumanName name = new HumanName();
        name.setFamily(patient.getLastName());
        name.addGivenElement(new StringType(patient.getFirstName()));
        fhirPatient.addName(name);
        
        // Set gender
        try {
            Enumerations.AdministrativeGender fhirGender = Enumerations.AdministrativeGender.fromCode(patient.getGender().toLowerCase());
            fhirPatient.setGender(fhirGender);
        } catch (Exception e) {
            fhirPatient.setGender(Enumerations.AdministrativeGender.UNKNOWN);
        }
        
        // Set birth date
        if (patient.getDateOfBirth() != null) {
            fhirPatient.setBirthDate(Date.from(patient.getDateOfBirth().atStartOfDay().toInstant(java.time.ZoneOffset.UTC)));
        }
        
        // Set contact information
        if (patient.getPhoneNumber() != null || patient.getEmail() != null) {
            ContactPoint contact = new ContactPoint();
            if (patient.getPhoneNumber() != null) {
                ContactPoint phoneContact = new ContactPoint();
                phoneContact.setSystem(ContactPoint.ContactPointSystem.PHONE);
                phoneContact.setValue(patient.getPhoneNumber());
                contact.addTelecom(phoneContact);
            }
            if (patient.getEmail() != null) {
                ContactPoint emailContact = new ContactPoint();
                emailContact.setSystem(ContactPoint.ContactPointSystem.EMAIL);
                emailContact.setValue(patient.getEmail());
                contact.addTelecom(emailContact);
            }
            fhirPatient.addTelecom(contact);
        }
        
        // Set address
        if (patient.getAddress() != null) {
            Address address = new Address();
            address.setUse(Address.AddressUse.HOME);
            address.setType(Address.AddressType.BOTH);
            address.setText(patient.getAddress());
            fhirPatient.addAddress(address);
        }
        
        // Set emergency contact
        if (patient.getEmergencyContactName() != null || patient.getEmergencyContactPhone() != null) {
            ContactComponent emergencyContact = new ContactComponent();
            HumanName emergencyName = new HumanName();
            emergencyName.setText(patient.getEmergencyContactName());
            emergencyContact.setName(emergencyName);
            
            if (patient.getEmergencyContactPhone() != null) {
                ContactPoint emergencyPhone = new ContactPoint();
                emergencyPhone.setSystem(ContactPoint.ContactPointSystem.PHONE);
                emergencyPhone.setValue(patient.getEmergencyContactPhone());
                emergencyContact.addTelecom(emergencyPhone);
            }
            
            fhirPatient.addContact(emergencyContact);
        }
        
        // Set blood type
        if (patient.getBloodType() != null) {
            Extension bloodTypeExtension = new Extension("http://hl7.org/fhir/StructureDefinition/bloodtype", new StringType(patient.getBloodType()));
            fhirPatient.addExtension(bloodTypeExtension);
        }
        
        // Set active status
        fhirPatient.setActive(new BooleanType(patient.getIsActive()));
        
        // Generate ID if not present
        if (fhirPatient.getId() == null || fhirPatient.getId().isEmpty()) {
            fhirPatient.setId(new IdType(UUID.randomUUID().toString()));
        }
        
        return fhirPatient;
    }
    
    private String serializeFhirResource(IBaseResource resource) {
        try {
            return objectMapper.writeValueAsString(resource);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize FHIR resource", e);
        }
    }
    
    private <T extends IBaseResource> T deserializeFhirResource(String resourceData, Class<T> clazz) {
        try {
            return objectMapper.readValue(resourceData, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize FHIR resource", e);
        }
    }
    
    // FHIR Bundle operations for multiple resources
    public Bundle getPatientBundle(Long patientId) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);
        bundle.setTimestamp(LocalDateTime.now());
        
        // Add patient resource
        org.hl7.fhir.r4.model.Patient fhirPatient = getFhirPatient(patientId);
        BundleEntryComponent patientEntry = new BundleEntryComponent();
        patientEntry.setFullUrl("Patient/" + fhirPatient.getId());
        patientEntry.setResource(fhirPatient);
        bundle.addEntry(patientEntry);
        
        // Add condition resources
        List<org.hl7.fhir.r4.model.Condition> conditions = getFhirConditions(patientId);
        for (org.hl7.fhir.r4.model.Condition condition : conditions) {
            BundleEntryComponent conditionEntry = new BundleEntryComponent();
            conditionEntry.setFullUrl("Condition/" + condition.getId());
            conditionEntry.setResource(condition);
            bundle.addEntry(conditionEntry);
        }
        
        // Add observation resources
        List<org.hl7.fhir.r4.model.Observation> observations = getFhirObservations(patientId);
        for (org.hl7.fhir.r4.model.Observation observation : observations) {
            BundleEntryComponent observationEntry = new BundleEntryComponent();
            observationEntry.setFullUrl("Observation/" + observation.getId());
            observationEntry.setResource(observation);
            bundle.addEntry(observationEntry);
        }
        
        return bundle;
    }
    
    // Search operations
    public Bundle searchPatients(String identifier, String name, String gender, LocalDate birthDate) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);
        bundle.setTotal(patientRepository.advancedSearch(name, name, gender, null, null, null, null, null, null).getTotalElements());
        
        List<Patient> patients = patientRepository.advancedSearch(name, name, gender, null, null, null, null, null, null).getContent();
        
        for (Patient patient : patients) {
            org.hl7.fhir.r4.model.Patient fhirPatient = convertToFhirPatient(patient);
            BundleEntryComponent entry = new BundleEntryComponent();
            entry.setResource(fhirPatient);
            bundle.addEntry(entry);
        }
        
        return bundle;
    }
    
    // Validation methods
    public boolean validateFhirPatient(org.hl7.fhir.r4.model.Patient fhirPatient) {
        try {
            // Basic validation
            if (fhirPatient.getName() == null || fhirPatient.getName().isEmpty()) {
                return false;
            }
            
            HumanName name = fhirPatient.getName().get(0);
            if (name.getFamily() == null || name.getGiven() == null || name.getGiven().isEmpty()) {
                return false;
            }
            
            // Gender validation
            if (fhirPatient.getGender() == null) {
                return false;
            }
            
            // Birth date validation
            if (fhirPatient.getBirthDate() == null) {
                return false;
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    public boolean validateFhirCondition(org.hl7.fhir.r4.model.Condition condition) {
        try {
            // Basic validation
            if (condition.getSubject() == null || condition.getCode() == null) {
                return false;
            }
            
            // Subject must be a patient reference
            if (!condition.getSubject().getReference().startsWith("Patient/")) {
                return false;
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    public boolean validateFhirObservation(org.hl7.fhir.r4.model.Observation observation) {
        try {
            // Basic validation
            if (observation.getSubject() == null || observation.getCode() == null) {
                return false;
            }
            
            // Subject must be a patient reference
            if (!observation.getSubject().getReference().startsWith("Patient/")) {
                return false;
            }
            
            // Must have effective date
            if (observation.getEffective() == null) {
                return false;
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
