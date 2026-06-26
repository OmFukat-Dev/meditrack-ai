package com.meditrack.notification.controller;

import com.meditrack.notification.dto.NotificationRequest;
import com.meditrack.notification.dto.NotificationResponse;
import com.meditrack.notification.dto.NotificationTemplateRequest;
import com.meditrack.notification.entity.NotificationEntity;
import com.meditrack.notification.entity.NotificationTemplateEntity;
import com.meditrack.notification.service.NotificationDispatchService;
import com.meditrack.notification.service.NotificationTemplateService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);

    private final NotificationDispatchService dispatchService;
    private final NotificationTemplateService templateService;

    public NotificationController(
        NotificationDispatchService dispatchService,
        NotificationTemplateService templateService
    ) {
        this.dispatchService = dispatchService;
        this.templateService = templateService;
    }

    @PostMapping
    public ResponseEntity<?> sendNotification(@Valid @RequestBody NotificationRequest request) {
        try {
            NotificationResponse response = dispatchService.sendNotification(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid notification request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", String.valueOf(e.getMessage())));
        } catch (Exception e) {
            logger.error("Failed to send notification", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to send notification", "message", String.valueOf(e.getMessage())));
        }
    }

    @GetMapping("/{notificationId}")
    public ResponseEntity<?> getNotification(@PathVariable Long notificationId) {
        try {
            NotificationEntity notification = dispatchService.getNotification(notificationId);
            return ResponseEntity.ok(notification);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Failed to get notification {}", notificationId, e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to get notification", "message", String.valueOf(e.getMessage())));
        }
    }

    @GetMapping
    public ResponseEntity<?> listNotifications(
        @RequestParam(required = false) String recipientId,
        @RequestParam(defaultValue = "50") int limit
    ) {
        try {
            if (recipientId != null && !recipientId.isBlank()) {
                return ResponseEntity.ok(dispatchService.getNotificationsByRecipient(recipientId, limit));
            }
            return ResponseEntity.ok(dispatchService.getRecentNotifications(limit));
        } catch (Exception e) {
            logger.error("Failed to list notifications", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to list notifications", "message", String.valueOf(e.getMessage())));
        }
    }

    @PostMapping("/{notificationId}/recall")
    public ResponseEntity<?> recallNotification(@PathVariable Long notificationId) {
        try {
            return ResponseEntity.ok(dispatchService.recallNotification(notificationId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Failed to recall notification {}", notificationId, e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to recall notification", "message", String.valueOf(e.getMessage())));
        }
    }

    @PostMapping("/templates")
    public ResponseEntity<?> saveTemplate(@Valid @RequestBody NotificationTemplateRequest request) {
        try {
            NotificationTemplateEntity template = templateService.saveTemplate(request);
            return ResponseEntity.ok(template);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", String.valueOf(e.getMessage())));
        } catch (Exception e) {
            logger.error("Failed to save notification template", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to save notification template", "message", String.valueOf(e.getMessage())));
        }
    }

    @GetMapping("/templates")
    public ResponseEntity<?> listTemplates() {
        try {
            return ResponseEntity.ok(templateService.listTemplates());
        } catch (Exception e) {
            logger.error("Failed to list notification templates", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to list notification templates", "message", String.valueOf(e.getMessage())));
        }
    }

    @GetMapping("/templates/{templateName}")
    public ResponseEntity<?> getTemplate(@PathVariable String templateName) {
        try {
            NotificationTemplateEntity template = templateService.getTemplate(templateName).orElse(null);
            if (template != null) {
                return ResponseEntity.ok(template);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Failed to get notification template {}", templateName, e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to get notification template", "message", String.valueOf(e.getMessage())));
        }
    }
}
