package com.meditrack.notification.repository;

import com.meditrack.notification.domain.NotificationStatus;
import com.meditrack.notification.entity.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    Page<NotificationEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<NotificationEntity> findByRecipientIdOrderByCreatedAtDesc(String recipientId, Pageable pageable);

    List<NotificationEntity> findByDeliveryStatusAndScheduledAtLessThanEqual(
        NotificationStatus deliveryStatus,
        LocalDateTime scheduledAt
    );

    List<NotificationEntity> findByDeliveryStatusAndNextRetryAtLessThanEqual(
        NotificationStatus deliveryStatus,
        LocalDateTime nextRetryAt
    );
}
