package com.meditrack.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meditrack.notification.domain.NotificationChannel;
import com.meditrack.notification.domain.NotificationType;
import com.meditrack.notification.dto.NotificationTemplateRequest;
import com.meditrack.notification.entity.NotificationTemplateEntity;
import com.meditrack.notification.repository.NotificationTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class NotificationTemplateService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationTemplateService.class);

    private final NotificationTemplateRepository templateRepository;
    private final ObjectMapper objectMapper;

    public NotificationTemplateService(
        NotificationTemplateRepository templateRepository,
        ObjectMapper objectMapper
    ) {
        this.templateRepository = templateRepository;
        this.objectMapper = objectMapper;
    }

    public NotificationTemplateEntity saveTemplate(NotificationTemplateRequest request) {
        NotificationTemplateEntity template = templateRepository
            .findByTemplateName(request.getTemplateName())
            .orElseGet(NotificationTemplateEntity::new);

        template.setTemplateName(request.getTemplateName());
        template.setTemplateType(request.getTemplateType());
        template.setChannelType(request.getChannelType());
        template.setSubjectTemplate(request.getSubjectTemplate());
        template.setBodyTemplate(request.getBodyTemplate());
        template.setTemplateVariablesJson(serialize(request.getTemplateVariables()));
        template.setCssStyles(request.getCssStyles());
        template.setHtml(request.getHtml());
        template.setActive(request.getActive());
        if (request.getCreatedBy() != null) {
            template.setCreatedBy(request.getCreatedBy());
        }
        if (request.getUpdatedBy() != null) {
            template.setUpdatedBy(request.getUpdatedBy());
        }

        return templateRepository.save(template);
    }

    public Optional<NotificationTemplateEntity> getTemplate(String templateName) {
        return templateRepository.findByTemplateNameAndActiveTrue(templateName);
    }

    public Optional<NotificationTemplateEntity> getTemplate(String templateName, NotificationChannel channelType) {
        return templateRepository.findByTemplateNameAndActiveTrue(templateName)
            .filter(template -> template.getChannelType() == channelType);
    }

    public List<NotificationTemplateEntity> listTemplates() {
        return templateRepository.findAllByOrderByTemplateNameAsc();
    }

    public List<NotificationTemplateEntity> listTemplates(NotificationType type, NotificationChannel channelType) {
        return templateRepository.findByTemplateTypeAndChannelTypeAndActiveTrue(type, channelType);
    }

    public Map<String, Object> extractTemplateVariables(NotificationTemplateEntity template) {
        if (template == null || template.getTemplateVariablesJson() == null || template.getTemplateVariablesJson().isBlank()) {
            return Collections.emptyMap();
        }

        try {
            Map<String, Object> values = objectMapper.readValue(
                template.getTemplateVariablesJson(),
                new TypeReference<LinkedHashMap<String, Object>>() {}
            );
            return values == null ? Collections.emptyMap() : values;
        } catch (Exception e) {
            logger.warn("Failed to parse template variables for template {}: {}", template.getTemplateName(), e.getMessage());
            return Collections.emptyMap();
        }
    }

    public String renderTemplate(String template, Map<String, Object> variables) {
        if (template == null) {
            return "";
        }

        String rendered = template;
        if (variables == null || variables.isEmpty()) {
            return rendered;
        }

        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = String.valueOf(entry.getKey());
            String replacement = entry.getValue() == null ? "" : String.valueOf(entry.getValue());
            rendered = rendered.replace("{{" + placeholder + "}}", replacement);
            rendered = rendered.replace("${" + placeholder + "}", replacement);
        }
        return rendered;
    }

    private String serialize(Map<String, Object> variables) {
        if (variables == null || variables.isEmpty()) {
            return "{}";
        }

        try {
            return objectMapper.writeValueAsString(variables);
        } catch (JsonProcessingException e) {
            logger.warn("Failed to serialize template variables: {}", e.getMessage());
            return "{}";
        }
    }
}
