package com.meditrack.patient.service;

import com.meditrack.patient.dto.AccessLoginRequest;
import com.meditrack.patient.dto.AccessPrincipalResponse;
import com.meditrack.patient.dto.StaffMemberRequest;
import com.meditrack.patient.dto.StaffMemberResponse;
import com.meditrack.patient.entity.Patient;
import com.meditrack.patient.entity.StaffMember;
import com.meditrack.patient.entity.StaffMember.StaffRole;
import com.meditrack.patient.repository.PatientRepository;
import com.meditrack.patient.repository.StaffMemberRepository;
import com.meditrack.patient.security.PatientAccessContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class StaffService {

    private final StaffMemberRepository staffMemberRepository;
    private final PatientRepository patientRepository;

    public StaffService(StaffMemberRepository staffMemberRepository, PatientRepository patientRepository) {
        this.staffMemberRepository = staffMemberRepository;
        this.patientRepository = patientRepository;
    }

    public AccessPrincipalResponse authenticate(AccessLoginRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Login request cannot be null");
        }

        String role = normalizeRole(request.getRole());
        if (role == null) {
            throw new IllegalArgumentException("Role is required");
        }

        if (isStaffRole(role)) {
            return authenticateStaff(request, role);
        }

        if (isPatientRole(role)) {
            return authenticatePatient(request);
        }

        throw new IllegalArgumentException("Unsupported role: " + request.getRole());
    }

    public List<StaffMemberResponse> getStaffMembers(String roleFilter) {
        PatientAccessContext accessContext = PatientAccessContext.fromCurrentRequest();
        ensureAdminOnly(accessContext);

        List<StaffMember> staffMembers;
        if (roleFilter == null || roleFilter.isBlank()) {
            staffMembers = staffMemberRepository.findAll();
        } else {
            StaffRole role = parseStaffRole(roleFilter);
            staffMembers = staffMemberRepository.findByRoleOrderByFullNameAsc(role);
        }

        return staffMembers.stream()
            .sorted(Comparator.comparing(StaffMember::getFullName, Comparator.nullsLast(String::compareToIgnoreCase)))
            .map(StaffMemberResponse::fromEntity)
            .collect(Collectors.toList());
    }

    public StaffMemberResponse createStaffMember(StaffMemberRequest request) {
        PatientAccessContext accessContext = PatientAccessContext.fromCurrentRequest();
        ensureAdminOnly(accessContext);
        return StaffMemberResponse.fromEntity(saveStaffMember(new StaffMember(), request));
    }

    public StaffMemberResponse updateStaffMember(Long id, StaffMemberRequest request) {
        PatientAccessContext accessContext = PatientAccessContext.fromCurrentRequest();
        ensureAdminOnly(accessContext);

        StaffMember staffMember = staffMemberRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Staff member not found: " + id));
        return StaffMemberResponse.fromEntity(saveStaffMember(staffMember, request));
    }

    public StaffMemberResponse toggleActiveStatus(Long id, Boolean active) {
        PatientAccessContext accessContext = PatientAccessContext.fromCurrentRequest();
        ensureAdminOnly(accessContext);

        StaffMember staffMember = staffMemberRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Staff member not found: " + id));
        staffMember.setActive(active == null ? Boolean.TRUE : active);
        return StaffMemberResponse.fromEntity(staffMemberRepository.save(staffMember));
    }

    public void deactivateStaffMember(Long id) {
        toggleActiveStatus(id, Boolean.FALSE);
    }

    public Optional<StaffMember> findActiveStaffByEmail(String email) {
        return staffMemberRepository.findByEmailIgnoreCase(email)
            .filter(member -> Boolean.TRUE.equals(member.getActive()));
    }

    public List<StaffMemberResponse> getSeededDoctors() {
        return staffMemberRepository.findByRoleAndActiveTrueOrderByFullNameAsc(StaffRole.DOCTOR).stream()
            .map(StaffMemberResponse::fromEntity)
            .collect(Collectors.toList());
    }

    public List<StaffMemberResponse> getSeededNurses() {
        return staffMemberRepository.findByRoleAndActiveTrueOrderByFullNameAsc(StaffRole.NURSE).stream()
            .map(StaffMemberResponse::fromEntity)
            .collect(Collectors.toList());
    }

    private AccessPrincipalResponse authenticateStaff(AccessLoginRequest request, String requestedRole) {
        String email = requireText(request.getEmail(), "Email is required");
        StaffMember staffMember = staffMemberRepository.findByEmailIgnoreCase(email)
            .filter(member -> Boolean.TRUE.equals(member.getActive()))
            .orElseThrow(() -> new AccessDeniedException("No registered staff account found for " + email));

        if (!matchesStaffRole(requestedRole, staffMember.getRole())) {
            throw new AccessDeniedException("Email does not match the selected role");
        }

        String requestedDepartment = trimToNull(request.getDepartment());
        if (requestedDepartment != null && staffMember.getDepartment() != null
            && !requestedDepartment.equalsIgnoreCase(staffMember.getDepartment())) {
            throw new AccessDeniedException("Department does not match the registered staff record");
        }

        return buildPrincipalResponse(
            staffMember.getId().toString(),
            staffMember.getFullName(),
            staffMember.getRole().name().toLowerCase(Locale.ROOT),
            staffMember.getEmail(),
            staffMember.getDepartment(),
            null,
            null,
            null
        );
    }

    private AccessPrincipalResponse authenticatePatient(AccessLoginRequest request) {
        String email = trimToNull(request.getEmail());
        String patientIdentifier = trimToNull(request.getPatientIdentifier());
        String fullName = trimToNull(request.getName());
        String bedNumber = trimToNull(request.getBedNumber());
        String wardNumber = trimToNull(request.getWardNumber());
        String requestedDepartment = trimToNull(request.getDepartment());

        if (email == null && patientIdentifier == null) {
            throw new IllegalArgumentException("Patient email or identifier is required");
        }

        Patient patient = null;
        if (email != null) {
            patient = patientRepository.findFirstByEmailIgnoreCaseAndIsActive(email, true).orElse(null);
        }
        if (patient == null && patientIdentifier != null) {
            patient = patientRepository.findByPatientIdentifier(patientIdentifier).orElse(null);
        }

        if (patient == null || !Boolean.TRUE.equals(patient.getIsActive())) {
            throw new AccessDeniedException("No matching patient record found");
        }

        if (patientIdentifier != null && !patientIdentifier.equalsIgnoreCase(patient.getPatientIdentifier())) {
            throw new AccessDeniedException("Patient identifier does not match");
        }

        if (email != null && !matchesPatientEmail(patient, email)) {
            throw new AccessDeniedException("Email does not match the patient record");
        }

        if (fullName != null && !fullName.equalsIgnoreCase(patient.getFullName())) {
            throw new AccessDeniedException("Name does not match the patient record");
        }

        if (bedNumber != null && patient.getBedNumber() != null && !bedNumber.equalsIgnoreCase(patient.getBedNumber())) {
            throw new AccessDeniedException("Bed number does not match the patient record");
        }

        if (wardNumber != null && patient.getWardNumber() != null && !wardNumber.equalsIgnoreCase(patient.getWardNumber())) {
            throw new AccessDeniedException("Ward number does not match the patient record");
        }

        if (requestedDepartment != null && patient.getDepartment() != null
            && !requestedDepartment.equalsIgnoreCase(patient.getDepartment())) {
            throw new AccessDeniedException("Department does not match the patient record");
        }

        return buildPrincipalResponse(
            patient.getId().toString(),
            patient.getFullName(),
            "patient",
            patient.getEmail(),
            patient.getDepartment(),
            patient.getBedNumber(),
            patient.getPatientIdentifier(),
            patient.getWardNumber()
        );
    }

    private StaffMember saveStaffMember(StaffMember staffMember, StaffMemberRequest request) {
        String fullName = requireText(request.getFullName(), "Full name is required");
        String email = requireText(request.getEmail(), "Email is required");
        String roleValue = normalizeRole(requireText(request.getRole(), "Role is required"));
        StaffRole role = parseStaffRole(roleValue);

        staffMember.setFullName(fullName);
        staffMember.setEmail(email);
        staffMember.setRole(role);
        staffMember.setDepartment(trimToNull(request.getDepartment()));
        staffMember.setSpecialization(trimToNull(request.getSpecialization()));
        staffMember.setPhoneNumber(trimToNull(request.getPhoneNumber()));
        staffMember.setLicenseNumber(trimToNull(request.getLicenseNumber()));
        if (request.getActive() != null) {
            staffMember.setActive(request.getActive());
        }

        if (role == StaffRole.ADMIN) {
            staffMember.setDepartment(null);
        }

        if (role != StaffRole.DOCTOR) {
            staffMember.setSpecialization(trimToNull(request.getSpecialization()));
        }

        if (role == StaffRole.DOCTOR && staffMember.getDepartment() == null) {
            throw new IllegalArgumentException("Department is required for doctors");
        }

        if (role == StaffRole.NURSE && staffMember.getDepartment() == null) {
            throw new IllegalArgumentException("Department is required for nurses");
        }

        if (staffMember.getRole() == StaffRole.DOCTOR && staffMember.getSpecialization() == null) {
            staffMember.setSpecialization(staffMember.getDepartment());
        }

        return staffMemberRepository.save(staffMember);
    }

    private void ensureAdminOnly(PatientAccessContext accessContext) {
        if (!accessContext.isAdmin()) {
            throw new AccessDeniedException("Only administrators can perform this action");
        }
    }

    private static AccessPrincipalResponse buildPrincipalResponse(String id,
                                                                  String name,
                                                                  String role,
                                                                  String email,
                                                                  String department,
                                                                  String bedNumber,
                                                                  String patientIdentifier,
                                                                  String wardNumber) {
        AccessPrincipalResponse response = new AccessPrincipalResponse();
        response.setId(id);
        response.setName(name);
        response.setRole(role);
        response.setEmail(email);
        response.setDepartment(department);
        response.setBedNumber(bedNumber);
        response.setPatientIdentifier(patientIdentifier);
        response.setWardNumber(wardNumber);
        return response;
    }

    private static boolean isStaffRole(String role) {
        return "admin".equalsIgnoreCase(role) || "doctor".equalsIgnoreCase(role) || "nurse".equalsIgnoreCase(role);
    }

    private static boolean isPatientRole(String role) {
        return "patient".equalsIgnoreCase(role) || "viewer".equalsIgnoreCase(role);
    }

    private static boolean matchesStaffRole(String requestedRole, StaffRole actualRole) {
        if (requestedRole == null || actualRole == null) {
            return false;
        }

        return switch (requestedRole.toLowerCase(Locale.ROOT)) {
            case "admin" -> actualRole == StaffRole.ADMIN;
            case "doctor", "clinician" -> actualRole == StaffRole.DOCTOR;
            case "nurse" -> actualRole == StaffRole.NURSE;
            default -> false;
        };
    }

    private static String normalizeRole(String role) {
        return trimToNull(role) == null ? null : role.trim().toLowerCase(Locale.ROOT);
    }

    private static StaffRole parseStaffRole(String value) {
        String normalized = normalizeRole(value);
        if (normalized == null) {
            throw new IllegalArgumentException("Role is required");
        }

        return switch (normalized) {
            case "admin" -> StaffRole.ADMIN;
            case "doctor", "clinician" -> StaffRole.DOCTOR;
            case "nurse" -> StaffRole.NURSE;
            default -> throw new IllegalArgumentException("Unsupported staff role: " + value);
        };
    }

    private static String requireText(String value, String message) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new IllegalArgumentException(message);
        }
        return trimmed;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean matchesPatientEmail(Patient patient, String email) {
        return (patient.getEmail() != null && patient.getEmail().trim().equalsIgnoreCase(email))
            || (patient.getViewerEmail() != null && patient.getViewerEmail().trim().equalsIgnoreCase(email));
    }
}
