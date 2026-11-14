package com.stockai.service.impl;

import com.stockai.dto.NotificationDto;
import com.stockai.entity.NotificationEntity;
import com.stockai.repository.NotificationRepository;
import com.stockai.service.interf.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Override
    public List<NotificationDto> getAllNotifications() {
        log.info("Fetching all notifications");
        List<NotificationEntity> entities = notificationRepository.findAllNotifications();
        List<NotificationDto> response = new ArrayList<>();
        for (NotificationEntity entity : entities) {
            response.add(toDto(entity));
        }
        log.info("Retrieved {} notifications", response.size());
        return response;
    }

    @Override
    public List<NotificationDto> getUnreadNotifications() {
        log.info("Fetching unread notifications");
        List<NotificationEntity> entities = notificationRepository.findUnreadNotifications();
        List<NotificationDto> response = new ArrayList<>();
        for (NotificationEntity entity : entities) {
            response.add(toDto(entity));
        }
        log.info("Retrieved {} unread notifications", response.size());
        return response;
    }

    @Override
    public NotificationDto createNotification(NotificationDto dto) {
        log.info("Creating notification with title: {}", dto == null ? null : dto.getTitle());
        if (dto == null) {
            throw new IllegalArgumentException("Notification payload is required");
        }
        if (dto.getTitle() == null || dto.getTitle().isBlank()) {
            throw new IllegalArgumentException("Notification title is required");
        }
        if (dto.getMessage() == null || dto.getMessage().isBlank()) {
            throw new IllegalArgumentException("Notification message is required");
        }

        NotificationEntity entity = new NotificationEntity();
        BeanUtils.copyProperties(dto, entity);
        entity.setRead(dto.getRead() != null ? dto.getRead() : false);
        entity.setPriority(dto.getPriority() == null ? "INFO" : dto.getPriority());
        entity.setType(dto.getType() == null ? "GENERAL" : dto.getType());
        entity.setCreatedAt(LocalDateTime.now());
        if (Boolean.TRUE.equals(entity.getRead())) {
            entity.setReadAt(LocalDateTime.now());
        }

        NotificationDto saved = toDto(notificationRepository.saveNotification(entity));
        log.info("Notification created successfully with id: {} and title: {}", saved.getId(), saved.getTitle());
        return saved;
    }

    @Override
    public NotificationDto markAsRead(UUID id) {
        log.info("Marking notification as read. Notification id: {}", id);
        if (id == null) {
            throw new IllegalArgumentException("Notification id is required");
        }
        NotificationDto updated = toDto(notificationRepository.markNotificationRead(id));
        log.info("Notification {} marked as read successfully", id);
        return updated;
    }

    private NotificationDto toDto(NotificationEntity entity) {
        NotificationDto dto = new NotificationDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }
}
