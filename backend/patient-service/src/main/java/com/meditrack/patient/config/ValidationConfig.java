package com.meditrack.patient.config;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;

@Component
public class ValidationConfig {
    
    // Custom constraint validators can be added here
    
    public static class PatientIdentifierValidator implements ConstraintValidator<String, String> {
        
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null) return false;
            
            // Patient identifier format: MED-YYYYMMDD-XXXX
            return value.matches("^MED-\\d{8}-\\d{4}$");
        }
    }
    
    public static class PhoneNumberValidator implements ConstraintValidator<String, String> {
        
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null) return true; // Optional field
            
            // Enhanced phone number validation
            String cleaned = value.replaceAll("[^0-9+]", "");
            return cleaned.length() >= 10 && cleaned.length() <= 15;
        }
    }
    
    public static class BloodTypeValidator implements ConstraintValidator<String, String> {
        
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null) return true; // Optional field
            
            // Enhanced blood type validation
            return value.matches("^(A|B|AB|O)[+-]?$");
        }
    }
    
    public static class Icd10CodeValidator implements ConstraintValidator<String, String> {
        
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null) return true; // Optional field
            
            // Basic ICD-10 format validation
            return value.matches("^[A-Z][0-9]{2}(?:\\.[0-9])?$");
        }
    }
    
    public static class MedicationRouteValidator implements ConstraintValidator<String, String> {
        
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null) return false;
            
            // Enhanced medication route validation
            return value.matches("^(ORAL|IV|IM|SC|TOPICAL|INHALATION|RECTAL|TRANSDERMAL|SUBCUTANEOUS|NASAL|OPHTHALMIC|OTIC|VAGINAL)$");
        }
    }
}
