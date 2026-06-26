package com.meditrack.notification.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VitalReadingEvent {
    private String patientId;
    private String vitalType;
    private Double value;
    private String unit;
    private String eventType;
    private String riskLevel;
    private Double confidence;
    private String severity;
    private String timestamp;
    private String department;
    private String createdBy;
    private String role;
}
