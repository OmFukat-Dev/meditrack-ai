package com.meditrack.patient.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class PatientResponse {
    private Long id;
    private String patientIdentifier;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String gender;
    private String bloodType;
    private String phoneNumber;
    private String email;
    private String address;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String createdBy;
    private String updatedBy;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Computed fields
    private String fullName;
    private Integer age;
    
    // Relationships (simplified for response)
    private List<MedicalHistorySummary> medicalHistory;
    private List<AllergySummary> allergies;
    private List<MedicationSummary> medications;
    
    // Constructors
    public PatientResponse() {}
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getPatientIdentifier() { return patientIdentifier; }
    public void setPatientIdentifier(String patientIdentifier) { this.patientIdentifier = patientIdentifier; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    
    public String getBloodType() { return bloodType; }
    public void setBloodType(String bloodType) { this.bloodType = bloodType; }
    
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    public String getEmergencyContactName() { return emergencyContactName; }
    public void setEmergencyContactName(String emergencyContactName) { this.emergencyContactName = emergencyContactName; }
    
    public String getEmergencyContactPhone() { return emergencyContactPhone; }
    public void setEmergencyContactPhone(String emergencyContactPhone) { this.emergencyContactPhone = emergencyContactPhone; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    
    public List<MedicalHistorySummary> getMedicalHistory() { return medicalHistory; }
    public void setMedicalHistory(List<MedicalHistorySummary> medicalHistory) { this.medicalHistory = medicalHistory; }
    
    public List<AllergySummary> getAllergies() { return allergies; }
    public void setAllergies(List<AllergySummary> allergies) { this.allergies = allergies; }
    
    public List<MedicationSummary> getMedications() { return medications; }
    public void setMedications(List<MedicationSummary> medications) { this.medications = medications; }
    
    // Inner summary classes for response optimization
    public static class MedicalHistorySummary {
        private Long id;
        private String conditionName;
        private String severity;
        private String status;
        private LocalDate diagnosisDate;
        
        // Constructors, getters, setters
        public MedicalHistorySummary() {}
        
        public MedicalHistorySummary(Long id, String conditionName, String severity, String status, LocalDate diagnosisDate) {
            this.id = id;
            this.conditionName = conditionName;
            this.severity = severity;
            this.status = status;
            this.diagnosisDate = diagnosisDate;
        }
        
        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getConditionName() { return conditionName; }
        public void setConditionName(String conditionName) { this.conditionName = conditionName; }
        
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public LocalDate getDiagnosisDate() { return diagnosisDate; }
        public void setDiagnosisDate(LocalDate diagnosisDate) { this.diagnosisDate = diagnosisDate; }
    }
    
    public static class AllergySummary {
        private Long id;
        private String allergen;
        private String allergyType;
        private String severity;
        
        // Constructors, getters, setters
        public AllergySummary() {}
        
        public AllergySummary(Long id, String allergen, String allergyType, String severity) {
            this.id = id;
            this.allergen = allergen;
            this.allergyType = allergyType;
            this.severity = severity;
        }
        
        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getAllergen() { return allergen; }
        public void setAllergen(String allergen) { this.allergen = allergen; }
        
        public String getAllergyType() { return allergyType; }
        public void setAllergyType(String allergyType) { this.allergyType = allergyType; }
        
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
    }
    
    public static class MedicationSummary {
        private Long id;
        private String medicationName;
        private String dosage;
        private String frequency;
        private String route;
        private Boolean isActive;
        
        // Constructors, getters, setters
        public MedicationSummary() {}
        
        public MedicationSummary(Long id, String medicationName, String dosage, String frequency, String route, Boolean isActive) {
            this.id = id;
            this.medicationName = medicationName;
            this.dosage = dosage;
            this.frequency = frequency;
            this.route = route;
            this.isActive = isActive;
        }
        
        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getMedicationName() { return medicationName; }
        public void setMedicationName(String medicationName) { this.medicationName = medicationName; }
        
        public String getDosage() { return dosage; }
        public void setDosage(String dosage) { this.dosage = dosage; }
        
        public String getFrequency() { return frequency; }
        public void setFrequency(String frequency) { this.frequency = frequency; }
        
        public String getRoute() { return route; }
        public void setRoute(String route) { this.route = route; }
        
        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    }
}
