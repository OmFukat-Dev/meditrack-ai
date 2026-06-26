package com.meditrack.notification.service;

import com.meditrack.notification.domain.NotificationChannel;
import com.meditrack.notification.domain.NotificationStatus;
import com.meditrack.notification.domain.NotificationType;
import com.meditrack.notification.domain.RecipientType;
import com.meditrack.notification.entity.NotificationEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationSchedulerServiceTest {

    @Test
    void processDueNotificationsDispatchesEachNotificationOnce() {
        NotificationDispatchService dispatchService = mock(NotificationDispatchService.class);
        NotificationSchedulerService schedulerService = new NotificationSchedulerService(dispatchService);

        NotificationEntity scheduled = notification(1L, NotificationStatus.SCHEDULED);
        NotificationEntity retry = notification(2L, NotificationStatus.FAILED);

        when(dispatchService.findDueScheduledNotifications()).thenReturn(List.of(scheduled, retry));
        when(dispatchService.findDueRetryNotifications()).thenReturn(List.of(retry));

        schedulerService.processDueNotifications();

        verify(dispatchService).findDueScheduledNotifications();
        verify(dispatchService).findDueRetryNotifications();
        verify(dispatchService, times(1)).dispatchNotification(scheduled);
        verify(dispatchService, times(1)).dispatchNotification(retry);
    }

    private NotificationEntity notification(Long id, NotificationStatus status) {
        NotificationEntity notification = new NotificationEntity();
        notification.setId(id);
        notification.setRecipientId("patient-1");
        notification.setRecipientType(RecipientType.EMAIL);
        notification.setNotificationType(NotificationType.ALERT);
        notification.setChannelType(NotificationChannel.EMAIL);
        notification.setSubject("Critical alert");
        notification.setMessage("Vitals are critical");
        notification.setPriorityLevel(1);
        notification.setSourceService("alert-service");
        notification.setDeliveryStatus(status);
        notification.setScheduledAt(LocalDateTime.now());
        return notification;
    }
}
