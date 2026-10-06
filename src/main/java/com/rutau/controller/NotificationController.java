package com.rutau.controller;

import com.rutau.dto.response.NotificationResponseDTO;
import com.rutau.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // Mis notificaciones (US08: cancelaciones, US20: solicitud aceptada o rechazada)
    @GetMapping("/me")
    public List<NotificationResponseDTO> myNotifications() {
        return notificationService.getMyNotifications();
    }

    // US20 - Cantidad de notificaciones sin leer: { "unread": 3 }
    @GetMapping("/me/unread-count")
    public Map<String, Long> unreadCount() {
        return Map.of("unread", notificationService.countUnread());
    }

    // US20 - Marcar una notificación como leída
    @PatchMapping("/{id}/read")
    public NotificationResponseDTO markAsRead(@PathVariable Long id) {
        return notificationService.markAsRead(id);
    }
}
