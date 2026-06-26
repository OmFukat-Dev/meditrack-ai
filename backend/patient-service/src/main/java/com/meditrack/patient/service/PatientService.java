package com.meditrack.patient.service;

import com.meditrack.patient.dto.ClinicianSummary;
import com.meditrack.patient.dto.PatientRequest;
import com.meditrack.patient.dto.PatientResponse;
import com.meditrack.patient.entity.Patient;
import com.meditrack.patient.repository.PatientRepository;
import com.meditrack.patient.security.PatientAccessContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class PatientService {

    @Autowired
    private PatientRepository patientRepository;

    public PatientResponse createPatient(PatientRequest request, String createdBy) {
        PatientAccessContext accessContext = currentAccessContext();
        ensureCreateAccess(accessContext);

        if (patientRepository.existsByPatientIdentifier(request.getPatientIdentifier())) {
            throw new IllegalArgumentException("Patient with identifier " + request.getPatientIdentifier() + " already exists");
        }

        Patient patient = convertToEntity(request);
        applyOwnershipDefaults(patient, request, accessContext);
        String actor = resolveActor(createdBy, accessContext);
        patient.setCreatedBy(actor);
        patient.setUpdatedBy(actor);

        Patient savedPatient = patientRepository.save(patient);
        return convertToResponse(savedPatient);
    }

    public PatientResponse getPatientById(Long id) {
        PatientAccessContext accessContext = currentAccessContext();
        Patient patient = loadPatient(id);
        assertCanViewPatient(patient, accessContext);
        return convertToResponse(patient);
    }

    public PatientResponse getPatientByIdentifier(String patientIdentifier) {
        PatientAccessContext accessContext = currentAccessContext();
        Patient patient = patientRepository.findByPatientIdentifier(patientIdentifier)
            .orElseThrow(() -> new IllegalArgumentException("Patient not found with identifier: " + patientIdentifier));
        assertCanViewPatient(patient, accessContext);
        return convertToResponse(patient);
    }

    public Page<PatientResponse> getAllPatients(int page, int size, String sortBy, String sortDir) {
        PatientAccessContext accessContext = currentAccessContext();
        if (isLegacyAdminContext(accessContext)) {
            Pageable pageable = PageRequest.of(page, size, Sort.by("desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy));
            return patientRepository.findByIsActive(true, pageable).map(this::convertToResponse);
        }

        List<Patient> accessiblePatients = sortPatients(getAccessiblePatients(accessContext), sortBy, sortDir);
        return pagePatients(accessiblePatients, page, size);
    }

    public PatientResponse updatePatient(Long id, PatientRequest request, String updatedBy) {
        PatientAccessContext accessContext = currentAccessContext();
        Patient existingPatient = loadPatient(id);
        assertCanManagePatient(existingPatient, accessContext);

        if (!existingPatient.getPatientIdentifier().equals(request.getPatientIdentifier()) &&
            patientRepository.existsByPatientIdentifier(request.getPatientIdentifier())) {
            throw new IllegalArgumentException("Patient with identifier " + request.getPatientIdentifier() + " already exists");
        }

        updateEntityFromRequest(existingPatient, request);
        applyOwnershipDefaults(existingPatient, request, accessContext);
        existingPatient.setUpdatedBy(resolveActor(updatedBy, accessContext));

        Patient updatedPatient = patientRepository.save(existingPatient);
        return convertToResponse(updatedPatient);
    }

    public PatientResponse updatePatientCondition(Long id, String condition, String updatedBy) {
        PatientAccessContext accessContext = currentAccessContext();
        Patient existingPatient = loadPatient(id);
        assertCanManagePatient(existingPatient, accessContext);

        existingPatient.setClinicalStatus(condition);
        existingPatient.setUpdatedBy(resolveActor(updatedBy, accessContext));

        Patient updatedPatient = patientRepository.save(existingPatient);
        return convertToResponse(updatedPatient);
    }

    public void deletePatient(Long id) {
        PatientAccessContext accessContext = currentAccessContext();
        Patient patient = loadPatient(id);
        assertCanManagePatient(patient, accessContext);
        patient.setIsActive(false);
        patientRepository.save(patient);
    }

    public void activatePatient(Long id) {
        PatientAccessContext accessContext = currentAccessContext();
        ensureAdminOnly(accessContext);

        Patient patient = loadPatient(id);
        patient.setIsActive(true);
        patientRepository.save(patient);
    }

    public Page<PatientResponse> searchPatients(String query, int page, int size) {
        PatientAccessContext accessContext = currentAccessContext();
        if (isLegacyAdminContext(accessContext)) {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "lastName"));
            return patientRepository.searchByNameOrIdentifier(query, pageable).map(this::convertToResponse);
        }

        List<Patient> matches = getAccessiblePatients(accessContext).stream()
            .filter(patient -> matchesQuery(patient, query))
            .collect(Collectors.toList());

        return pagePatients(sortPatients(matches, "lastName", "asc"), page, size);
    }

    public Page<PatientResponse> searchPatientsByFirstName(String firstName, int page, int size) {
        PatientAccessContext accessContext = currentAccessContext();
        if (isLegacyAdminContext(accessContext)) {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "firstName"));
            return patientRepository.findByFirstNameContainingIgnoreCase(firstName, pageable).map(this::convertToResponse);
        }

        List<Patient> matches = getAccessiblePatients(accessContext).stream()
            .filter(patient -> containsIgnoreCase(patient.getFirstName(), firstName))
            .collect(Collectors.toList());

        return pagePatients(sortPatients(matches, "firstName", "asc"), page, size);
    }

    public Page<PatientResponse> searchPatientsByLastName(String lastName, int page, int size) {
        PatientAccessContext accessContext = currentAccessContext();
        if (isLegacyAdminContext(accessContext)) {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "lastName"));
            return patientRepository.findByLastNameContainingIgnoreCase(lastName, pageable).map(this::convertToResponse);
        }

        List<Patient> matches = getAccessiblePatients(accessContext).stream()
            .filter(patient -> containsIgnoreCase(patient.getLastName(), lastName))
            .collect(Collectors.toList());

        return pagePatients(sortPatients(matches, "lastName", "asc"), page, size);
    }

    public Page<PatientResponse> getPatientsByGender(String gender, int page, int size) {
        PatientAccessContext accessContext = currentAccessContext();
        if (isLegacyAdminContext(accessContext)) {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "lastName"));
            return patientRepository.findByGenderAndIsActive(gender, true, pageable).map(this::convertToResponse);
        }

        List<Patient> matches = getAccessiblePatients(accessContext).stream()
            .filter(patient -> equalsIgnoreCase(patient.getGender(), gender))
            .collect(Collectors.toList());

        return pagePatients(sortPatients(matches, "lastName", "asc"), page, size);
    }

    public Page<PatientResponse> getPatientsByBloodType(String bloodType, int page, int size) {
        PatientAccessContext accessContext = currentAccessContext();
        if (isLegacyAdminContext(accessContext)) {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "lastName"));
            return patientRepository.findByBloodTypeAndIsActive(bloodType, true, pageable).map(this::convertToResponse);
        }

        List<Patient> matches = getAccessiblePatients(accessContext).stream()
            .filter(patient -> equalsIgnoreCase(patient.getBloodType(), bloodType))
            .collect(Collectors.toList());

        return pagePatients(sortPatients(matches, "lastName", "asc"), page, size);
    }

    public Page<PatientResponse> getPatientsByAgeRange(int minAge, int maxAge, int page, int size) {
        PatientAccessContext accessContext = currentAccessContext();
        if (isLegacyAdminContext(accessContext)) {
            List<Patient> patients = patientRepository.findByAgeRange(minAge, maxAge);
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "lastName"));
            int start = page * size;
            int end = Math.min(start + size, patients.size());
            List<Patient> pageContent = start < patients.size() ? patients.subList(start, end) : List.of();
            return new PageImpl<>(pageContent.stream().map(this::convertToResponse).collect(Collectors.toList()),
                pageable, patients.size());
        }

        List<Patient> matches = getAccessiblePatients(accessContext).stream()
            .filter(patient -> {
                int age = patient.getAge();
                return age >= minAge && age <= maxAge;
            })
            .collect(Collectors.toList());

        return pagePatients(sortPatients(matches, "lastName", "asc"), page, size);
    }

    public boolean isPatientEligibleForVitalMonitoring(Long patientId) {
        PatientAccessContext accessContext = currentAccessContext();
        return patientRepository.findById(patientId)
            .filter(patient -> accessContext.canViewPatient(patient))
            .map(patient -> patient.getIsActive() && patient.getAge() >= 0)
            .orElse(false);
    }

    public List<PatientResponse> getPatientsByEmergencyContact(String emergencyContactPhone) {
        PatientAccessContext accessContext = currentAccessContext();
        if (isLegacyAdminContext(accessContext)) {
            return patientRepository.findByEmergencyContactPhone(emergencyContactPhone).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        }

        return getAccessiblePatients(accessContext).stream()
            .filter(patient -> equalsIgnoreCase(patient.getEmergencyContactPhone(), emergencyContactPhone))
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    public long getTotalActivePatients() {
        PatientAccessContext accessContext = currentAccessContext();
        if (isLegacyAdminContext(accessContext)) {
            return patientRepository.countByIsActive(true);
        }

        return getAccessiblePatients(accessContext).size();
    }

    public long getPatientsByGender(String gender) {
        PatientAccessContext accessContext = currentAccessContext();
        if (isLegacyAdminContext(accessContext)) {
            return patientRepository.countByGender(gender);
        }

        return getAccessiblePatients(accessContext).stream()
            .filter(patient -> equalsIgnoreCase(patient.getGender(), gender))
            .count();
    }

    public long getPatientsByBloodType(String bloodType) {
        PatientAccessContext accessContext = currentAccessContext();
        if (isLegacyAdminContext(accessContext)) {
            return patientRepository.countByBloodType(bloodType);
        }

        return getAccessiblePatients(accessContext).stream()
            .filter(patient -> equalsIgnoreCase(patient.getBloodType(), bloodType))
            .count();
    }

    public List<ClinicianSummary> getClinicians() {
        PatientAccessContext accessContext = currentAccessContext();
        ensureAdminOnly(accessContext);
        return patientRepository.findClinicianSummaries();
    }

    private PatientAccessContext currentAccessContext() {
        return PatientAccessContext.fromCurrentRequest();
    }

    private List<Patient> getAccessiblePatients(PatientAccessContext accessContext) {
        if (accessContext.isAdmin()) {
            return patientRepository.findByIsActive(true);
        }

        if (accessContext.isClinician() || accessContext.isNurse()) {
            if (accessContext.getDepartment() != null && !accessContext.getDepartment().isEmpty()) {
                return patientRepository.findByDepartmentIgnoreCaseAndIsActive(accessContext.getDepartment(), true);
            }
            throw new AccessDeniedException("Department is required to access patient records");
        }

        if (accessContext.isViewer()) {
            return patientRepository.findByViewerEmailIgnoreCaseAndIsActive(accessContext.getEmail(), true);
        }

        throw new AccessDeniedException("Unable to resolve patient access scope");
    }

    private boolean isLegacyAdminContext(PatientAccessContext accessContext) {
        return accessContext.getRole() == PatientAccessContext.Role.SYSTEM || accessContext.isAdmin();
    }

    private void ensureCreateAccess(PatientAccessContext accessContext) {
        if (!(accessContext.isAdmin() || accessContext.isClinician())) {
            throw new AccessDeniedException("Only administrators and clinicians can create patient records");
        }
    }

    private void ensureAdminOnly(PatientAccessContext accessContext) {
        if (!accessContext.isAdmin()) {
            throw new AccessDeniedException("Only administrators can perform this action");
        }
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

    private void applyOwnershipDefaults(Patient patient, PatientRequest request, PatientAccessContext accessContext) {
        if (accessContext.isClinician()) {
            patient.setAssignedClinicianEmail(accessContext.getEmail());
            patient.setAssignedClinicianName(accessContext.getDisplayName());
        }

        if (accessContext.isViewer()) {
            throw new AccessDeniedException("Viewer accounts cannot create or update patient records");
        }

        if (isBlank(patient.getAssignedClinicianEmail()) && request.getAssignedClinicianEmail() != null) {
            patient.setAssignedClinicianEmail(request.getAssignedClinicianEmail().trim());
        }

        if (isBlank(patient.getAssignedClinicianName()) && request.getAssignedClinicianName() != null) {
            patient.setAssignedClinicianName(request.getAssignedClinicianName().trim());
        }

        if (isBlank(patient.getViewerEmail()) && request.getViewerEmail() != null) {
            patient.setViewerEmail(request.getViewerEmail().trim());
        }
    }

    private String resolveActor(String suppliedActor, PatientAccessContext accessContext) {
        if (!isBlank(suppliedActor)) {
            return suppliedActor.trim();
        }

        return accessContext.actorName();
    }

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
        patient.setAssignedClinicianName(trimToNull(request.getAssignedClinicianName()));
        patient.setAssignedClinicianEmail(trimToNull(request.getAssignedClinicianEmail()));
        patient.setViewerEmail(trimToNull(request.getViewerEmail()));
        patient.setDepartment(trimToNull(request.getDepartment()));
        patient.setWardNumber(trimToNull(request.getWardNumber()));
        patient.setBedNumber(trimToNull(request.getBedNumber()));
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

        if (!isBlank(request.getAssignedClinicianName())) {
            patient.setAssignedClinicianName(request.getAssignedClinicianName().trim());
        }
        if (!isBlank(request.getAssignedClinicianEmail())) {
            patient.setAssignedClinicianEmail(request.getAssignedClinicianEmail().trim());
        }
        if (!isBlank(request.getViewerEmail())) {
            patient.setViewerEmail(request.getViewerEmail().trim());
        }
        if (!isBlank(request.getDepartment())) {
            patient.setDepartment(request.getDepartment().trim());
        }
        if (!isBlank(request.getWardNumber())) {
            patient.setWardNumber(request.getWardNumber().trim());
        }
        if (!isBlank(request.getBedNumber())) {
            patient.setBedNumber(request.getBedNumber().trim());
        }
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
        response.setAssignedClinicianName(patient.getAssignedClinicianName());
        response.setAssignedClinicianEmail(patient.getAssignedClinicianEmail());
        response.setViewerEmail(patient.getViewerEmail());
        response.setDepartment(patient.getDepartment());
        response.setWardNumber(patient.getWardNumber());
        response.setBedNumber(patient.getBedNumber());
        response.setIsActive(patient.getIsActive());
        response.setCreatedAt(patient.getCreatedAt());
        response.setUpdatedAt(patient.getUpdatedAt());
        response.setFullName(patient.getFullName());
        response.setAge(patient.getAge());
        response.setCondition(patient.getClinicalStatus() != null ? patient.getClinicalStatus() : "Stable");
        return response;
    }

    private Patient loadPatient(Long id) {
        return patientRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Patient not found with id: " + id));
    }

    private Page<PatientResponse> pagePatients(List<Patient> patients, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        int start = (int) pageable.getOffset();
        if (start >= patients.size()) {
            return new PageImpl<>(List.of(), pageable, patients.size());
        }

        int end = Math.min(start + pageable.getPageSize(), patients.size());
        List<PatientResponse> content = patients.subList(start, end).stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, patients.size());
    }

    private List<Patient> sortPatients(List<Patient> patients, String sortBy, String sortDir) {
        Comparator<Patient> comparator = comparatorFor(sortBy);
        if ("desc".equalsIgnoreCase(sortDir)) {
            comparator = comparator.reversed();
        }

        return patients.stream().sorted(comparator).collect(Collectors.toList());
    }

    private Comparator<Patient> comparatorFor(String sortBy) {
        String normalized = sortBy == null ? "lastName" : sortBy.trim().toLowerCase(Locale.ROOT);
        Comparator<String> stringComparator = Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER);
        Comparator<LocalDateTime> dateTimeComparator = Comparator.nullsLast(Comparator.naturalOrder());
        Comparator<LocalDate> dateComparator = Comparator.nullsLast(Comparator.naturalOrder());

        return switch (normalized) {
            case "firstname" -> Comparator.comparing(Patient::getFirstName, stringComparator)
                .thenComparing(Patient::getLastName, stringComparator);
            case "lastname" -> Comparator.comparing(Patient::getLastName, stringComparator)
                .thenComparing(Patient::getFirstName, stringComparator);
            case "patientidentifier" -> Comparator.comparing(Patient::getPatientIdentifier, stringComparator);
            case "createdat" -> Comparator.comparing(Patient::getCreatedAt, dateTimeComparator);
            case "updatedat" -> Comparator.comparing(Patient::getUpdatedAt, dateTimeComparator);
            case "dateofbirth" -> Comparator.comparing(Patient::getDateOfBirth, dateComparator);
            case "assignedclinicianname" -> Comparator.comparing(Patient::getAssignedClinicianName, stringComparator);
            case "assignedclinicianemail" -> Comparator.comparing(Patient::getAssignedClinicianEmail, stringComparator);
            case "vieweremail" -> Comparator.comparing(Patient::getViewerEmail, stringComparator);
            case "department" -> Comparator.comparing(Patient::getDepartment, stringComparator);
            case "wardnumber" -> Comparator.comparing(Patient::getWardNumber, stringComparator);
            case "bednumber" -> Comparator.comparing(Patient::getBedNumber, stringComparator);
            default -> Comparator.comparing(Patient::getLastName, stringComparator)
                .thenComparing(Patient::getFirstName, stringComparator);
        };
    }

    private boolean matchesQuery(Patient patient, String query) {
        if (isBlank(query)) {
            return true;
        }

        String normalizedQuery = query.trim().toLowerCase(Locale.ROOT);
        return containsIgnoreCase(patient.getFirstName(), normalizedQuery)
            || containsIgnoreCase(patient.getLastName(), normalizedQuery)
            || containsIgnoreCase(patient.getFullName(), normalizedQuery)
            || containsIgnoreCase(patient.getPatientIdentifier(), normalizedQuery)
            || containsIgnoreCase(patient.getAssignedClinicianName(), normalizedQuery)
            || containsIgnoreCase(patient.getAssignedClinicianEmail(), normalizedQuery)
            || containsIgnoreCase(patient.getViewerEmail(), normalizedQuery)
            || containsIgnoreCase(patient.getDepartment(), normalizedQuery)
            || containsIgnoreCase(patient.getWardNumber(), normalizedQuery)
            || containsIgnoreCase(patient.getBedNumber(), normalizedQuery)
            || containsIgnoreCase(patient.getEmail(), normalizedQuery);
    }

    private boolean containsIgnoreCase(String value, String query) {
        if (isBlank(value) || isBlank(query)) {
            return false;
        }

        return value.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
    }

    private boolean equalsIgnoreCase(String left, String right) {
        if (isBlank(left) || isBlank(right)) {
            return false;
        }

        return left.trim().equalsIgnoreCase(right.trim());
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String trimToNull(String value) {
        if (isBlank(value)) {
            return null;
        }

        return value.trim();
    }
}
