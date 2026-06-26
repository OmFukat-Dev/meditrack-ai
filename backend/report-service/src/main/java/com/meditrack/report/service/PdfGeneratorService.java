package com.meditrack.report.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.html2pdf.HtmlConverter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class PdfGeneratorService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;
    private final String patientServiceBaseUrl;
    private final String vitalsServiceBaseUrl;
    private final int shiftLookbackHours;

    public PdfGeneratorService(
        ObjectMapper objectMapper,
        @Value("${meditrack.services.patient-base-url:http://localhost:8082}") String patientServiceBaseUrl,
        @Value("${meditrack.services.vitals-base-url:http://localhost:8083}") String vitalsServiceBaseUrl,
        @Value("${meditrack.report.shift-lookback-hours:12}") int shiftLookbackHours
    ) {
        this.objectMapper = objectMapper;
        this.patientServiceBaseUrl = patientServiceBaseUrl;
        this.vitalsServiceBaseUrl = vitalsServiceBaseUrl;
        this.shiftLookbackHours = shiftLookbackHours;
    }

    public byte[] generatePatientReport(String patientId) {
        return generatePatientReport(patientId, null);
    }

    public byte[] generatePatientReport(String patientId, HttpServletRequest request) {
        PatientSnapshot patient = fetchPatient(patientId, request);
        VitalSummary latestVitals = fetchVitalSummary(patientId, request);
        String generatedAt = formatDateTime(LocalDateTime.now());

        String body = buildPatientReportBody(patientId, patient, latestVitals, request, generatedAt);
        return renderPdf(wrapHtml("Patient Clinical Report", body));
    }

    public byte[] generateDoctorShiftReport(HttpServletRequest request) {
        StaffIdentity identity = StaffIdentity.from(request);
        List<PatientSnapshot> patients = fetchAccessiblePatients(request);
        List<PatientShiftLine> lines = buildShiftLines(patients, request);
        String generatedAt = formatDateTime(LocalDateTime.now());

        String body = buildShiftReportBody(
            "Doctor Shift Report",
            "Summary for the active doctor shift",
            identity,
            generatedAt,
            patients,
            lines
        );
        return renderPdf(wrapHtml("Doctor Shift Report", body));
    }

    public byte[] generateNurseHandoverReport(HttpServletRequest request) {
        StaffIdentity identity = StaffIdentity.from(request);
        List<PatientSnapshot> patients = fetchAccessiblePatients(request);
        List<PatientShiftLine> lines = buildShiftLines(patients, request);
        String generatedAt = formatDateTime(LocalDateTime.now());

        String body = buildShiftReportBody(
            "Nurse Handover Report",
            "Shift handover summary for ward nursing staff",
            identity,
            generatedAt,
            patients,
            lines
        );
        return renderPdf(wrapHtml("Nurse Handover Report", body));
    }

    private String buildPatientReportBody(
        String patientId,
        PatientSnapshot patient,
        VitalSummary latestVitals,
        HttpServletRequest request,
        String generatedAt
    ) {
        StringBuilder body = new StringBuilder();
        StaffIdentity identity = StaffIdentity.from(request);

        body.append(sectionHeader(
            "Clinical Summary",
            "Patient " + safeDisplayName(patient, patientId) + " and the latest available bedside readings."
        ));

        body.append(summaryCards(
            new SummaryCard("Generated At", generatedAt),
            new SummaryCard("Prepared By", identity.nameOrEmail()),
            new SummaryCard("Status", safe(patient.clinicalStatus, "Stable"))
        ));

        body.append(buildKeyValueTable(
            "Patient Details",
            List.of(
                new KeyValueRow("Patient ID", safe(patient.id, patientId)),
                new KeyValueRow("Patient Identifier", safe(patient.patientIdentifier, patientId)),
                new KeyValueRow("Name", safeDisplayName(patient, patientId)),
                new KeyValueRow("Department", safe(patient.department, "General")),
                new KeyValueRow("Ward", safe(patient.wardNumber, "N/A")),
                new KeyValueRow("Bed", safe(patient.bedNumber, "N/A")),
                new KeyValueRow("Assigned Clinician", safe(patient.assignedClinicianName, "Unassigned"))
            )
        ));

        body.append(buildVitalsTable("Latest Vitals", latestVitals.latestVitals));

        body.append(buildNarrativeSection(
            "Clinical Notes",
            "This report is generated from the live patient-service and vitals-service data. If readings are missing, the dashboard will still render the record and show what is currently available.",
            List.of(
                "Latest status: " + safe(patient.clinicalStatus, "Stable"),
                "Care team: " + safe(patient.assignedClinicianName, "Unassigned"),
                "Reading window: latest bedside values available at the time of generation"
            )
        ));

        return body.toString();
    }

    private String buildShiftReportBody(
        String title,
        String subtitle,
        StaffIdentity identity,
        String generatedAt,
        List<PatientSnapshot> patients,
        List<PatientShiftLine> lines
    ) {
        StringBuilder body = new StringBuilder();

        body.append(sectionHeader(title, subtitle + " - last " + shiftLookbackHours + " hours"));
        body.append(summaryCards(
            new SummaryCard("Prepared By", identity.nameOrEmail()),
            new SummaryCard("Role", identity.roleLabel()),
            new SummaryCard("Department", safe(identity.department, "General")),
            new SummaryCard("Generated At", generatedAt)
        ));

        body.append(buildKeyValueTable(
            "Shift Overview",
            List.of(
                new KeyValueRow("Accessible Patients", String.valueOf(patients.size())),
                new KeyValueRow("Patients Requiring Review", String.valueOf(lines.stream().filter(PatientShiftLine::needsReview).count())),
                new KeyValueRow("Shift Window", "Last " + shiftLookbackHours + " hours"),
                new KeyValueRow("Prepared For", identity.nameOrEmail())
            )
        ));

        body.append(buildPatientsTable("Ward Roster", lines));
        body.append(buildNarrativeSection(
            "Handover Notes",
            "This handover report is derived from the currently accessible ward records and the latest vitals captured by nursing staff.",
            List.of(
                "Use the highlighted rows to identify patients who require follow-up.",
                "The vitals table shows the latest readings available for each patient.",
                "Any missing rows usually indicate no recent reading has been captured yet."
            )
        ));

        return body.toString();
    }

    private List<PatientShiftLine> buildShiftLines(List<PatientSnapshot> patients, HttpServletRequest request) {
        List<PatientShiftLine> lines = new ArrayList<>();
        for (PatientSnapshot patient : patients) {
            VitalSummary vitals = fetchVitalSummary(safe(patient.id, null), request);
            lines.add(new PatientShiftLine(patient, vitals));
        }
        return lines;
    }

    private String buildPatientsTable(String title, List<PatientShiftLine> lines) {
        StringBuilder table = new StringBuilder();
        table.append("<div class=\"section\"><div class=\"section-title\">")
            .append(escapeHtml(title))
            .append("</div><table class=\"details-table\"><thead><tr>")
            .append("<th>Patient</th><th>Ward / Bed</th><th>Status</th><th>Latest Reading</th><th>Attention</th>")
            .append("</tr></thead><tbody>");

        if (lines.isEmpty()) {
            table.append("<tr><td colspan=\"5\">No accessible patients were returned by the patient-service.</td></tr>");
        } else {
            for (PatientShiftLine line : lines) {
                String attention = line.needsReview() ? "Needs review" : "Stable";
                String rowClass = line.needsReview() ? " style=\"background:#fff7ed;\"" : "";
                table.append("<tr").append(rowClass).append(">")
                    .append("<td>").append(escapeHtml(line.patient.displayName())).append("<br/><span style=\"color:#64748b;font-size:12px;\">")
                    .append(escapeHtml(safe(line.patient.patientIdentifier, "N/A"))).append("</span></td>")
                    .append("<td>").append(escapeHtml(safe(line.patient.wardNumber, "N/A"))).append(" / ")
                    .append(escapeHtml(safe(line.patient.bedNumber, "N/A"))).append("</td>")
                    .append("<td>").append(escapeHtml(safe(line.patient.clinicalStatus, "Stable"))).append("</td>")
                    .append("<td>").append(escapeHtml(line.latestVitals.latestReadingLabel())).append("</td>")
                    .append("<td>").append(escapeHtml(attention)).append("</td>")
                    .append("</tr>");
            }
        }

        table.append("</tbody></table></div>");
        return table.toString();
    }

    private String buildVitalsTable(String title, List<VitalSnapshot> vitals) {
        StringBuilder table = new StringBuilder();
        table.append("<div class=\"section\"><div class=\"section-title\">")
            .append(escapeHtml(title))
            .append("</div><table class=\"details-table\"><thead><tr>")
            .append("<th>Vital Type</th><th>Latest Value</th><th>Status</th><th>Recorded</th><th>Quality</th>")
            .append("</tr></thead><tbody>");

        if (vitals.isEmpty()) {
            table.append("<tr><td colspan=\"5\">No recent readings were available at the time of generation.</td></tr>");
        } else {
            for (VitalSnapshot vital : vitals) {
                table.append("<tr>")
                    .append("<td>").append(escapeHtml(safe(vital.vitalType, "UNKNOWN"))).append("</td>")
                    .append("<td>").append(escapeHtml(vital.displayValue())).append("</td>")
                    .append("<td>").append(escapeHtml(safe(vital.vitalStatus, "UNKNOWN"))).append("</td>")
                    .append("<td>").append(escapeHtml(safe(vital.readingTimestamp, "N/A"))).append("</td>")
                    .append("<td>").append(escapeHtml(safe(vital.qualityScore, "N/A"))).append("</td>")
                    .append("</tr>");
            }
        }

        table.append("</tbody></table></div>");
        return table.toString();
    }

    private String buildKeyValueTable(String title, List<KeyValueRow> rows) {
        StringBuilder table = new StringBuilder();
        table.append("<div class=\"section\"><div class=\"section-title\">")
            .append(escapeHtml(title))
            .append("</div><table class=\"details-table\">");
        for (KeyValueRow row : rows) {
            table.append("<tr><th>")
                .append(escapeHtml(row.label))
                .append("</th><td>")
                .append(escapeHtml(row.value))
                .append("</td></tr>");
        }
        table.append("</table></div>");
        return table.toString();
    }

    private String buildNarrativeSection(String title, String intro, List<String> bullets) {
        StringBuilder section = new StringBuilder();
        section.append("<div class=\"section\"><div class=\"section-title\">")
            .append(escapeHtml(title))
            .append("</div><p>").append(escapeHtml(intro)).append("</p><ul>");
        for (String bullet : bullets) {
            section.append("<li>").append(escapeHtml(bullet)).append("</li>");
        }
        section.append("</ul></div>");
        return section.toString();
    }

    private String summaryCards(SummaryCard... cards) {
        StringBuilder builder = new StringBuilder();
        builder.append("<div class=\"summary-grid\">");
        for (SummaryCard card : cards) {
            builder.append("<div class=\"summary-card\">")
                .append("<div class=\"summary-label\">")
                .append(escapeHtml(card.label))
                .append("</div><div class=\"summary-value\">")
                .append(escapeHtml(card.value))
                .append("</div></div>");
        }
        builder.append("</div>");
        return builder.toString();
    }

    private String sectionHeader(String title, String subtitle) {
        return "<div class=\"header\"><h1>" + escapeHtml(title) + "</h1><p>" + escapeHtml(subtitle) + "</p></div>";
    }

    private String wrapHtml(String title, String body) {
        String generatedAt = formatDateTime(LocalDateTime.now());
        StringBuilder html = new StringBuilder();
        html.append("<html><head><style>")
            .append("body { font-family: Helvetica, Arial, sans-serif; color: #0f172a; }")
            .append(".header { text-align: center; border-bottom: 2px solid #2563eb; padding-bottom: 12px; margin-bottom: 20px; }")
            .append(".header h1 { color: #1e3a8a; margin: 0; font-size: 28px; }")
            .append(".header p { color: #64748b; margin: 6px 0 0 0; }")
            .append(".summary-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 20px; }")
            .append(".summary-card { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 12px; }")
            .append(".summary-label { color: #64748b; font-size: 11px; text-transform: uppercase; letter-spacing: 0.08em; margin-bottom: 8px; }")
            .append(".summary-value { color: #0f172a; font-size: 16px; font-weight: 700; }")
            .append(".section { margin-bottom: 20px; }")
            .append(".section-title { background: #f1f5f9; padding: 8px 10px; font-weight: 700; color: #0f172a; border-left: 4px solid #2563eb; margin-bottom: 8px; }")
            .append(".details-table { width: 100%; border-collapse: collapse; margin-top: 10px; }")
            .append(".details-table th, .details-table td { padding: 8px; border-bottom: 1px solid #e2e8f0; text-align: left; vertical-align: top; }")
            .append(".details-table th { width: 30%; color: #475569; }")
            .append(".details-table thead th { background: #f8fafc; color: #334155; font-size: 12px; text-transform: uppercase; letter-spacing: 0.06em; }")
            .append("ul { margin: 0; padding-left: 20px; color: #334155; }")
            .append("li { margin-bottom: 6px; }")
            .append(".footer { margin-top: 36px; font-size: 11px; color: #94a3b8; text-align: center; }")
            .append("</style></head><body>")
            .append("<div class=\"header\"><h1>MediTrack AI</h1><p>")
            .append(escapeHtml(title))
            .append("</p></div>")
            .append(body)
            .append("<div class=\"footer\"><p>Generated ")
            .append(escapeHtml(generatedAt))
            .append(" - MediTrack AI clinical system</p></div>")
            .append("</body></html>");
        return html.toString();
    }

    private byte[] renderPdf(String htmlContent) {
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        HtmlConverter.convertToPdf(htmlContent, target);
        return target.toByteArray();
    }

    private PatientSnapshot fetchPatient(String patientId, HttpServletRequest request) {
        try {
            Map<String, Object> response = exchangeForMap(patientServiceBaseUrl + "/api/patients/" + patientId, requestHeaders(request));
            return toPatientSnapshot(response);
        } catch (RestClientException ex) {
            throw new IllegalStateException("Unable to fetch patient data from patient-service", ex);
        }
    }

    private List<PatientSnapshot> fetchAccessiblePatients(HttpServletRequest request) {
        try {
            Map<String, Object> response = exchangeForMap(patientServiceBaseUrl + "/api/patients?page=0&size=100", requestHeaders(request));
            Object content = response.get("content");
            return convertList(content, PatientSnapshot.class);
        } catch (RestClientException ex) {
            throw new IllegalStateException("Unable to fetch patient list from patient-service", ex);
        }
    }

    private VitalSummary fetchVitalSummary(String patientId, HttpServletRequest request) {
        if (patientId == null || patientId.isBlank()) {
            return VitalSummary.empty();
        }

        try {
            Map<String, Object> response = exchangeForMap(vitalsServiceBaseUrl + "/api/vitals/patient/" + patientId + "/summary", requestHeaders(request));
            List<VitalSnapshot> vitals = convertList(response.get("latestVitals"), VitalSnapshot.class);
            String timestamp = safe(response.get("timestamp"), null);
            return new VitalSummary(vitals, timestamp);
        } catch (RestClientException ex) {
            throw new IllegalStateException("Unable to fetch vitals summary from vitals-service", ex);
        }
    }

    private HttpHeaders requestHeaders(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (request == null) {
            return headers;
        }

        copyHeader(request, headers, "X-User-Role");
        copyHeader(request, headers, "X-User-Email");
        copyHeader(request, headers, "X-User-Department");
        copyHeader(request, headers, "X-User-Id");
        copyHeader(request, headers, "X-User-Display-Name");
        return headers;
    }

    private void copyHeader(HttpServletRequest request, HttpHeaders headers, String name) {
        String value = request.getHeader(name);
        if (value != null && !value.isBlank()) {
            headers.add(name, value);
        }
    }

    private Map<String, Object> exchangeForMap(String url, HttpHeaders headers) {
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        Map<?, ?> body = response.getBody();
        if (body == null) {
            return Map.of();
        }

        Map<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : body.entrySet()) {
            copy.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return copy;
    }

    private <T> List<T> convertList(Object value, Class<T> type) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }

        List<T> converted = new ArrayList<>();
        for (Object item : list) {
            converted.add(objectMapper.convertValue(item, type));
        }
        return converted;
    }

    private PatientSnapshot toPatientSnapshot(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalStateException("Patient-service returned an empty response");
        }
        return objectMapper.convertValue(value, PatientSnapshot.class);
    }

    private static String safe(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }

        String text = String.valueOf(value).trim();
        return text.isEmpty() ? fallback : text;
    }

    private String safeDisplayName(PatientSnapshot patient, String fallback) {
        if (patient == null) {
            return fallback;
        }

        String fullName = safe(patient.fullName, null);
        if (fullName != null) {
            return fullName;
        }

        String first = safe(patient.firstName, null);
        String last = safe(patient.lastName, null);
        if (first != null || last != null) {
            StringBuilder builder = new StringBuilder();
            if (first != null) {
                builder.append(first);
            }
            if (last != null) {
                if (builder.length() > 0) {
                    builder.append(' ');
                }
                builder.append(last);
            }
            String joined = builder.toString().trim();
            if (!joined.isBlank()) {
                return joined;
            }
        }

        return safe(patient.patientIdentifier, fallback);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }

        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }

    private static final class SummaryCard {
        private final String label;
        private final String value;

        private SummaryCard(String label, String value) {
            this.label = label;
            this.value = value;
        }
    }

    private static final class KeyValueRow {
        private final String label;
        private final String value;

        private KeyValueRow(String label, String value) {
            this.label = label;
            this.value = value;
        }
    }

    public static final class PatientSnapshot {
        public String id;
        public String patientIdentifier;
        public String firstName;
        public String lastName;
        public String fullName;
        public String department;
        public String wardNumber;
        public String bedNumber;
        public String clinicalStatus;
        public String assignedClinicianName;
        public String assignedClinicianEmail;

        public String displayName() {
            if (fullName != null && !fullName.isBlank()) {
                return fullName.trim();
            }

            String first = firstName == null ? "" : firstName.trim();
            String last = lastName == null ? "" : lastName.trim();
            String joined = (first + " " + last).trim();
            if (!joined.isBlank()) {
                return joined;
            }

            if (patientIdentifier != null && !patientIdentifier.isBlank()) {
                return patientIdentifier.trim();
            }

            return id != null ? "Patient " + id : "Patient";
        }
    }

    public static final class VitalSnapshot {
        public String vitalType;
        public String displayValue;
        public String vitalStatus;
        public String readingTimestamp;
        public String qualityScore;
        public Object value;
        public Object systolic;
        public Object diastolic;
        public String unit;

        public String displayValue() {
            if (displayValue != null && !displayValue.isBlank()) {
                return displayValue.trim();
            }

            if ("BLOOD_PRESSURE".equalsIgnoreCase(vitalType) && systolic != null && diastolic != null) {
                return (systolic + "/" + diastolic + " " + safe(unit, "mmHg")).trim();
            }

            if (value != null) {
                return (value + " " + safe(unit, "")).trim();
            }

            return "N/A";
        }
    }

    private static final class VitalSummary {
        private final List<VitalSnapshot> latestVitals;
        private final String timestamp;

        private VitalSummary(List<VitalSnapshot> latestVitals, String timestamp) {
            this.latestVitals = latestVitals;
            this.timestamp = timestamp;
        }

        private static VitalSummary empty() {
            return new VitalSummary(List.of(), null);
        }

        private String latestReadingLabel() {
            if (latestVitals.isEmpty()) {
                return "No recent reading";
            }

            VitalSnapshot first = latestVitals.get(0);
            return safe(first.vitalType, "Unknown") + " - " + first.displayValue();
        }
    }

    private static final class PatientShiftLine {
        private final PatientSnapshot patient;
        private final VitalSummary latestVitals;

        private PatientShiftLine(PatientSnapshot patient, VitalSummary latestVitals) {
            this.patient = Objects.requireNonNullElseGet(patient, PatientSnapshot::new);
            this.latestVitals = Objects.requireNonNullElseGet(latestVitals, VitalSummary::empty);
        }

        private boolean needsReview() {
            for (VitalSnapshot vital : latestVitals.latestVitals) {
                String status = vital.vitalStatus == null ? "" : vital.vitalStatus.toUpperCase();
                if ("HIGH".equals(status) || "LOW".equals(status) || "CRITICAL".equals(status)) {
                    return true;
                }
            }

            String clinicalStatus = patient.clinicalStatus == null ? "" : patient.clinicalStatus.toUpperCase();
            return !clinicalStatus.isBlank() && !"STABLE".equals(clinicalStatus) && !"DISCHARGED".equals(clinicalStatus);
        }
    }

    private static final class StaffIdentity {
        private final String name;
        private final String email;
        private final String role;
        private final String department;

        private StaffIdentity(String name, String email, String role, String department) {
            this.name = name;
            this.email = email;
            this.role = role;
            this.department = department;
        }

        private static StaffIdentity from(HttpServletRequest request) {
            if (request == null) {
                return new StaffIdentity("System", "system@meditrack.ai", "system", null);
            }

            return new StaffIdentity(
                request.getHeader("X-User-Display-Name"),
                request.getHeader("X-User-Email"),
                request.getHeader("X-User-Role"),
                request.getHeader("X-User-Department")
            );
        }

        private String nameOrEmail() {
            if (name != null && !name.isBlank()) {
                return name.trim();
            }
            if (email != null && !email.isBlank()) {
                return email.trim();
            }
            return "Unknown";
        }

        private String roleLabel() {
            if (role == null || role.isBlank()) {
                return "Unknown";
            }
            return role.trim();
        }
    }
}
