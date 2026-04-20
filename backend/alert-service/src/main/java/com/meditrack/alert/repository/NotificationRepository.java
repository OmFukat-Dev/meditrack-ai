package com.meditrack.alert.repository;

import com.meditrack.alert.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {
    
    @Query("SELECT n FROM Notification n WHERE n.recipient = :recipient ORDER BY n.createdAt DESC")
    List<Notification> findByRecipientOrderByCreatedAtDesc(@Param("recipient") String recipient);
    
    @Query("SELECT n FROM Notification n WHERE n.notificationType = :notificationType ORDER BY n.createdAt DESC")
    List<Notification> findByNotificationTypeOrderByCreatedAtDesc(@Param("notificationType") Notification.NotificationType notificationType);
    
    @Query("SELECT n FROM Notification n WHERE n.priority = :priority ORDER BY n.createdAt DESC")
    List<Notification> findByPriorityOrderByCreatedAtDesc(@Param("priority") Notification.NotificationPriority priority);
    
    @Query("SELECT n FROM Notification n WHERE n.status = :status ORDER BY n.createdAt DESC")
    List<Notification> findByStatusOrderByCreatedAtDesc(@Param("status") Notification.NotificationStatus status);
    
    @Query("SELECT n FROM Notification n WHERE n.createdAt BETWEEN :startTime AND :endTime ORDER BY n.createdAt DESC")
    List<Notification> findByCreatedAtBetweenOrderByCreatedAtDesc(
        @Param("startTime") LocalDateTime startTime, 
        @Param("endTime") LocalDateTime endTime);
    
    @Query("SELECT n FROM Notification n WHERE n.recall = true ORDER BY n.recallAt DESC")
    List<Notification> findRecalledNotificationsOrderByRecallAtDesc();
    
    @Query("SELECT n FROM Notification n WHERE n.escalationLevel IS NOT NULL ORDER BY n.createdAt DESC")
    List<Notification> findEscalationNotificationsOrderByCreatedAtDesc();
    
    @Query("SELECT n FROM Notification n WHERE n.status = 'PENDING' ORDER BY n.createdAt ASC")
    List<Notification> findPendingNotificationsOrderByCreatedAtAsc();
    
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.createdAt >= :startTime")
    long countNotificationsSince(@Param("startTime") LocalDateTime startTime);
    
    @Query("SELECT n FROM Notification n ORDER BY n.createdAt DESC")
    List<Notification> findTop100ByOrderByCreatedAtDesc();
    
    @Query("SELECT n FROM Notification n WHERE n.recipient = :recipient AND n.status = 'SENT' ORDER BY n.sentAt DESC")
    List<Notification> findSentNotificationsByRecipientOrderBySentAtDesc(@Param("recipient") String recipient);
    
    @Query("SELECT n FROM Notification n WHERE n.recipient = :recipient AND n.status = 'FAILED' ORDER BY n.createdAt DESC")
    List<Notification> findFailedNotificationsByRecipientOrderByCreatedAtDesc(@Param("recipient") String recipient);
}
