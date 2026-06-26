package com.meditrack.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meditrack.notification.domain.NotificationChannel;
import com.meditrack.notification.domain.NotificationType;
import com.meditrack.notification.dto.NotificationTemplateRequest;
import com.meditrack.notification.entity.NotificationTemplateEntity;
import com.meditrack.notification.repository.NotificationTemplateRepository;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificationTemplateServiceTest {

    private final NotificationTemplateRepository templateRepository = mock(NotificationTemplateRepository.class);
    private final NotificationTemplateService templateService = new NotificationTemplateService(templateRepository, new ObjectMapper());

    @Test
    void renderTemplateReplacesPlaceholders() {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("name", "Alice");
        variables.put("value", 42);

        String rendered = templateService.renderTemplate("Hello {{name}}, value ${value}", variables);

        assertEquals("Hello Alice, value 42", rendered);
    }

    @Test
    void extractTemplateVariablesParsesJsonPayload() {
        NotificationTemplateEntity template = new NotificationTemplateEntity();
        template.setTemplateName("critical-email");
        template.setTemplateVariablesJson("{\"name\":\"Alice\",\"value\":42}");

        Map<String, Object> variables = templateService.extractTemplateVariables(template);

        assertEquals("Alice", variables.get("name"));
        assertEquals(42, ((Number) variables.get("value")).intValue());
    }

    @Test
    void saveTemplateCreatesNewTemplateWhenMissing() {
        NotificationTemplateRequest request = new NotificationTemplateRequest();
        request.setTemplateName("critical-email");
        request.setTemplateType(NotificationType.ALERT);
        request.setChannelType(NotificationChannel.EMAIL);
        request.setSubjectTemplate("Alert for {{recipientId}}");
        request.setBodyTemplate("Vitals are critical for {{recipientId}}");
        request.setTemplateVariables(Map.of("recipientId", "patient-1"));
        request.setCssStyles("body { color: red; }");
        request.setHtml(Boolean.TRUE);
        request.setActive(Boolean.TRUE);
        request.setCreatedBy("system");
        request.setUpdatedBy("system");

        when(templateRepository.findByTemplateName(eq("critical-email"))).thenReturn(Optional.empty());
        when(templateRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationTemplateEntity saved = templateService.saveTemplate(request);

        assertEquals("critical-email", saved.getTemplateName());
        assertEquals(NotificationType.ALERT, saved.getTemplateType());
        assertEquals(NotificationChannel.EMAIL, saved.getChannelType());
        assertTrue(saved.getHtml());
        assertTrue(saved.getActive());
        assertFalse(saved.getBodyTemplate().isBlank());
    }
}
