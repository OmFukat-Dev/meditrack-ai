package com.meditrack.patient.fhir;

import ca.uhn.fhir.context.FhirContext;
import com.meditrack.patient.security.PatientAccessContext;
import com.meditrack.patient.entity.FhirResource;
import com.meditrack.patient.entity.Patient;
import com.meditrack.patient.repository.FhirResourceRepository;
import com.meditrack.patient.repository.PatientRepository;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class FhirPatientService {

    private static final int DEFAULT_PAGE_SIZE = 100;
    private static final int MAX_PAGE_SIZE = 500;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private FhirResourceRepository fhirResourceRepository;

    @Autowired
    private FhirContext fhirContext;

    public org.hl7.fhir.r4.model.Patient createFhirPatient(Long patientId, String createdBy) {
        PatientAccessContext accessContext = currentAccessContext();
        Patient patient = loadPatient(patientId);
        assertCanManagePatient(patient, accessContext);
        org.hl7.fhir.r4.model.Patient fhirPatient = convertToFhirPatient(patient);
        persistFhirResource(patient, FhirResource.FhirResourceType.PATIENT, patient.getPatientIdentifier(), fhirPatient);
        return fhirPatient;
    }

    public org.hl7.fhir.r4.model.Patient updateFhirPatient(Long patientId, String updatedBy) {
        PatientAccessContext accessContext = currentAccessContext();
        Patient patient = loadPatient(patientId);
        assertCanManagePatient(patient, accessContext);
        org.hl7.fhir.r4.model.Patient fhirPatient = convertToFhirPatient(patient);
        persistFhirResource(patient, FhirResource.FhirResourceType.PATIENT, patient.getPatientIdentifier(), fhirPatient);
        return fhirPatient;
    }

    @Transactional(readOnly = true)
    public org.hl7.fhir.r4.model.Patient getFhirPatient(Long patientId) {
        Patient patient = loadPatient(patientId);
        assertCanViewPatient(patient);
        return fhirResourceRepository
            .findFirstByPatientIdAndResourceTypeAndResourceIdOrderByCreatedAtDesc(
                patientId,
                FhirResource.FhirResourceType.PATIENT,
                patient.getPatientIdentifier()
            )
            .map(resource -> deserializeFhirResource(resource.getResourceData(), org.hl7.fhir.r4.model.Patient.class))
            .orElseGet(() -> {
                org.hl7.fhir.r4.model.Patient fhirPatient = convertToFhirPatient(patient);
                fhirPatient.setId(patient.getPatientIdentifier());
                return fhirPatient;
            });
    }

    @Transactional(readOnly = true)
    public Bundle getAllFhirPatients() {
        return getAllFhirPatients(0, DEFAULT_PAGE_SIZE);
    }

    @Transactional(readOnly = true)
    public Bundle getAllFhirPatients(int page, int size) {
        PatientAccessContext accessContext = currentAccessContext();
        PageRequest pageable = resolvePageRequest(page, size);

        if (accessContext.isAdmin()) {
            Page<Patient> activePatients = patientRepository.findByIsActive(true, pageable);
            return buildPatientBundle(activePatients.getContent(), Bundle.BundleType.SEARCHSET, activePatients.getTotalElements());
        }

        List<Patient> accessiblePatients = getAccessiblePatients(accessContext);
        List<Patient> pagePatients = pageAccessiblePatients(accessContext, pageable);
        return buildPatientBundle(pagePatients, Bundle.BundleType.SEARCHSET, accessiblePatients.size());
    }

    public void deleteFhirPatient(Long patientId) {
        PatientAccessContext accessContext = currentAccessContext();
        Patient patient = loadPatient(patientId);
        assertCanManagePatient(patient, accessContext);
        patient.setIsActive(false);
        patientRepository.save(patient);
        fhirResourceRepository.deleteByPatientIdAndResourceType(patientId, FhirResource.FhirResourceType.PATIENT);
    }

    public Observation createFhirObservation(Long patientId, Observation observation, String createdBy) {
        PatientAccessContext accessContext = currentAccessContext();
        Patient patient = loadPatient(patientId);
        assertCanManagePatient(patient, accessContext);
        observation.setSubject(new Reference("Patient/" + patient.getPatientIdentifier()));

        String resourceId = resolveResourceId(observation.getIdElement().getIdPart());
        observation.setId(resourceId);
        persistFhirResource(patient, FhirResource.FhirResourceType.OBSERVATION, resourceId, observation);
        return observation;
    }

    @Transactional(readOnly = true)
    public List<Observation> getFhirObservations(Long patientId) {
        PatientAccessContext accessContext = currentAccessContext();
        Patient patient = loadPatient(patientId);
        assertCanViewPatient(patient, accessContext);
        List<FhirResource> fhirResources = fhirResourceRepository
            .findByPatientIdAndResourceType(patientId, FhirResource.FhirResourceType.OBSERVATION);

        return fhirResources.stream()
            .map(resource -> deserializeFhirResource(resource.getResourceData(), Observation.class))
            .toList();
    }

    public Condition createFhirCondition(Long patientId, Condition condition, String createdBy) {
        PatientAccessContext accessContext = currentAccessContext();
        Patient patient = loadPatient(patientId);
        assertCanManagePatient(patient, accessContext);
        condition.setSubject(new Reference("Patient/" + patient.getPatientIdentifier()));

        String resourceId = resolveResourceId(condition.getIdElement().getIdPart());
        condition.setId(resourceId);
        persistFhirResource(patient, FhirResource.FhirResourceType.CONDITION, resourceId, condition);
        return condition;
    }

    @Transactional(readOnly = true)
    public List<Condition> getFhirConditions(Long patientId) {
        PatientAccessContext accessContext = currentAccessContext();
        Patient patient = loadPatient(patientId);
        assertCanViewPatient(patient, accessContext);
        List<FhirResource> fhirResources = fhirResourceRepository
            .findByPatientIdAndResourceType(patientId, FhirResource.FhirResourceType.CONDITION);

        return fhirResources.stream()
            .map(resource -> deserializeFhirResource(resource.getResourceData(), Condition.class))
            .toList();
    }

    @Transactional(readOnly = true)
    public Bundle getPatientBundle(Long patientId) {
        PatientAccessContext accessContext = currentAccessContext();
        Patient patient = loadPatient(patientId);
        assertCanViewPatient(patient, accessContext);
        List<Patient> patients = List.of(patient);
        Bundle bundle = buildPatientBundle(patients);

        List<Condition> conditions = getFhirConditions(patientId);
        for (Condition condition : conditions) {
            Bundle.BundleEntryComponent entry = new Bundle.BundleEntryComponent();
            entry.setFullUrl("Condition/" + condition.getIdElement().getIdPart());
            entry.setResource(condition);
            bundle.addEntry(entry);
        }

        List<Observation> observations = getFhirObservations(patientId);
        for (Observation observation : observations) {
            Bundle.BundleEntryComponent entry = new Bundle.BundleEntryComponent();
            entry.setFullUrl("Observation/" + observation.getIdElement().getIdPart());
            entry.setResource(observation);
            bundle.addEntry(entry);
        }

        return bundle;
    }

    @Transactional(readOnly = true)
    public Bundle searchPatients(String identifier, String name, String gender, LocalDate birthDate) {
        return searchPatients(identifier, name, gender, birthDate, 0, DEFAULT_PAGE_SIZE);
    }

    @Transactional(readOnly = true)
    public Bundle searchPatients(String identifier, String name, String gender, LocalDate birthDate, int page, int size) {
        PatientAccessContext accessContext = currentAccessContext();
        List<Patient> matches = new ArrayList<>();

        if (!accessContext.isAdmin()) {
            List<Patient> accessiblePatients = getAccessiblePatients(accessContext);
            if (identifier != null && !identifier.isBlank()) {
                matches = accessiblePatients.stream()
                    .filter(patient -> identifier.equalsIgnoreCase(patient.getPatientIdentifier()))
                    .filter(patient -> birthDate == null || birthDate.equals(patient.getDateOfBirth()))
                    .toList();
                return buildPatientBundle(pageBundle(matches, page, size), Bundle.BundleType.SEARCHSET, matches.size());
            }

            List<Patient> filtered = accessiblePatients.stream()
                .filter(patient -> name == null || name.isBlank() || patientMatchesName(patient, name))
                .filter(patient -> gender == null || gender.isBlank() || gender.equalsIgnoreCase(patient.getGender()))
                .filter(patient -> birthDate == null || birthDate.equals(patient.getDateOfBirth()))
                .collect(java.util.stream.Collectors.toList());

            return buildPatientBundle(pageBundle(filtered, page, size), Bundle.BundleType.SEARCHSET, filtered.size());
        }

        if (identifier != null && !identifier.isBlank()) {
            patientRepository.findByPatientIdentifier(identifier).ifPresent(matches::add);
            if (birthDate != null) {
                matches = matches.stream()
                    .filter(patient -> birthDate.equals(patient.getDateOfBirth()))
                    .toList();
            }
            return buildPatientBundle(matches, Bundle.BundleType.SEARCHSET, matches.size());
        } else {
            Page<Patient> resultPage = patientRepository.advancedSearch(
                name,
                name,
                gender,
                null,
                null,
                null,
                null,
                birthDate,
                null,
                null,
                null,
                resolvePageRequest(page, size)
            );
            matches.addAll(resultPage.getContent());
            return buildPatientBundle(matches, Bundle.BundleType.SEARCHSET, resultPage.getTotalElements());
        }
    }

    @Transactional(readOnly = true)
    public boolean validateFhirPatient(org.hl7.fhir.r4.model.Patient fhirPatient) {
        try {
            if (fhirPatient.getName() == null || fhirPatient.getName().isEmpty()) {
                return false;
            }

            HumanName name = fhirPatient.getName().get(0);
            if (name.getFamily() == null || name.getGiven() == null || name.getGiven().isEmpty()) {
                return false;
            }

            return fhirPatient.getGender() != null && fhirPatient.getBirthDate() != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public boolean validateFhirCondition(Condition condition) {
        try {
            return condition.getSubject() != null
                && condition.getCode() != null
                && condition.getSubject().getReference() != null
                && condition.getSubject().getReference().startsWith("Patient/");
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public boolean validateFhirObservation(Observation observation) {
        try {
            return observation.getSubject() != null
                && observation.getCode() != null
                && observation.getSubject().getReference() != null
                && observation.getSubject().getReference().startsWith("Patient/")
                && observation.getEffective() != null;
        } catch (Exception e) {
            return false;
        }
    }

    private Patient loadPatient(Long patientId) {
        return patientRepository.findById(patientId)
            .orElseThrow(() -> new IllegalArgumentException("Patient not found with id: " + patientId));
    }

    private PatientAccessContext currentAccessContext() {
        return PatientAccessContext.fromCurrentRequest();
    }

    private void assertCanViewPatient(Patient patient) {
        assertCanViewPatient(patient, currentAccessContext());
    }

    private void assertCanViewPatient(Patient patient, PatientAccessContext accessContext) {
        if (!accessContext.canViewPatient(patient)) {
            throw new AccessDeniedException("You do not have access to this patient");
        }
    }

    private void assertCanManagePatient(Patient patient, PatientAccessContext accessContext) {
        if (!accessContext.canManagePatient(patient)) {
            throw new AccessDeniedException("You do not have permission to modify this patient");
        }
    }

    private List<Patient> getAccessiblePatients(PatientAccessContext accessContext) {
        if (accessContext.isAdmin()) {
            return patientRepository.findByIsActive(true);
        }

        if (accessContext.isClinician() || accessContext.isNurse()) {
            if (accessContext.getDepartment() == null || accessContext.getDepartment().isBlank()) {
                throw new AccessDeniedException("Department is required to access patient records");
            }
            return patientRepository.findByDepartmentIgnoreCaseAndIsActive(accessContext.getDepartment(), true);
        }

        if (accessContext.isViewer()) {
            return patientRepository.findByViewerEmailIgnoreCaseAndIsActive(accessContext.getEmail(), true);
        }

        throw new AccessDeniedException("Unable to resolve access scope");
    }

    private List<Patient> pageAccessiblePatients(PatientAccessContext accessContext, Pageable pageable) {
        List<Patient> patients = getAccessiblePatients(accessContext);
        int start = (int) pageable.getOffset();
        if (start >= patients.size()) {
            return List.of();
        }

        int end = Math.min(start + pageable.getPageSize(), patients.size());
        return new ArrayList<>(patients.subList(start, end));
    }

    private List<Patient> pageBundle(List<Patient> patients, int page, int size) {
        PageRequest pageable = resolvePageRequest(page, size);
        int start = (int) pageable.getOffset();
        if (start >= patients.size()) {
            return List.of();
        }

        int end = Math.min(start + pageable.getPageSize(), patients.size());
        return new ArrayList<>(patients.subList(start, end));
    }

    private boolean patientMatchesName(Patient patient, String query) {
        String normalized = query.trim().toLowerCase();
        return containsIgnoreCase(patient.getFirstName(), normalized)
            || containsIgnoreCase(patient.getLastName(), normalized)
            || containsIgnoreCase(patient.getFullName(), normalized)
            || containsIgnoreCase(patient.getPatientIdentifier(), normalized);
    }

    private boolean containsIgnoreCase(String value, String query) {
        if (value == null || query == null || query.isBlank()) {
            return false;
        }

        return value.toLowerCase().contains(query.toLowerCase());
    }

    private void persistFhirResource(Patient patient,
                                     FhirResource.FhirResourceType resourceType,
                                     String resourceId,
                                     IBaseResource resource) {
        String resolvedResourceId = resolveResourceId(resourceId);
        String serialized = serializeFhirResource(resource);

        FhirResource fhirResource = fhirResourceRepository
            .findFirstByPatientIdAndResourceTypeAndResourceIdAndResourceVersion(
                patient.getId(),
                resourceType,
                resolvedResourceId,
                "1"
            )
            .orElseGet(() -> new FhirResource(patient, resourceType, resolvedResourceId, serialized));

        fhirResource.setPatient(patient);
        fhirResource.setResourceType(resourceType);
        fhirResource.setResourceId(resolvedResourceId);
        fhirResource.setResourceVersion("1");
        fhirResource.setResourceData(serialized);
        fhirResourceRepository.save(fhirResource);
    }

    private org.hl7.fhir.r4.model.Patient convertToFhirPatient(Patient patient) {
        org.hl7.fhir.r4.model.Patient fhirPatient = new org.hl7.fhir.r4.model.Patient();

        if (patient.getPatientIdentifier() != null) {
            fhirPatient.setId(patient.getPatientIdentifier());
        }

        Identifier identifier = new Identifier();
        identifier.setSystem("urn:meditrack:patient:id");
        identifier.setValue(patient.getPatientIdentifier());
        fhirPatient.addIdentifier(identifier);

        HumanName name = new HumanName();
        name.setFamily(patient.getLastName());
        name.addGiven(patient.getFirstName());
        fhirPatient.addName(name);

        if (patient.getGender() != null) {
            try {
                fhirPatient.setGender(Enumerations.AdministrativeGender.fromCode(patient.getGender().toLowerCase()));
            } catch (Exception e) {
                fhirPatient.setGender(Enumerations.AdministrativeGender.UNKNOWN);
            }
        } else {
            fhirPatient.setGender(Enumerations.AdministrativeGender.UNKNOWN);
        }

        if (patient.getDateOfBirth() != null) {
            fhirPatient.setBirthDate(Date.from(patient.getDateOfBirth().atStartOfDay().toInstant(ZoneOffset.UTC)));
        }

        if (patient.getPhoneNumber() != null) {
            ContactPoint phone = new ContactPoint();
            phone.setSystem(ContactPoint.ContactPointSystem.PHONE);
            phone.setValue(patient.getPhoneNumber());
            fhirPatient.addTelecom(phone);
        }

        if (patient.getEmail() != null) {
            ContactPoint email = new ContactPoint();
            email.setSystem(ContactPoint.ContactPointSystem.EMAIL);
            email.setValue(patient.getEmail());
            fhirPatient.addTelecom(email);
        }

        if (patient.getAddress() != null) {
            Address address = new Address();
            address.setUse(Address.AddressUse.HOME);
            address.setType(Address.AddressType.BOTH);
            address.setText(patient.getAddress());
            fhirPatient.addAddress(address);
        }

        if (patient.getEmergencyContactName() != null || patient.getEmergencyContactPhone() != null) {
            org.hl7.fhir.r4.model.Patient.ContactComponent contact = new org.hl7.fhir.r4.model.Patient.ContactComponent();
            HumanName emergencyName = new HumanName();
            emergencyName.setText(patient.getEmergencyContactName());
            contact.setName(emergencyName);

            if (patient.getEmergencyContactPhone() != null) {
                ContactPoint emergencyPhone = new ContactPoint();
                emergencyPhone.setSystem(ContactPoint.ContactPointSystem.PHONE);
                emergencyPhone.setValue(patient.getEmergencyContactPhone());
                contact.addTelecom(emergencyPhone);
            }

            fhirPatient.addContact(contact);
        }

        if (patient.getBloodType() != null) {
            fhirPatient.addExtension(new Extension(
                "http://hl7.org/fhir/StructureDefinition/bloodtype",
                new StringType(patient.getBloodType())
            ));
        }

        fhirPatient.setActive(Boolean.TRUE.equals(patient.getIsActive()));
        return fhirPatient;
    }

    private String serializeFhirResource(IBaseResource resource) {
        try {
            return fhirContext.newJsonParser().encodeResourceToString(resource);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize FHIR resource", e);
        }
    }

    private <T extends IBaseResource> T deserializeFhirResource(String resourceData, Class<T> clazz) {
        try {
            return fhirContext.newJsonParser().parseResource(clazz, resourceData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize FHIR resource", e);
        }
    }

    private Bundle buildPatientBundle(List<Patient> patients) {
        return buildPatientBundle(patients, Bundle.BundleType.COLLECTION, patients.size());
    }

    private Bundle buildPatientBundle(List<Patient> patients, Bundle.BundleType bundleType, long total) {
        Bundle bundle = new Bundle();
        bundle.setType(bundleType);
        bundle.setTimestamp(new Date());
        bundle.setTotal(safeTotal(total));

        for (Patient patient : patients) {
            org.hl7.fhir.r4.model.Patient fhirPatient = convertToFhirPatient(patient);
            Bundle.BundleEntryComponent entry = new Bundle.BundleEntryComponent();
            entry.setFullUrl("Patient/" + resolveResourceId(patient.getPatientIdentifier()));
            entry.setResource(fhirPatient);
            bundle.addEntry(entry);
        }

        return bundle;
    }

    private PageRequest resolvePageRequest(int page, int size) {
        int normalizedPage = Math.max(0, page);
        int normalizedSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        return PageRequest.of(normalizedPage, normalizedSize);
    }

    private int safeTotal(long total) {
        return total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
    }

    private String resolveResourceId(String resourceId) {
        if (resourceId == null || resourceId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return resourceId;
    }
}
