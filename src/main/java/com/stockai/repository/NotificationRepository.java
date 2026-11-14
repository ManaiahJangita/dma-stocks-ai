package com.stockai.repository;

import com.stockai.entity.NotificationEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository {
    List<NotificationEntity> findAllNotifications();

    List<NotificationEntity> findUnreadNotifications();

    NotificationEntity saveNotification(NotificationEntity notification);

    NotificationEntity markNotificationRead(UUID id);
}

