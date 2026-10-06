package com.rutau.controller;

import com.rutau.dto.response.NotificationResponseDTO;
import com.rutau.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "09. Notificaciones", description = "Avisos sobre solicitudes y viajes (US08, US20)")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // Mis notificaciones (US08: cancelaciones, US20: solicitud aceptada o rechazada)
    @Operation(summary = "Ver mis notificaciones")
    @GetMapping("/me")
    public List<NotificationResponseDTO> myNotifications() {
        return notificationService.getMyNotifications();
    }

    // US20 - Cantidad de notificaciones sin leer: { "unread": 3 }
    @Operation(summary = "Cantidad de notificaciones sin leer (US20)")
    @GetMapping("/me/unread-count")
    public Map<String, Long> unreadCount() {
        return Map.of("unread", notificationService.countUnread());
    }

    // US20 - Marcar una notificación como leída
    @Operation(summary = "Marcar una notificación como leída (US20)")
    @PatchMapping("/{id}/read")
    public NotificationResponseDTO markAsRead(@PathVariable Long id) {
        return notificationService.markAsRead(id);
    }
}
