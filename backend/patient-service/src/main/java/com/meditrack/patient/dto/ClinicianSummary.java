package com.meditrack.patient.dto;

public class ClinicianSummary {

    private String clinicianName;
    private String clinicianEmail;
    private Long patientCount;

    public ClinicianSummary() {
    }

    public ClinicianSummary(String clinicianName, String clinicianEmail, Long patientCount) {
        this.clinicianName = clinicianName;
        this.clinicianEmail = clinicianEmail;
        this.patientCount = patientCount;
    }

    public String getClinicianName() {
        return clinicianName;
    }

    public void setClinicianName(String clinicianName) {
        this.clinicianName = clinicianName;
    }

    public String getClinicianEmail() {
        return clinicianEmail;
    }

    public void setClinicianEmail(String clinicianEmail) {
        this.clinicianEmail = clinicianEmail;
    }

    public Long getPatientCount() {
        return patientCount;
    }

    public void setPatientCount(Long patientCount) {
        this.patientCount = patientCount;
    }
}
