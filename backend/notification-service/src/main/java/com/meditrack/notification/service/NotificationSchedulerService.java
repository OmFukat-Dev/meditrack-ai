package com.meditrack.notification.service;

import com.meditrack.notification.domain.NotificationStatus;
import com.meditrack.notification.entity.NotificationEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class NotificationSchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationSchedulerService.class);

    private final NotificationDispatchService dispatchService;

    public NotificationSchedulerService(NotificationDispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    @Scheduled(fixedDelayString = "${meditrack.notification.retry.delay-seconds:60}000")
    public void processDueNotifications() {
        List<NotificationEntity> scheduled = dispatchService.findDueScheduledNotifications();
        List<NotificationEntity> retries = dispatchService.findDueRetryNotifications();

        Set<Long> processedIds = new LinkedHashSet<>();
        int processedCount = 0;

        for (NotificationEntity notification : scheduled) {
            if (processedIds.add(notification.getId())) {
                dispatchService.dispatchNotification(notification);
                processedCount++;
            }
        }

        for (NotificationEntity notification : retries) {
            if (processedIds.add(notification.getId())) {
                dispatchService.dispatchNotification(notification);
                processedCount++;
            }
        }

        if (processedCount > 0) {
            logger.info("Processed {} due notification records", processedCount);
        }
    }
}
