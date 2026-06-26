package com.meditrack.notification.dto;

import com.meditrack.notification.domain.NotificationChannel;
import com.meditrack.notification.domain.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

public class NotificationTemplateRequest {

    @NotBlank
    private String templateName;

    @NotNull
    private NotificationType templateType;

    @NotNull
    private NotificationChannel channelType;

    private String subjectTemplate;

    @NotBlank
    private String bodyTemplate;

    private Map<String, Object> templateVariables = new LinkedHashMap<>();

    private String cssStyles;

    private Boolean html = Boolean.FALSE;

    private Boolean active = Boolean.TRUE;

    private String createdBy;

    private String updatedBy;

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public NotificationType getTemplateType() {
        return templateType;
    }

    public void setTemplateType(NotificationType templateType) {
        this.templateType = templateType;
    }

    public NotificationChannel getChannelType() {
        return channelType;
    }

    public void setChannelType(NotificationChannel channelType) {
        this.channelType = channelType;
    }

    public String getSubjectTemplate() {
        return subjectTemplate;
    }

    public void setSubjectTemplate(String subjectTemplate) {
        this.subjectTemplate = subjectTemplate;
    }

    public String getBodyTemplate() {
        return bodyTemplate;
    }

    public void setBodyTemplate(String bodyTemplate) {
        this.bodyTemplate = bodyTemplate;
    }

    public Map<String, Object> getTemplateVariables() {
        return templateVariables;
    }

    public void setTemplateVariables(Map<String, Object> templateVariables) {
        this.templateVariables = templateVariables;
    }

    public String getCssStyles() {
        return cssStyles;
    }

    public void setCssStyles(String cssStyles) {
        this.cssStyles = cssStyles;
    }

    public Boolean getHtml() {
        return html;
    }

    public void setHtml(Boolean html) {
        this.html = html;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
