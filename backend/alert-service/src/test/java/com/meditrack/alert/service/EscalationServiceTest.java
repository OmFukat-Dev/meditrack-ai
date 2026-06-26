package com.meditrack.alert.service;

import com.meditrack.alert.entity.Alert;
import com.meditrack.alert.entity.EscalationRule;
import com.meditrack.alert.repository.EscalationRuleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

@ExtendWith(MockitoExtension.class)
class EscalationServiceTest {

    @Mock
    private EscalationRuleRepository escalationRuleRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private EscalationService escalationService;

    @Test
    void checkAndEscalateAlertExecutesMatchingRule() {
        Alert alert = new Alert();
        alert.setId("alert-1");
        alert.setAlertType("CRITICAL_HEART_RATE");
        alert.setPriority(Alert.AlertPriority.HIGH);
        alert.setMessage("Heart rate critical");
        alert.setCreatedAt(LocalDateTime.now().minusHours(1));

        EscalationRule rule = baseRule();
        when(escalationRuleRepository.findByAlertTypeAndPriorityAndActive(
            "CRITICAL_HEART_RATE",
            EscalationRule.AlertPriority.HIGH,
            true
        )).thenReturn(List.of(rule));
        when(notificationService.sendEscalationNotification(eq("doctor@example.com"), anyString(), eq("L1")))
            .thenReturn(true);

        boolean result = escalationService.checkAndEscalateAlert(alert);

        assertTrue(result);
        assertTrue(escalationService.getActiveEscalations().containsKey("rule-1-alert-1"));
        verify(notificationService).sendEscalationNotification(eq("doctor@example.com"), argThat(containsString("Escalate alert")), eq("L1"));
        verify(auditService).logEscalation(rule, alert, true);
    }

    @Test
    void executeAndUndoEscalationReleasesContext() {
        EscalationRule rule = baseRule();
        when(notificationService.sendEscalationNotification(anyString(), anyString(), anyString())).thenReturn(true);
        when(notificationService.sendEscalationRecallNotification(anyString(), anyString())).thenReturn(true);

        assertTrue(escalationService.executeEscalation(rule));
        assertTrue(escalationService.getActiveEscalations().containsKey("rule-1"));

        assertTrue(escalationService.undoEscalation(rule));
        assertFalse(escalationService.getActiveEscalations().containsKey("rule-1"));
        verify(notificationService).sendEscalationRecallNotification(eq("doctor@example.com"), argThat(containsString("recalled")));
        verify(auditService).logEscalationRecall(rule, true);
    }

    private EscalationRule baseRule() {
        EscalationRule rule = new EscalationRule();
        rule.setId("rule-1");
        rule.setName("Critical heart rate escalation");
        rule.setAlertType("CRITICAL_HEART_RATE");
        rule.setPriority(EscalationRule.AlertPriority.HIGH);
        rule.setEscalationLevel("L1");
        rule.setTargetRole("DOCTOR");
        rule.setEscalationRecipients(List.of("doctor@example.com"));
        rule.setEscalationDelayMinutes(0);
        rule.setTimeBasedEscalation(false);
        rule.setConditionBasedEscalation(false);
        rule.setPriorityBasedEscalation(true);
        rule.setMinPriorityLevel(EscalationRule.AlertPriority.MEDIUM);
        rule.setEscalationCondition("priority >= HIGH");
        rule.setEscalationMessage("Escalate alert");
        rule.setActive(true);
        rule.setCreatedAt(LocalDateTime.now().minusDays(1));
        rule.setUpdatedAt(LocalDateTime.now());
        return rule;
    }
}
