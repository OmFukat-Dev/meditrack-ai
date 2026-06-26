package com.meditrack.patient.dto;

import com.meditrack.patient.entity.StaffMember;

import java.time.LocalDateTime;

public class StaffMemberResponse {

    private Long id;
    private String fullName;
    private String email;
    private String role;
    private String department;
    private String specialization;
    private String phoneNumber;
    private String licenseNumber;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static StaffMemberResponse fromEntity(StaffMember staffMember) {
        StaffMemberResponse response = new StaffMemberResponse();
        response.setId(staffMember.getId());
        response.setFullName(staffMember.getFullName());
        response.setEmail(staffMember.getEmail());
        response.setRole(staffMember.getRole() == null ? null : staffMember.getRole().name().toLowerCase());
        response.setDepartment(staffMember.getDepartment());
        response.setSpecialization(staffMember.getSpecialization());
        response.setPhoneNumber(staffMember.getPhoneNumber());
        response.setLicenseNumber(staffMember.getLicenseNumber());
        response.setActive(staffMember.getActive());
        response.setCreatedAt(staffMember.getCreatedAt());
        response.setUpdatedAt(staffMember.getUpdatedAt());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
