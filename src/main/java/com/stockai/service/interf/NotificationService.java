package com.stockai.service.interf;

import com.stockai.dto.NotificationDto;

import java.util.List;
import java.util.UUID;

public interface NotificationService {

    List<NotificationDto> getAllNotifications();

    List<NotificationDto> getUnreadNotifications();

    NotificationDto createNotification(NotificationDto notificationDto);

    NotificationDto markAsRead(UUID id);
}

