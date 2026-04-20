package com.meditrack.alert.saga;

import com.meditrack.alert.service.NotificationService;
import com.meditrack.alert.entity.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class NotificationSagaStep extends SagaStep {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationSagaStep.class);
    
    private final NotificationService notificationService;
    private final Notification notification;
    
    public NotificationSagaStep(NotificationService notificationService, Notification notification) {
        super("notification-" + notification.getId(), "Notification Sending");
        this.notificationService = notificationService;
        this.notification = notification;
        this.addContextValue("notificationId", notification.getId());
        this.addContextValue("notificationType", notification.getNotificationType());
        this.addContextValue("recipient", notification.getRecipient());
    }
    
    @Override
    public CompletableFuture<StepResult> execute() {
        try {
            logger.info("Executing notification step for notification: {}", notification.getId());
            
            // Send the notification
            boolean success = notificationService.sendNotification(notification);
            
            if (success) {
                addContextValue("sentAt", java.time.LocalDateTime.now());
                return CompletableFuture.completedFuture(
                    new StepResult(true, "Notification sent successfully")
                );
            } else {
                return CompletableFuture.completedFuture(
                    new StepResult(false, "Failed to send notification")
                );
            }
            
        } catch (Exception e) {
            logger.error("Error executing notification step for notification: {}", notification.getId(), e);
            return CompletableFuture.completedFuture(
                new StepResult(false, "Error sending notification: " + e.getMessage())
            );
        }
    }
    
    @Override
    public CompletableFuture<StepResult> compensate() {
        try {
            logger.info("Compensating notification step for notification: {}", notification.getId());
            
            // Undo notification (mark as recalled)
            boolean success = notificationService.recallNotification(notification);
            
            if (success) {
                return CompletableFuture.completedFuture(
                    new StepResult(true, "Notification compensation successful")
                );
            } else {
                return CompletableFuture.completedFuture(
                    new StepResult(false, "Failed to compensate notification")
                );
            }
            
        } catch (Exception e) {
            logger.error("Error compensating notification step for notification: {}", notification.getId(), e);
            return CompletableFuture.completedFuture(
                new StepResult(false, "Error compensating notification: " + e.getMessage())
            );
        }
    }
}
