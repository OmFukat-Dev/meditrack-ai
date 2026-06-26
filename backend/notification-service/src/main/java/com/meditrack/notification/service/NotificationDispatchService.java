package com.meditrack.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meditrack.notification.config.NotificationProperties;
import com.meditrack.notification.domain.NotificationChannel;
import com.meditrack.notification.domain.NotificationStatus;
import com.meditrack.notification.domain.NotificationType;
import com.meditrack.notification.dto.NotificationChannelResult;
import com.meditrack.notification.dto.NotificationRequest;
import com.meditrack.notification.dto.NotificationResponse;
import com.meditrack.notification.entity.NotificationDeliveryLogEntity;
import com.meditrack.notification.entity.NotificationEntity;
import com.meditrack.notification.entity.NotificationTemplateEntity;
import com.meditrack.notification.repository.NotificationDeliveryLogRepository;
import com.meditrack.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class NotificationDispatchService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationDispatchService.class);

    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryLogRepository deliveryLogRepository;
    private final NotificationTemplateService templateService;
    private final ObjectMapper objectMapper;
    private final NotificationProperties properties;
    private final Map<NotificationChannel, NotificationChannelSender> channelSenders = new EnumMap<>(NotificationChannel.class);

    public NotificationDispatchService(
        NotificationRepository notificationRepository,
        NotificationDeliveryLogRepository deliveryLogRepository,
        NotificationTemplateService templateService,
        ObjectMapper objectMapper,
        NotificationProperties properties,
        List<NotificationChannelSender> senders
    ) {
        this.notificationRepository = notificationRepository;
        this.deliveryLogRepository = deliveryLogRepository;
        this.templateService = templateService;
        this.objectMapper = objectMapper;
        this.properties = properties;

        for (NotificationChannelSender sender : senders) {
            channelSenders.put(sender.getChannel(), sender);
        }
    }

    public NotificationResponse sendNotification(NotificationRequest request) {
        validateRequest(request);

        List<NotificationChannel> channels = resolveChannels(request);
        List<NotificationChannelResult> results = new ArrayList<>();

        for (NotificationChannel channel : channels) {
            NotificationEntity notification = null;
            try {
                notification = createNotification(request, channel);
                notification = notificationRepository.save(notification);

                Map<String, Object> context = buildContextVariables(request, notification);
                NotificationTemplateEntity template = resolveTemplate(request.getTemplateName(), channel).orElse(null);
                applyResolvedContent(notification, request, template, context, channel);
                notification = notificationRepository.save(notification);

                NotificationChannelResult result;
                if (notification.getScheduledAt() != null && notification.getScheduledAt().isAfter(LocalDateTime.now())) {
                    notification.setDeliveryStatus(NotificationStatus.SCHEDULED);
                    notification.setNextRetryAt(notification.getScheduledAt());
                    notificationRepository.save(notification);
                    result = mapScheduledResult(notification, channel);
                } else {
                    result = dispatchNotification(notification);
                }

                results.add(result);
            } catch (Exception e) {
                logger.error("Failed to process notification for channel {}: {}", channel, e.getMessage(), e);
                if (notification != null) {
                    notification.setDeliveryStatus(NotificationStatus.FAILED);
                    notification.setFailureReason(e.getMessage());
                    notificationRepository.save(notification);

                    int attempt = (notification.getRetryCount() == null ? 0 : notification.getRetryCount()) + 1;
                    ChannelDeliveryResult processingFailure = ChannelDeliveryResult.failure(
                        channel,
                        "SYSTEM",
                        "500",
                        "Notification processing failed",
                        e.getMessage()
                    );
                    saveDeliveryLog(notification, processingFailure, attempt);
                }

                NotificationChannelResult failedResult = new NotificationChannelResult();
                failedResult.setNotificationId(notification == null ? null : notification.getId());
                failedResult.setChannel(channel);
                failedResult.setStatus(NotificationStatus.FAILED);
                failedResult.setProvider("SYSTEM");
                failedResult.setResponseCode("500");
                failedResult.setResponseMessage("Notification processing failed");
                failedResult.setFailureReason(e.getMessage());
                results.add(failedResult);
            }
        }

        NotificationResponse response = new NotificationResponse();
        response.setRequestId(UUID.randomUUID().toString());
        response.setResults(results);
        populateSummary(response, results);
        return response;
    }

    public NotificationChannelResult dispatchNotification(NotificationEntity notification) {
        NotificationChannelSender sender = channelSenders.get(notification.getChannelType());
        if (sender == null) {
            throw new IllegalArgumentException("No sender available for channel " + notification.getChannelType());
        }

        String subject = notification.getSubject();
        String body = notification.getMessage();
        int deliveryAttempt = (notification.getRetryCount() == null ? 0 : notification.getRetryCount()) + 1;

        ChannelDeliveryResult deliveryResult;
        try {
            deliveryResult = sender.send(notification, subject, body);
        } catch (Exception e) {
            deliveryResult = ChannelDeliveryResult.failure(
                notification.getChannelType(),
                sender.getClass().getSimpleName(),
                "500",
                "Unexpected delivery error",
                e.getMessage()
            );
        }

        applyDeliveryResult(notification, deliveryResult);
        saveDeliveryLog(notification, deliveryResult, deliveryAttempt);

        NotificationChannelResult result = new NotificationChannelResult();
        result.setNotificationId(notification.getId());
        result.setChannel(notification.getChannelType());
        result.setStatus(notification.getDeliveryStatus());
        result.setProvider(deliveryResult.getProvider());
        result.setResponseCode(deliveryResult.getResponseCode());
        result.setResponseMessage(deliveryResult.getResponseMessage());
        result.setFailureReason(notification.getFailureReason());
        result.setSentAt(notification.getSentAt());
        result.setTemplateUsed(notification.getTemplateUsed());
        return result;
    }

    public NotificationChannelResult recallNotification(Long notificationId) {
        NotificationEntity notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));

        NotificationChannelSender sender = channelSenders.get(notification.getChannelType());
        if (sender == null) {
            throw new IllegalArgumentException("No sender available for channel " + notification.getChannelType());
        }

        ChannelDeliveryResult deliveryResult = sender.recall(notification);
        notification.setDeliveryStatus(deliveryResult.isSuccess() ? NotificationStatus.RECALLED : NotificationStatus.FAILED);
        notification.setFailureReason(deliveryResult.isSuccess() ? null : deliveryResult.getErrorMessage());
        notification.setUpdatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
        saveDeliveryLog(notification, deliveryResult, notification.getRetryCount() == null ? 1 : notification.getRetryCount() + 1);

        NotificationChannelResult result = new NotificationChannelResult();
        result.setNotificationId(notification.getId());
        result.setChannel(notification.getChannelType());
        result.setStatus(notification.getDeliveryStatus());
        result.setProvider(deliveryResult.getProvider());
        result.setResponseCode(deliveryResult.getResponseCode());
        result.setResponseMessage(deliveryResult.getResponseMessage());
        result.setFailureReason(notification.getFailureReason());
        result.setSentAt(notification.getSentAt());
        result.setTemplateUsed(notification.getTemplateUsed());
        return result;
    }

    public NotificationEntity getNotification(Long notificationId) {
        return notificationRepository.findById(notificationId)
            .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));
    }

    public List<NotificationEntity> getRecentNotifications(int limit) {
        int pageSize = Math.max(1, Math.min(limit, 100));
        return notificationRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, pageSize, Sort.by("createdAt").descending()))
            .getContent();
    }

    public List<NotificationEntity> getNotificationsByRecipient(String recipientId, int limit) {
        int pageSize = Math.max(1, Math.min(limit, 100));
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(
            recipientId,
            PageRequest.of(0, pageSize, Sort.by("createdAt").descending())
        ).getContent();
    }

    public List<NotificationEntity> findDueScheduledNotifications() {
        return notificationRepository.findByDeliveryStatusAndScheduledAtLessThanEqual(
            NotificationStatus.SCHEDULED,
            LocalDateTime.now()
        );
    }

    public List<NotificationEntity> findDueRetryNotifications() {
        return notificationRepository.findByDeliveryStatusAndNextRetryAtLessThanEqual(
            NotificationStatus.FAILED,
            LocalDateTime.now()
        );
    }

    private Optional<NotificationTemplateEntity> resolveTemplate(String templateName, NotificationChannel channel) {
        if (templateName == null || templateName.isBlank()) {
            return Optional.empty();
        }

        Optional<NotificationTemplateEntity> template = templateService.getTemplate(templateName, channel);
        if (template.isPresent()) {
            return template;
        }

        return templateService.getTemplate(templateName);
    }

    private void validateRequest(NotificationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Notification request cannot be null");
        }
        if (request.getRecipientId() == null || request.getRecipientId().isBlank()) {
            throw new IllegalArgumentException("Recipient ID is required");
        }
        if (request.getRecipientType() == null) {
            throw new IllegalArgumentException("Recipient type is required");
        }
        if (request.getNotificationType() == null) {
            throw new IllegalArgumentException("Notification type is required");
        }
        if (request.getSourceService() == null || request.getSourceService().isBlank()) {
            throw new IllegalArgumentException("Source service is required");
        }
        if ((request.getMessage() == null || request.getMessage().isBlank()) && (request.getTemplateName() == null || request.getTemplateName().isBlank())) {
            throw new IllegalArgumentException("Either a message or template name must be provided");
        }
    }

    private List<NotificationChannel> resolveChannels(NotificationRequest request) {
        Set<NotificationChannel> resolved = new LinkedHashSet<>();
        if (request.getChannels() == null || request.getChannels().isEmpty()) {
            resolved.addAll(defaultChannels(request.getNotificationType()));
        } else {
            for (NotificationChannel channel : request.getChannels()) {
                if (!channelSenders.containsKey(channel)) {
                    throw new IllegalArgumentException("Unsupported notification channel: " + channel);
                }
                resolved.add(channel);
            }
        }

        if (resolved.isEmpty()) {
            throw new IllegalArgumentException("No supported notification channels resolved");
        }
        return new ArrayList<>(resolved);
    }

    private List<NotificationChannel> defaultChannels(NotificationType type) {
        switch (type) {
            case ALERT:
            case ESCALATION:
                return List.of(NotificationChannel.EMAIL, NotificationChannel.SMS, NotificationChannel.PUSH);
            case REMINDER:
                return List.of(NotificationChannel.EMAIL, NotificationChannel.PUSH);
            case REPORT:
            case SYSTEM:
            default:
                return List.of(NotificationChannel.EMAIL);
        }
    }

    private NotificationEntity createNotification(NotificationRequest request, NotificationChannel channel) {
        NotificationEntity notification = new NotificationEntity();
        notification.setRecipientId(request.getRecipientId());
        notification.setRecipientType(request.getRecipientType());
        notification.setNotificationType(request.getNotificationType());
        notification.setChannelType(channel);
        notification.setPriorityLevel(request.getPriorityLevel() == null ? 3 : request.getPriorityLevel());
        notification.setDeliveryStatus(NotificationStatus.PENDING);
        notification.setScheduledAt(request.getScheduledAt());
        notification.setRetryCount(0);
        notification.setMaxRetries(properties.getRetry().getMaxAttempts());
        notification.setSourceService(request.getSourceService());
        notification.setSourceReference(request.getSourceReference());
        notification.setTemplateUsed(request.getTemplateName());
        notification.setMessage(request.getMessage() == null ? "" : request.getMessage());
        notification.setSubject(request.getSubject());
        return notification;
    }

    private void applyResolvedContent(
        NotificationEntity notification,
        NotificationRequest request,
        NotificationTemplateEntity template,
        Map<String, Object> context,
        NotificationChannel channel
    ) {
        Map<String, Object> mergedContext = new LinkedHashMap<>();
        if (template != null) {
            mergedContext.putAll(templateService.extractTemplateVariables(template));
        }
        if (request.getTemplateVariables() != null) {
            mergedContext.putAll(request.getTemplateVariables());
        }
        mergedContext.putAll(context);

        String subject = resolveSubject(request, template, mergedContext, channel);
        String body = resolveBody(request, template, mergedContext);

        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("Notification body cannot be empty");
        }

        notification.setSubject(subject);
        notification.setMessage(body);
        notification.setMessageData(serialize(mergedContext));
        if (template != null) {
            notification.setTemplateUsed(template.getTemplateName());
        }
    }

    private String resolveSubject(
        NotificationRequest request,
        NotificationTemplateEntity template,
        Map<String, Object> context,
        NotificationChannel channel
    ) {
        if (template != null && template.getSubjectTemplate() != null && !template.getSubjectTemplate().isBlank()) {
            return templateService.renderTemplate(template.getSubjectTemplate(), context);
        }
        if (request.getSubject() != null && !request.getSubject().isBlank()) {
            return templateService.renderTemplate(request.getSubject(), context);
        }
        return defaultSubject(request.getNotificationType(), channel);
    }

    private String resolveBody(NotificationRequest request, NotificationTemplateEntity template, Map<String, Object> context) {
        if (template != null && template.getBodyTemplate() != null && !template.getBodyTemplate().isBlank()) {
            return templateService.renderTemplate(template.getBodyTemplate(), context);
        }
        return templateService.renderTemplate(request.getMessage(), context);
    }

    private String defaultSubject(NotificationType type, NotificationChannel channel) {
        switch (type) {
            case ALERT:
                return "Alert Notification";
            case ESCALATION:
                return "Escalation Notification";
            case REMINDER:
                return "Reminder Notification";
            case REPORT:
                return "Report Notification";
            case SYSTEM:
            default:
                return channel.name() + " Notification";
        }
    }

    private NotificationChannelResult mapScheduledResult(NotificationEntity notification, NotificationChannel channel) {
        NotificationChannelResult result = new NotificationChannelResult();
        result.setNotificationId(notification.getId());
        result.setChannel(channel);
        result.setStatus(NotificationStatus.SCHEDULED);
        result.setProvider("SCHEDULER");
        result.setResponseCode("202");
        result.setResponseMessage("Notification scheduled for " + notification.getScheduledAt());
        result.setTemplateUsed(notification.getTemplateUsed());
        result.setSentAt(notification.getSentAt());
        return result;
    }

    private void applyDeliveryResult(NotificationEntity notification, ChannelDeliveryResult deliveryResult) {
        LocalDateTime now = LocalDateTime.now();
        if (deliveryResult.isSuccess()) {
            notification.setDeliveryStatus(NotificationStatus.DELIVERED);
            notification.setSentAt(now);
            notification.setDeliveredAt(now);
            notification.setFailureReason(null);
            notification.setNextRetryAt(null);
        } else {
            int retryCount = notification.getRetryCount() == null ? 0 : notification.getRetryCount();
            retryCount++;
            notification.setRetryCount(retryCount);
            notification.setDeliveryStatus(NotificationStatus.FAILED);
            notification.setFailureReason(deliveryResult.getErrorMessage());
            if (retryCount < notification.getMaxRetries()) {
                notification.setNextRetryAt(now.plusSeconds(properties.getRetry().getDelaySeconds()));
            } else {
                notification.setNextRetryAt(null);
            }
        }
        notificationRepository.save(notification);
    }

    private void saveDeliveryLog(NotificationEntity notification, ChannelDeliveryResult deliveryResult, int deliveryAttempt) {
        NotificationDeliveryLogEntity log = new NotificationDeliveryLogEntity();
        log.setNotificationId(notification.getId());
        log.setDeliveryAttempt(deliveryAttempt);
        log.setChannelType(notification.getChannelType());
        log.setProvider(deliveryResult.getProvider());
        log.setProviderResponseCode(deliveryResult.getResponseCode());
        log.setProviderResponseMessage(deliveryResult.getResponseMessage());
        log.setDeliveryStatus(deliveryResult.isSuccess() ? "DELIVERED" : "FAILED");
        log.setErrorDetails(deliveryResult.getErrorMessage());
        log.setMetadataJson(serialize(deliveryResult.getMetadata()));
        deliveryLogRepository.save(log);
    }

    private Map<String, Object> buildContextVariables(NotificationRequest request, NotificationEntity notification) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("notificationId", notification.getId());
        variables.put("recipientId", request.getRecipientId());
        variables.put("recipientType", request.getRecipientType().name());
        variables.put("notificationType", request.getNotificationType().name());
        variables.put("channelType", notification.getChannelType().name());
        variables.put("priorityLevel", notification.getPriorityLevel());
        variables.put("sourceService", request.getSourceService());
        variables.put("sourceReference", request.getSourceReference());
        variables.put("scheduledAt", request.getScheduledAt());
        return variables;
    }

    private String serialize(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value == null ? new LinkedHashMap<>() : value);
        } catch (JsonProcessingException e) {
            logger.warn("Failed to serialize notification payload: {}", e.getMessage());
            return "{}";
        }
    }

    private void populateSummary(NotificationResponse response, List<NotificationChannelResult> results) {
        int success = 0;
        int failure = 0;
        int scheduled = 0;

        for (NotificationChannelResult result : results) {
            if (result.getStatus() == NotificationStatus.SCHEDULED) {
                scheduled++;
            } else if (result.getStatus() == NotificationStatus.DELIVERED || result.getStatus() == NotificationStatus.SENT) {
                success++;
            } else if (result.getStatus() == NotificationStatus.FAILED) {
                failure++;
            }
        }

        response.setSuccessCount(success);
        response.setFailureCount(failure);
        response.setScheduledCount(scheduled);

        if (scheduled > 0 && success == 0 && failure == 0) {
            response.setOverallStatus("SCHEDULED");
            response.setMessage("Notification scheduled");
            return;
        }

        if (failure == 0) {
            response.setOverallStatus("COMPLETED");
            response.setMessage("Notification delivered successfully");
            return;
        }

        if (success > 0) {
            response.setOverallStatus("PARTIAL_SUCCESS");
            response.setMessage("Notification delivered to some channels and failed on others");
            return;
        }

        response.setOverallStatus("FAILED");
        response.setMessage("Notification delivery failed");
    }
}
