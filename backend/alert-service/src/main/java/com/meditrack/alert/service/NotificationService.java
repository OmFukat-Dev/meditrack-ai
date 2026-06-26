package com.meditrack.alert.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meditrack.alert.entity.Notification;
import com.meditrack.alert.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.meditrack.security.ServiceJwtHttpInterceptor;
import com.meditrack.security.ServiceJwtUtil;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final String notificationServiceBaseUrl;
    private final Map<String, NotificationContext> activeNotifications = new ConcurrentHashMap<>();

    public NotificationService(
        NotificationRepository notificationRepository,
        AuditService auditService,
        ObjectMapper objectMapper,
        RestTemplateBuilder restTemplateBuilder,
        ServiceJwtUtil serviceJwtUtil,
        @Value("${meditrack.alert.notification.service-base-url:http://localhost:8086}") String notificationServiceBaseUrl
    ) {
        this.notificationRepository = notificationRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplateBuilder
            .additionalInterceptors(new ServiceJwtHttpInterceptor(serviceJwtUtil, "alert-service", "notification-service"))
            .build();
        this.notificationServiceBaseUrl = normalizeBaseUrl(notificationServiceBaseUrl);
    }

    public boolean sendNotification(Notification notification) {
        try {
            ensureNotificationDefaults(notification);
            validateNotification(notification);
            notification.setProviderReference(null);
            notification.setFailureReason(null);
            notification.setSentAt(null);
            notification.setDeliveredAt(null);
            notification.setReadAt(null);
            notification.setRecall(false);
            notification.setRecallAt(null);

            NotificationContext context = new NotificationContext(notification, LocalDateTime.now());
            activeNotifications.put(notification.getId(), context);

            notification.setStatus(com.meditrack.alert.entity.Notification.NotificationStatus.PENDING);
            notificationRepository.save(notification);

            NotificationDispatchResult result = dispatchNotification(notification);
            com.meditrack.alert.entity.Notification.NotificationStatus entityStatus = mapToEntityStatus(result);
            boolean success = isSuccessful(entityStatus);

            applyNotificationOutcome(notification, context, result, entityStatus, success);
            notificationRepository.save(notification);

            auditService.logNotification(notification, success);

            logger.info(
                "Notification {} {} via {}",
                notification.getId(),
                success ? "sent" : "failed",
                notification.getNotificationType()
            );
            return success;
        } catch (Exception e) {
            logger.error("Error sending notification: {}", notification == null ? "null" : notification.getId(), e);

            if (notification != null) {
                try {
                    ensureNotificationDefaults(notification);
                    notification.setStatus(com.meditrack.alert.entity.Notification.NotificationStatus.FAILED);
                    notification.setFailureReason(e.getMessage());
                    notificationRepository.save(notification);

                    NotificationContext context = activeNotifications.get(notification.getId());
                    if (context != null) {
                        context.setStatus(NotificationStatus.FAILED);
                    }
                } catch (Exception saveException) {
                    logger.error("Failed to persist notification failure for {}", notification.getId(), saveException);
                }

                auditService.logNotification(notification, false);
            }

            return false;
        }
    }

    public boolean sendMultiChannelNotification(Notification notification, List<NotificationChannel> channels) {
        try {
            ensureNotificationDefaults(notification);
            validateNotificationBasics(notification);
            notification.setProviderReference(null);
            notification.setFailureReason(null);
            notification.setSentAt(null);
            notification.setDeliveredAt(null);
            notification.setReadAt(null);
            notification.setRecall(false);
            notification.setRecallAt(null);

            Set<NotificationChannel> resolvedChannels = resolveMultiChannelTargets(notification, channels);
            if (resolvedChannels.isEmpty()) {
                throw new IllegalArgumentException("No notification channels provided");
            }

            if (notification.getNotificationType() == null) {
                notification.setNotificationType(toEntityChannel(resolvedChannels.iterator().next()));
            }

            Map<String, String> providerReferences = new LinkedHashMap<>();
            List<NotificationStatus> childStatuses = new ArrayList<>();
            boolean allSuccess = true;

            for (NotificationChannel channel : resolvedChannels) {
                Notification channelNotification = createChannelNotification(notification, channel);
                boolean success = sendNotification(channelNotification);
                allSuccess = allSuccess && success;
                childStatuses.add(mapToServiceStatus(channelNotification.getStatus()));

                if (channelNotification.getProviderReference() != null && !channelNotification.getProviderReference().isBlank()) {
                    providerReferences.put(channel.name(), channelNotification.getProviderReference());
                }
            }

            notification.setStatus(mapToEntityStatus(summarizeMultiChannelStatus(childStatuses)));
            if (notification.getStatus() == com.meditrack.alert.entity.Notification.NotificationStatus.DELIVERED
                || notification.getStatus() == com.meditrack.alert.entity.Notification.NotificationStatus.SENT) {
                notification.setSentAt(LocalDateTime.now());
                if (notification.getStatus() == com.meditrack.alert.entity.Notification.NotificationStatus.DELIVERED) {
                    notification.setDeliveredAt(notification.getSentAt());
                }
            }
            notification.setRecall(false);
            notification.setRecallAt(null);
            notification.setFailureReason(allSuccess ? null : "One or more notification channels failed");
            notification.setProviderReference(serializeProviderReferences(providerReferences));
            notificationRepository.save(notification);

            NotificationContext context = new NotificationContext(notification, LocalDateTime.now());
            context.setStatus(mapToServiceStatus(notification.getStatus()));
            context.setSentAt(notification.getSentAt());
            activeNotifications.put(notification.getId(), context);

            auditService.logNotification(notification, allSuccess);
            return allSuccess;
        } catch (Exception e) {
            logger.error("Error sending multi-channel notification: {}", notification == null ? "null" : notification.getId(), e);
            if (notification != null) {
                try {
                    ensureNotificationDefaults(notification);
                    notification.setStatus(com.meditrack.alert.entity.Notification.NotificationStatus.FAILED);
                    notification.setFailureReason(e.getMessage());
                    notificationRepository.save(notification);
                    auditService.logNotification(notification, false);
                } catch (Exception saveException) {
                    logger.error("Failed to persist multi-channel notification failure for {}", notification.getId(), saveException);
                }
            }
            return false;
        }
    }

    public boolean sendEscalationNotification(String recipient, String message, String escalationLevel) {
        try {
            Notification notification = new Notification();
            notification.setId("escalation-" + UUID.randomUUID());
            notification.setRecipient(recipient);
            notification.setMessage(message);
            notification.setNotificationType(com.meditrack.alert.entity.Notification.NotificationType.EMAIL);
            notification.setPriority(com.meditrack.alert.entity.Notification.NotificationPriority.HIGH);
            notification.setCreatedAt(LocalDateTime.now());
            notification.setEscalationLevel(escalationLevel);
            return sendNotification(notification);
        } catch (Exception e) {
            logger.error("Error sending escalation notification to: {}", recipient, e);
            return false;
        }
    }

    public boolean sendEscalationRecallNotification(String recipient, String message) {
        try {
            Notification notification = new Notification();
            notification.setId("recall-" + UUID.randomUUID());
            notification.setRecipient(recipient);
            notification.setMessage(message);
            notification.setNotificationType(com.meditrack.alert.entity.Notification.NotificationType.EMAIL);
            notification.setPriority(com.meditrack.alert.entity.Notification.NotificationPriority.MEDIUM);
            notification.setCreatedAt(LocalDateTime.now());
            notification.setRecall(true);
            return sendNotification(notification);
        } catch (Exception e) {
            logger.error("Error sending escalation recall notification to: {}", recipient, e);
            return false;
        }
    }

    public boolean recallNotification(Notification notification) {
        try {
            Notification persisted = notificationRepository.findById(notification.getId()).orElse(notification);
            if (persisted.getProviderReference() == null || persisted.getProviderReference().isBlank()) {
                logger.warn("No provider reference stored for notification {}", persisted.getId());
                return false;
            }

            List<String> providerReferences = extractProviderReferences(persisted.getProviderReference());
            if (providerReferences.isEmpty()) {
                logger.warn("No recall targets resolved for notification {}", persisted.getId());
                return false;
            }

            boolean allSuccess = true;
            for (String providerReference : providerReferences) {
                NotificationDispatchResult result = recallNotificationByProviderReference(providerReference);
                allSuccess = allSuccess && result != null && isRecallSuccessful(result);
            }

            persisted.setRecall(true);
            persisted.setRecallAt(LocalDateTime.now());
            persisted.setStatus(allSuccess
                ? com.meditrack.alert.entity.Notification.NotificationStatus.RECALLED
                : com.meditrack.alert.entity.Notification.NotificationStatus.FAILED);
            persisted.setFailureReason(allSuccess ? null : "One or more recall requests failed");
            notificationRepository.save(persisted);

            NotificationContext context = activeNotifications.get(persisted.getId());
            if (context != null) {
                context.setStatus(allSuccess ? NotificationStatus.RECALLED : NotificationStatus.FAILED);
            }

            auditService.logNotificationRecall(persisted, allSuccess);
            return allSuccess;
        } catch (Exception e) {
            logger.error("Error recalling notification: {}", notification == null ? "null" : notification.getId(), e);
            return false;
        }
    }

    public NotificationStatus getNotificationStatus(String notificationId) {
        NotificationContext context = activeNotifications.get(notificationId);
        if (context != null) {
            return context.getStatus();
        }

        Notification notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification != null && notification.getStatus() != null) {
            return mapToServiceStatus(notification.getStatus());
        }

        return NotificationStatus.NOT_FOUND;
    }

    public Map<String, NotificationContext> getActiveNotifications() {
        return new ConcurrentHashMap<>(activeNotifications);
    }

    public List<Notification> getNotificationHistory(int limit) {
        try {
            int pageSize = Math.max(1, Math.min(limit, 100));
            return notificationRepository.findAll(
                PageRequest.of(0, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"))
            ).getContent();
        } catch (Exception e) {
            logger.error("Error getting notification history", e);
            return List.of();
        }
    }

    private NotificationDispatchResult dispatchNotification(Notification notification) {
        NotificationChannel channel = NotificationChannel.valueOf(notification.getNotificationType().name());
        NotificationDispatchRequest request = buildDispatchRequest(notification, channel);

        try {
            NotificationDispatchResponse response = restTemplate.postForObject(
                buildUrl("/api/notifications"),
                request,
                NotificationDispatchResponse.class
            );

            if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
                return NotificationDispatchResult.failed(channel.name(), "No response from notification service");
            }

            NotificationDispatchResult result = findMatchingResult(response.getResults(), channel.name());
            if (result == null) {
                result = response.getResults().get(0);
            }
            return result;
        } catch (Exception e) {
            logger.error("Failed to dispatch notification {} via {}", notification.getId(), channel, e);
            return NotificationDispatchResult.failed(channel.name(), e.getMessage());
        }
    }

    private NotificationDispatchResult recallNotificationByProviderReference(String providerReference) {
        try {
            return restTemplate.postForObject(
                buildUrl("/api/notifications/" + providerReference + "/recall"),
                null,
                NotificationDispatchResult.class
            );
        } catch (Exception e) {
            logger.error("Failed to recall notification provider reference {}", providerReference, e);
            return NotificationDispatchResult.failed("RECALL", e.getMessage());
        }
    }

    private NotificationDispatchRequest buildDispatchRequest(Notification notification, NotificationChannel channel) {
        NotificationDispatchRequest request = new NotificationDispatchRequest();
        request.setRecipientId(notification.getRecipient());
        request.setRecipientType(resolveRecipientType(channel));
        request.setNotificationType(notification.getEscalationLevel() != null ? "ESCALATION" : "ALERT");
        request.setChannels(List.of(channel.name()));
        request.setSubject(resolveSubject(notification));
        request.setMessage(notification.getMessage());
        request.setPriorityLevel(resolvePriorityLevel(notification.getPriority()));
        request.setSourceService("alert-service");
        request.setSourceReference(notification.getId());
        request.setTemplateVariables(new LinkedHashMap<>());
        return request;
    }

    private void applyNotificationOutcome(
        Notification notification,
        NotificationContext context,
        NotificationDispatchResult result,
        com.meditrack.alert.entity.Notification.NotificationStatus entityStatus,
        boolean success
    ) {
        LocalDateTime now = LocalDateTime.now();
        notification.setStatus(entityStatus);
        notification.setFailureReason(success ? null : firstNonBlank(result == null ? null : result.getFailureReason(), result == null ? null : result.getResponseMessage()));

        if (success) {
            if (entityStatus == com.meditrack.alert.entity.Notification.NotificationStatus.SCHEDULED) {
                notification.setSentAt(null);
                notification.setDeliveredAt(null);
            } else {
                notification.setSentAt(result != null && result.getSentAt() != null ? result.getSentAt() : now);
                if (entityStatus == com.meditrack.alert.entity.Notification.NotificationStatus.DELIVERED) {
                    notification.setDeliveredAt(notification.getSentAt());
                } else {
                    notification.setDeliveredAt(null);
                }
            }
        } else {
            notification.setSentAt(null);
            notification.setDeliveredAt(null);
        }

        if (result != null && result.getNotificationId() != null) {
            notification.setProviderReference(String.valueOf(result.getNotificationId()));
        }

        notification.setRecall(false);
        notification.setRecallAt(null);

        context.setStatus(mapToServiceStatus(entityStatus));
        context.setSentAt(notification.getSentAt());
    }

    private void ensureNotificationDefaults(Notification notification) {
        if (notification.getId() == null || notification.getId().isBlank()) {
            notification.setId("notification-" + UUID.randomUUID());
        }
        if (notification.getCreatedAt() == null) {
            notification.setCreatedAt(LocalDateTime.now());
        }
        if (notification.getPriority() == null) {
            notification.setPriority(com.meditrack.alert.entity.Notification.NotificationPriority.MEDIUM);
        }
        if (notification.getStatus() == null) {
            notification.setStatus(com.meditrack.alert.entity.Notification.NotificationStatus.PENDING);
        }
    }

    private void validateNotification(Notification notification) {
        validateNotificationBasics(notification);
        if (notification.getNotificationType() == null) {
            throw new IllegalArgumentException("Notification type is required");
        }
    }

    private void validateNotificationBasics(Notification notification) {
        if (notification == null) {
            throw new IllegalArgumentException("Notification cannot be null");
        }
        if (notification.getRecipient() == null || notification.getRecipient().isBlank()) {
            throw new IllegalArgumentException("Notification recipient is required");
        }
        if (notification.getMessage() == null || notification.getMessage().isBlank()) {
            throw new IllegalArgumentException("Notification message is required");
        }
    }

    private Set<NotificationChannel> resolveMultiChannelTargets(Notification notification, List<NotificationChannel> channels) {
        Set<NotificationChannel> resolved = new LinkedHashSet<>();
        if (channels == null || channels.isEmpty()) {
            if (notification.getNotificationType() != null) {
                resolved.add(NotificationChannel.valueOf(notification.getNotificationType().name()));
            }
            return resolved;
        }

        for (NotificationChannel channel : channels) {
            if (channel != null) {
                resolved.add(channel);
            }
        }
        return resolved;
    }

    private Notification createChannelNotification(Notification original, NotificationChannel channel) {
        Notification channelNotification = new Notification();
        channelNotification.setId(original.getId() + "-" + channel.name().toLowerCase());
        channelNotification.setRecipient(original.getRecipient());
        channelNotification.setMessage(original.getMessage());
        channelNotification.setNotificationType(toEntityChannel(channel));
        channelNotification.setPriority(original.getPriority());
        channelNotification.setCreatedAt(LocalDateTime.now());
        channelNotification.setEscalationLevel(original.getEscalationLevel());
        channelNotification.setRecall(false);
        return channelNotification;
    }

    private String resolveRecipientType(NotificationChannel channel) {
        switch (channel) {
            case EMAIL:
                return "EMAIL";
            case SMS:
                return "PHONE";
            case PUSH:
                return "USER";
            case WEBHOOK:
            default:
                return "USER";
        }
    }

    private String resolveSubject(Notification notification) {
        if (notification.getEscalationLevel() != null && !notification.getEscalationLevel().isBlank()) {
            return "ESCALATION ALERT - " + notification.getEscalationLevel().toUpperCase();
        }

        com.meditrack.alert.entity.Notification.NotificationPriority priority = notification.getPriority();
        if (priority == null) {
            return "Notification";
        }

        switch (priority) {
            case CRITICAL:
                return "CRITICAL ALERT";
            case HIGH:
                return "HIGH PRIORITY ALERT";
            case MEDIUM:
                return "Alert Notification";
            case LOW:
                return "Information";
            default:
                return "Notification";
        }
    }

    private int resolvePriorityLevel(com.meditrack.alert.entity.Notification.NotificationPriority priority) {
        if (priority == null) {
            return 3;
        }

        switch (priority) {
            case CRITICAL:
                return 1;
            case HIGH:
                return 2;
            case MEDIUM:
                return 3;
            case LOW:
            default:
                return 4;
        }
    }

    private com.meditrack.alert.entity.Notification.NotificationType toEntityChannel(NotificationChannel channel) {
        return com.meditrack.alert.entity.Notification.NotificationType.valueOf(channel.name());
    }

    private com.meditrack.alert.entity.Notification.NotificationStatus mapToEntityStatus(NotificationDispatchResult result) {
        NotificationStatus status = result == null ? NotificationStatus.FAILED : mapToServiceStatus(result.getStatus());
        return mapToEntityStatus(status);
    }

    private com.meditrack.alert.entity.Notification.NotificationStatus mapToEntityStatus(NotificationStatus status) {
        if (status == null) {
            return com.meditrack.alert.entity.Notification.NotificationStatus.FAILED;
        }

        switch (status) {
            case PENDING:
                return com.meditrack.alert.entity.Notification.NotificationStatus.PENDING;
            case SCHEDULED:
                return com.meditrack.alert.entity.Notification.NotificationStatus.SCHEDULED;
            case SENT:
                return com.meditrack.alert.entity.Notification.NotificationStatus.SENT;
            case DELIVERED:
                return com.meditrack.alert.entity.Notification.NotificationStatus.DELIVERED;
            case READ:
                return com.meditrack.alert.entity.Notification.NotificationStatus.READ;
            case RECALLED:
                return com.meditrack.alert.entity.Notification.NotificationStatus.RECALLED;
            case FAILED:
            case NOT_FOUND:
            default:
                return com.meditrack.alert.entity.Notification.NotificationStatus.FAILED;
        }
    }

    private NotificationStatus mapToServiceStatus(com.meditrack.alert.entity.Notification.NotificationStatus status) {
        if (status == null) {
            return NotificationStatus.FAILED;
        }
        try {
            return NotificationStatus.valueOf(status.name());
        } catch (IllegalArgumentException e) {
            return NotificationStatus.FAILED;
        }
    }

    private NotificationStatus mapToServiceStatus(String status) {
        if (status == null || status.isBlank()) {
            return NotificationStatus.FAILED;
        }
        try {
            return NotificationStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return NotificationStatus.FAILED;
        }
    }

    private NotificationStatus summarizeMultiChannelStatus(List<NotificationStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return NotificationStatus.FAILED;
        }

        boolean allDelivered = statuses.stream().allMatch(status -> status == NotificationStatus.DELIVERED);
        if (allDelivered) {
            return NotificationStatus.DELIVERED;
        }

        boolean allScheduled = statuses.stream().allMatch(status -> status == NotificationStatus.SCHEDULED);
        if (allScheduled) {
            return NotificationStatus.SCHEDULED;
        }

        boolean anySuccess = statuses.stream().anyMatch(this::isSuccessful);
        if (anySuccess) {
            return NotificationStatus.SENT;
        }

        return NotificationStatus.FAILED;
    }

    private boolean isSuccessful(com.meditrack.alert.entity.Notification.NotificationStatus status) {
        return status != null
            && status != com.meditrack.alert.entity.Notification.NotificationStatus.FAILED
            && status != com.meditrack.alert.entity.Notification.NotificationStatus.PENDING;
    }

    private boolean isSuccessful(NotificationStatus status) {
        return status != null && status != NotificationStatus.FAILED && status != NotificationStatus.NOT_FOUND;
    }

    private boolean isRecallSuccessful(NotificationDispatchResult result) {
        return result != null && "RECALLED".equalsIgnoreCase(result.getStatus());
    }

    private NotificationDispatchResult findMatchingResult(List<NotificationDispatchResult> results, String channelName) {
        if (results == null || results.isEmpty()) {
            return null;
        }

        for (NotificationDispatchResult result : results) {
            if (result != null && channelName.equalsIgnoreCase(result.getChannel())) {
                return result;
            }
        }

        return results.get(0);
    }

    private List<String> extractProviderReferences(String providerReference) {
        if (providerReference == null || providerReference.isBlank()) {
            return List.of();
        }

        String trimmed = providerReference.trim();
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            return List.of(trimmed);
        }

        try {
            if (trimmed.startsWith("{")) {
                Map<String, String> references = objectMapper.readValue(
                    trimmed,
                    new TypeReference<LinkedHashMap<String, String>>() {}
                );
                return references == null ? List.of() : references.values().stream().filter(Objects::nonNull).filter(value -> !value.isBlank()).toList();
            }

            List<String> references = objectMapper.readValue(trimmed, new TypeReference<List<String>>() {});
            return references == null ? List.of() : references.stream().filter(Objects::nonNull).filter(value -> !value.isBlank()).toList();
        } catch (Exception e) {
            logger.warn("Failed to parse provider reference payload: {}", e.getMessage());
            return List.of(trimmed);
        }
    }

    private String serializeProviderReferences(Map<String, String> providerReferences) {
        if (providerReferences == null || providerReferences.isEmpty()) {
            return null;
        }

        if (providerReferences.size() == 1) {
            return providerReferences.values().iterator().next();
        }

        try {
            return objectMapper.writeValueAsString(providerReferences);
        } catch (JsonProcessingException e) {
            logger.warn("Failed to serialize provider references: {}", e.getMessage());
            return providerReferences.values().iterator().next();
        }
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return null;
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "http://localhost:8086";
        }
        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String buildUrl(String path) {
        if (path == null || path.isBlank()) {
            return notificationServiceBaseUrl;
        }
        if (path.startsWith("/")) {
            return notificationServiceBaseUrl + path;
        }
        return notificationServiceBaseUrl + "/" + path;
    }

    public static class NotificationDispatchRequest {
        private String recipientId;
        private String recipientType;
        private String notificationType;
        private List<String> channels = new ArrayList<>();
        private String subject;
        private String message;
        private String templateName;
        private Map<String, Object> templateVariables = new LinkedHashMap<>();
        private Integer priorityLevel;
        private String sourceService;
        private String sourceReference;
        private LocalDateTime scheduledAt;

        public String getRecipientId() {
            return recipientId;
        }

        public void setRecipientId(String recipientId) {
            this.recipientId = recipientId;
        }

        public String getRecipientType() {
            return recipientType;
        }

        public void setRecipientType(String recipientType) {
            this.recipientType = recipientType;
        }

        public String getNotificationType() {
            return notificationType;
        }

        public void setNotificationType(String notificationType) {
            this.notificationType = notificationType;
        }

        public List<String> getChannels() {
            return channels;
        }

        public void setChannels(List<String> channels) {
            this.channels = channels;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getTemplateName() {
            return templateName;
        }

        public void setTemplateName(String templateName) {
            this.templateName = templateName;
        }

        public Map<String, Object> getTemplateVariables() {
            return templateVariables;
        }

        public void setTemplateVariables(Map<String, Object> templateVariables) {
            this.templateVariables = templateVariables;
        }

        public Integer getPriorityLevel() {
            return priorityLevel;
        }

        public void setPriorityLevel(Integer priorityLevel) {
            this.priorityLevel = priorityLevel;
        }

        public String getSourceService() {
            return sourceService;
        }

        public void setSourceService(String sourceService) {
            this.sourceService = sourceService;
        }

        public String getSourceReference() {
            return sourceReference;
        }

        public void setSourceReference(String sourceReference) {
            this.sourceReference = sourceReference;
        }

        public LocalDateTime getScheduledAt() {
            return scheduledAt;
        }

        public void setScheduledAt(LocalDateTime scheduledAt) {
            this.scheduledAt = scheduledAt;
        }
    }

    public static class NotificationDispatchResponse {
        private String requestId;
        private String overallStatus;
        private int successCount;
        private int failureCount;
        private int scheduledCount;
        private String message;
        private List<NotificationDispatchResult> results = new ArrayList<>();

        public String getRequestId() {
            return requestId;
        }

        public void setRequestId(String requestId) {
            this.requestId = requestId;
        }

        public String getOverallStatus() {
            return overallStatus;
        }

        public void setOverallStatus(String overallStatus) {
            this.overallStatus = overallStatus;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public void setSuccessCount(int successCount) {
            this.successCount = successCount;
        }

        public int getFailureCount() {
            return failureCount;
        }

        public void setFailureCount(int failureCount) {
            this.failureCount = failureCount;
        }

        public int getScheduledCount() {
            return scheduledCount;
        }

        public void setScheduledCount(int scheduledCount) {
            this.scheduledCount = scheduledCount;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public List<NotificationDispatchResult> getResults() {
            return results;
        }

        public void setResults(List<NotificationDispatchResult> results) {
            this.results = results;
        }
    }

    public static class NotificationDispatchResult {
        private Long notificationId;
        private String channel;
        private String status;
        private String provider;
        private String responseCode;
        private String responseMessage;
        private String failureReason;
        private LocalDateTime sentAt;
        private String templateUsed;

        public static NotificationDispatchResult failed(String channel, String failureReason) {
            NotificationDispatchResult result = new NotificationDispatchResult();
            result.setChannel(channel);
            result.setStatus("FAILED");
            result.setFailureReason(failureReason);
            result.setResponseMessage(failureReason);
            return result;
        }

        public Long getNotificationId() {
            return notificationId;
        }

        public void setNotificationId(Long notificationId) {
            this.notificationId = notificationId;
        }

        public String getChannel() {
            return channel;
        }

        public void setChannel(String channel) {
            this.channel = channel;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getResponseCode() {
            return responseCode;
        }

        public void setResponseCode(String responseCode) {
            this.responseCode = responseCode;
        }

        public String getResponseMessage() {
            return responseMessage;
        }

        public void setResponseMessage(String responseMessage) {
            this.responseMessage = responseMessage;
        }

        public String getFailureReason() {
            return failureReason;
        }

        public void setFailureReason(String failureReason) {
            this.failureReason = failureReason;
        }

        public LocalDateTime getSentAt() {
            return sentAt;
        }

        public void setSentAt(LocalDateTime sentAt) {
            this.sentAt = sentAt;
        }

        public String getTemplateUsed() {
            return templateUsed;
        }

        public void setTemplateUsed(String templateUsed) {
            this.templateUsed = templateUsed;
        }
    }

    public enum NotificationChannel {
        EMAIL,
        SMS,
        PUSH,
        WEBHOOK
    }

    public enum NotificationStatus {
        PENDING,
        SCHEDULED,
        SENT,
        DELIVERED,
        FAILED,
        READ,
        RECALLED,
        NOT_FOUND
    }

    public enum NotificationPriority {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public static class NotificationContext {
        private Notification notification;
        private LocalDateTime createdAt;
        private LocalDateTime sentAt;
        private NotificationStatus status;

        public NotificationContext(Notification notification, LocalDateTime createdAt) {
            this.notification = notification;
            this.createdAt = createdAt;
            this.status = NotificationStatus.PENDING;
        }

        public Notification getNotification() {
            return notification;
        }

        public void setNotification(Notification notification) {
            this.notification = notification;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public LocalDateTime getSentAt() {
            return sentAt;
        }

        public void setSentAt(LocalDateTime sentAt) {
            this.sentAt = sentAt;
        }

        public NotificationStatus getStatus() {
            return status;
        }

        public void setStatus(NotificationStatus status) {
            this.status = status;
        }
    }
}
