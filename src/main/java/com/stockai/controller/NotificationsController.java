package com.stockai.controller;

import com.stockai.dto.NotificationDto;
import com.stockai.service.interf.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1", "/api/v1/notifications", "/api/v1/notification"})
public class NotificationsController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping({"/notifications", "/notification"})
    public ResponseEntity<List<NotificationDto>> getNotifications() {
        List<NotificationDto> notifications = notificationService.getAllNotifications();
        if (notifications == null || notifications.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(notifications);
    }

    @GetMapping({"/notifications/unread", "/notification/unread"})
    public ResponseEntity<List<NotificationDto>> getUnreadNotifications() {
        List<NotificationDto> notifications = notificationService.getUnreadNotifications();
        if (notifications == null || notifications.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(notifications);
    }

    @PostMapping({"/notifications", "/notification"})
    public ResponseEntity<?> createNotification(@RequestBody NotificationDto notificationDto) {
        try {
            NotificationDto created = notificationService.createNotification(notificationDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping({"/notifications/{id}/read", "/notification/{id}/read"})
    public ResponseEntity<?> markNotificationRead(@PathVariable UUID id) {
        try {
            NotificationDto updated = notificationService.markAsRead(id);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
