package com.meditrack.vitals.controller;

import com.meditrack.vitals.dto.VitalReadingMessage;
import com.meditrack.vitals.entity.Patient;
import com.meditrack.vitals.entity.VitalReading;
import com.meditrack.vitals.service.ErrorHandlerService;
import com.meditrack.vitals.service.RateLimitingService;
import com.meditrack.vitals.service.VitalService;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.standaloneSetup;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VitalControllerRestAssuredTest {

    @Mock
    private VitalService vitalService;

    @Mock
    private RateLimitingService rateLimitingService;

    @Mock
    private ErrorHandlerService errorHandlerService;

    @InjectMocks
    private VitalController vitalController;

    @BeforeEach
    void setUp() {
        standaloneSetup(vitalController);
    }

    @Test
    void createVitalReadingReturnsCreatedEntity() {
        when(rateLimitingService.checkVitalIngestionRateLimit("pat-001", "HEART_RATE"))
            .thenReturn(new RateLimitingService.RateLimitResult(true, 99, System.currentTimeMillis() + 60_000, "vital_ingestion_minute"));

        VitalReading savedReading = new VitalReading();
        savedReading.setId(55L);
        savedReading.setPatient(patient());
        savedReading.setVitalType("HEART_RATE");
        savedReading.setValue(new BigDecimal("72"));
        savedReading.setUnit("bpm");
        savedReading.setReadingTimestamp(LocalDateTime.of(2026, 4, 23, 10, 15, 30));
        savedReading.setSource("API");
        savedReading.setQualityScore(new BigDecimal("0.95"));

        when(vitalService.processVitalReading(any(VitalReadingMessage.class))).thenReturn(savedReading);

        given()
            .contentType(ContentType.JSON)
            .header("X-User-Role", "SYSTEM")
            .header("X-User-Department", "CARDIOLOGY")
            .body("""
                {
                  "patientIdentifier": "pat-001",
                  "vitalType": "HEART_RATE",
                  "value": 72,
                  "unit": "bpm",
                  "readingTimestamp": "2026-04-23T10:15:30",
                  "source": "API",
                  "notes": "Routine heart rate measurement",
                  "nurseId": "nurse-123",
                  "department": "CARDIOLOGY"
                }
                """)
        .when()
            .post("/api/vitals")
        .then()
            .statusCode(201)
            .body("id", equalTo(55))
            .body("patient.patientIdentifier", equalTo("PAT-001"))
            .body("vitalType", equalTo("HEART_RATE"))
            .body("displayValue", equalTo("72 bpm"));
    }

    @Test
    void createVitalReadingReturnsTooManyRequestsWhenRateLimited() {
        when(rateLimitingService.checkVitalIngestionRateLimit("pat-001", "HEART_RATE"))
            .thenReturn(new RateLimitingService.RateLimitResult(false, 0, 123L, "vital_ingestion_minute", "Rate limit exceeded, try again later"));

        given()
            .contentType(ContentType.JSON)
            .header("X-User-Role", "SYSTEM")
            .header("X-User-Department", "CARDIOLOGY")
            .body("""
                {
                  "patientIdentifier": "pat-001",
                  "vitalType": "HEART_RATE",
                  "value": 72,
                  "unit": "bpm",
                  "readingTimestamp": "2026-04-23T10:15:30",
                  "source": "API",
                  "notes": "Routine heart rate measurement",
                  "nurseId": "nurse-123",
                  "department": "CARDIOLOGY"
                }
                """)
        .when()
            .post("/api/vitals")
        .then()
            .statusCode(429)
            .body("error", equalTo("Rate limit exceeded"))
            .body("message", containsString("try again later"));
    }

    private Patient patient() {
        Patient patient = new Patient();
        patient.setId(1L);
        patient.setPatientIdentifier("PAT-001");
        patient.setFirstName("Alice");
        patient.setLastName("Smith");
        patient.setDateOfBirth(LocalDate.now().minusYears(30));
        patient.setGender("FEMALE");
        return patient;
    }
}
