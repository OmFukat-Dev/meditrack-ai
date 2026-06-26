package com.meditrack.notification.repository;

import com.meditrack.notification.domain.NotificationChannel;
import com.meditrack.notification.domain.NotificationType;
import com.meditrack.notification.entity.NotificationTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplateEntity, Long> {

    Optional<NotificationTemplateEntity> findByTemplateName(String templateName);

    Optional<NotificationTemplateEntity> findByTemplateNameAndActiveTrue(String templateName);

    List<NotificationTemplateEntity> findByTemplateTypeAndChannelTypeAndActiveTrue(
        NotificationType templateType,
        NotificationChannel channelType
    );

    List<NotificationTemplateEntity> findAllByOrderByTemplateNameAsc();
}
