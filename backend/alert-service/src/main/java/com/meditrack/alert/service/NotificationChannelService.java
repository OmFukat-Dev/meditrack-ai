package com.meditrack.alert.service;

import com.meditrack.alert.entity.Notification;

public interface NotificationChannelService {
    
    boolean sendNotification(Notification notification);
    
    boolean recallNotification(Notification notification);
}
