package com.meditrack.notification.repository;

import com.meditrack.notification.entity.NotificationDeliveryLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationDeliveryLogRepository extends JpaRepository<NotificationDeliveryLogEntity, Long> {

    List<NotificationDeliveryLogEntity> findByNotificationIdOrderByDeliveryAttemptAsc(Long notificationId);
}
