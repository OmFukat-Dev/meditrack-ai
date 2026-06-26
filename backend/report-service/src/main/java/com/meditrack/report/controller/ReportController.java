package com.meditrack.report.controller;

import com.meditrack.report.service.PdfGeneratorService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final PdfGeneratorService pdfGeneratorService;

    public ReportController(PdfGeneratorService pdfGeneratorService) {
        this.pdfGeneratorService = pdfGeneratorService;
    }

    @GetMapping("/patient/{patientId}/pdf")
    public ResponseEntity<byte[]> downloadPatientReport(@PathVariable String patientId, HttpServletRequest request) {
        return pdfResponse(pdfGeneratorService.generatePatientReport(patientId, request), "patient_report_" + patientId + ".pdf");
    }

    @GetMapping("/doctor/shift/pdf")
    public ResponseEntity<byte[]> downloadDoctorShiftReport(HttpServletRequest request) {
        String fileName = "doctor_shift_report.pdf";
        return pdfResponse(pdfGeneratorService.generateDoctorShiftReport(request), fileName);
    }

    @GetMapping("/nurse/handover/pdf")
    public ResponseEntity<byte[]> downloadNurseHandoverReport(HttpServletRequest request) {
        String fileName = "nurse_handover_report.pdf";
        return pdfResponse(pdfGeneratorService.generateNurseHandoverReport(request), fileName);
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] pdfBytes, String fileName) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", fileName);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        return ResponseEntity.ok()
            .headers(headers)
            .body(pdfBytes);
    }
}
