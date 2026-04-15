package com.meditrack.vitals.entity;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "patients")
public class Patient {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "patient_identifier", unique = true, nullable = false, length = 50)
    private String patientIdentifier;
    
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;
    
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;
    
    @Column(name = "date_of_birth", nullable = false)
    private java.time.LocalDate dateOfBirth;
    
    @Column(name = "gender", nullable = false, length = 20)
    private String gender;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    // Constructors
    public Patient() {}
    
    public Patient(String patientIdentifier, String firstName, String lastName, 
                java.time.LocalDate dateOfBirth, String gender) {
        this.patientIdentifier = patientIdentifier;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getPatientIdentifier() { return patientIdentifier; }
    public void setPatientIdentifier(String patientIdentifier) { this.patientIdentifier = patientIdentifier; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public java.time.LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(java.time.LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    // Utility methods
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Patient patient = (Patient) o;
        return Objects.equals(id, patient.id) && 
               Objects.equals(patientIdentifier, patient.patientIdentifier);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, patientIdentifier);
    }
    
    @Override
    public String toString() {
        return "Patient{" +
               "id=" + id +
               ", patientIdentifier='" + patientIdentifier + '\'' +
               ", firstName='" + firstName + '\'' +
               ", lastName='" + lastName + '\'' +
               '}';
    }
    
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    public int getAge() {
        return dateOfBirth != null ? 
            java.time.Period.between(dateOfBirth, java.time.LocalDate.now()).getYears() : 0;
    }
}
