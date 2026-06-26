package com.meditrack.alert.service;

import com.meditrack.alert.entity.Alert;
import com.meditrack.alert.repository.AlertRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private AlertService alertService;

    @Test
    void createAlertAppliesDefaultsAndPersistsEntity() {
        Alert alert = baseAlert();
        alert.setStatus(null);
        alert.setPriority(null);
        alert.setCreatedAt(null);

        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Alert created = alertService.createAlert(alert);

        assertNotNull(created.getCreatedAt());
        assertEquals(Alert.AlertStatus.ACTIVE, created.getStatus());
        assertEquals(Alert.AlertPriority.MEDIUM, created.getPriority());
        verify(alertRepository).save(alert);
    }

    @Test
    void processAlertByIdMarksAlertAsProcessed() {
        Alert alert = baseAlert();
        alert.setStatus(Alert.AlertStatus.ACTIVE);
        alert.setCreatedAt(LocalDateTime.now().minusMinutes(30));

        when(alertRepository.findById("alert-1")).thenReturn(Optional.of(alert));
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean result = alertService.processAlert("alert-1");

        assertTrue(result);
        assertEquals(Alert.AlertStatus.RESOLVED, alert.getStatus());
        assertNotNull(alert.getProcessedAt());
        verify(alertRepository, times(2)).save(alert);
    }

    @Test
    void undoAlertProcessingResetsStatus() {
        Alert alert = baseAlert();
        alert.setStatus(Alert.AlertStatus.RESOLVED);
        alert.setProcessedAt(LocalDateTime.now());
        alert.setEscalatedAt(LocalDateTime.now());

        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean result = alertService.undoAlertProcessing(alert);

        assertTrue(result);
        assertEquals(Alert.AlertStatus.ACTIVE, alert.getStatus());
        assertNull(alert.getProcessedAt());
        assertNull(alert.getEscalatedAt());
    }

    @Test
    void getAlertsReturnsMostRecentAlerts() {
        Alert first = baseAlert();
        first.setId("alert-1");
        first.setCreatedAt(LocalDateTime.now().minusMinutes(1));
        Alert second = baseAlert();
        second.setId("alert-2");
        second.setCreatedAt(LocalDateTime.now());

        Page<Alert> page = new PageImpl<>(List.of(second, first), PageRequest.of(0, 2), 2);
        when(alertRepository.findAll(any(Pageable.class))).thenReturn(page);

        List<Alert> alerts = alertService.getAlerts(2);

        assertEquals(2, alerts.size());
        assertEquals("alert-2", alerts.get(0).getId());
    }

    private Alert baseAlert() {
        Alert alert = new Alert();
        alert.setId("alert-1");
        alert.setAlertType("CRITICAL_HEART_RATE");
        alert.setPriority(Alert.AlertPriority.HIGH);
        alert.setMessage("Heart rate critical");
        alert.setPatientId("patient-1");
        alert.setVitalType("HEART_RATE");
        alert.setCreatedBy("system");
        alert.setCreatedAt(LocalDateTime.now());
        alert.setStatus(Alert.AlertStatus.ACTIVE);
        return alert;
    }
}
