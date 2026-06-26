package com.meditrack.vitals.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ErrorHandlerServiceTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ErrorHandlerService errorHandlerService;

    @Test
    void handleErrorCategorizesValidationError() {
        ErrorHandlerService.ErrorResult result = errorHandlerService.handleError(
            new IllegalArgumentException("Invalid value provided"),
            "CREATE_VITAL",
            Map.of("patientId", "123")
        );

        assertFalse(result.shouldRetry());
        assertEquals("VALIDATION_ERROR", result.getErrorCategory());
        assertTrue(result.getUserMessage().contains("Invalid data"));
        verify(kafkaTemplate).send(eq("error-events"), anyString());
        assertEquals(1, errorHandlerService.getErrorStats().values().iterator().next().getTotalCount());
    }

    @Test
    void handleErrorCategorizesNetworkError() {
        ErrorHandlerService.ErrorResult result = errorHandlerService.handleError(
            new RuntimeException("connection timeout"),
            "READ_PATIENT_VITALS",
            Map.of()
        );

        assertTrue(result.shouldRetry());
        assertEquals("NETWORK_ERROR", result.getErrorCategory());
        verify(kafkaTemplate).send(eq("error-events"), anyString());
    }
}
