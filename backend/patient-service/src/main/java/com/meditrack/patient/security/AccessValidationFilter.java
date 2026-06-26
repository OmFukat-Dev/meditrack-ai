package com.meditrack.patient.security;

import com.meditrack.patient.entity.Patient;
import com.meditrack.patient.entity.StaffMember;
import com.meditrack.patient.repository.PatientRepository;
import com.meditrack.patient.repository.StaffMemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;
import java.util.Optional;

@Component
public class AccessValidationFilter extends OncePerRequestFilter {

    private final StaffMemberRepository staffMemberRepository;
    private final PatientRepository patientRepository;

    public AccessValidationFilter(StaffMemberRepository staffMemberRepository, PatientRepository patientRepository) {
        this.staffMemberRepository = staffMemberRepository;
        this.patientRepository = patientRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator/")
            || path.startsWith("/swagger-ui/")
            || path.startsWith("/v3/api-docs/")
            || path.startsWith("/api/access/login")
            || path.startsWith("/api/fhir/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        String role = trimToNull(request.getHeader("X-User-Role"));
        String email = trimToNull(request.getHeader("X-User-Email"));

        if (role == null || email == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing user headers");
            return;
        }

        String normalizedRole = role.toUpperCase(Locale.ROOT);
        if ("PATIENT".equals(normalizedRole) || "VIEWER".equals(normalizedRole)) {
            validatePatientAccess(request, response, email);
            filterChain.doFilter(request, response);
            return;
        }

        Optional<StaffMember> staffMember = staffMemberRepository.findByEmailIgnoreCase(email)
            .filter(member -> Boolean.TRUE.equals(member.getActive()));
        if (staffMember.isEmpty()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Staff account not found");
            return;
        }

        StaffMember member = staffMember.get();
        if (!member.getRole().name().equalsIgnoreCase(normalizedRole)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Role does not match registered staff member");
            return;
        }

        String departmentHeader = trimToNull(request.getHeader("X-User-Department"));
        if (departmentHeader != null && member.getDepartment() != null
            && !departmentHeader.equalsIgnoreCase(member.getDepartment())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Department does not match registered staff member");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void validatePatientAccess(HttpServletRequest request, HttpServletResponse response, String email) throws IOException {
        String idHeader = trimToNull(request.getHeader("X-User-Id"));
        if (idHeader == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing patient identifier");
            return;
        }

        Long patientId;
        try {
            patientId = Long.parseLong(idHeader);
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid patient identifier");
            return;
        }

        Optional<Patient> patient = patientRepository.findById(patientId);
        if (patient.isEmpty()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Patient account not found");
            return;
        }

        Patient resolvedPatient = patient.get();
        if (!matchesPatientEmail(resolvedPatient, email)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Patient email does not match the registered record");
            return;
        }
    }

    private static boolean matchesPatientEmail(Patient patient, String email) {
        return (patient.getEmail() != null && patient.getEmail().trim().equalsIgnoreCase(email))
            || (patient.getViewerEmail() != null && patient.getViewerEmail().trim().equalsIgnoreCase(email));
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
