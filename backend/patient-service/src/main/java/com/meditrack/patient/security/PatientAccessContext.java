package com.meditrack.patient.security;

import com.meditrack.patient.entity.Patient;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.Objects;

public final class PatientAccessContext {

    public enum Role {
        ADMIN,
        CLINICIAN,
        NURSE,
        VIEWER,
        SYSTEM
    }

    private final Role role;
    private final String email;
    private final String displayName;
    private final String department;

    private PatientAccessContext(Role role, String email, String displayName, String department) {
        this.role = role;
        this.email = email;
        this.displayName = displayName;
        this.department = department;
    }

    public static PatientAccessContext fromCurrentRequest() {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            return system();
        }

        HttpServletRequest request = requestAttributes.getRequest();
        if (request == null) {
            return system();
        }

        String roleHeader = trimToNull(request.getHeader("X-User-Role"));
        String emailHeader = trimToNull(request.getHeader("X-User-Email"));
        String departmentHeader = trimToNull(request.getHeader("X-User-Department"));
        String displayNameHeader = firstNonBlank(
            request.getHeader("X-User-Display-Name"),
            request.getHeader("X-User-Name"),
            emailHeader
        );

        if (roleHeader == null || emailHeader == null) {
            throw new AccessDeniedException("X-User-Role and X-User-Email headers are required");
        }

        Role role = parseRole(roleHeader);

        return new PatientAccessContext(role, emailHeader, displayNameHeader, departmentHeader);
    }

    public static PatientAccessContext system() {
        return new PatientAccessContext(Role.SYSTEM, "system@meditrack.ai", "System", null);
    }

    public Role getRole() {
        return role;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDepartment() {
        return department;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN || role == Role.SYSTEM;
    }

    public boolean isClinician() {
        return role == Role.CLINICIAN;
    }

    public boolean isNurse() {
        return role == Role.NURSE;
    }

    public boolean isViewer() {
        return role == Role.VIEWER;
    }

    public boolean canViewPatient(Patient patient) {
        if (isAdmin()) {
            return true;
        }

        if (patient == null) {
            return false;
        }

        if (isClinician() || isNurse()) {
            return department != null && equalsIgnoreCase(department, patient.getDepartment());
        }

        if (isViewer()) {
            return equalsIgnoreCase(email, patient.getViewerEmail()) || equalsIgnoreCase(email, patient.getEmail());
        }

        return false;
    }

    public boolean canManagePatient(Patient patient) {
        return isAdmin() || ((isClinician() || isNurse()) && canViewPatient(patient));
    }

    public boolean canViewClinicians() {
        return isAdmin();
    }

    public String actorName() {
        return firstNonBlank(displayName, email, "system");
    }

    private static Role parseRole(String value) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "ADMIN" -> Role.ADMIN;
            case "CLINICIAN", "DOCTOR" -> Role.CLINICIAN;
            case "NURSE" -> Role.NURSE;
            case "VIEWER", "PATIENT" -> Role.VIEWER;
            default -> throw new AccessDeniedException("Unsupported role: " + value);
        };
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && Objects.equals(left.trim().toLowerCase(Locale.ROOT), right.trim().toLowerCase(Locale.ROOT));
    }
}
