package com.rutau.controller;

import com.rutau.dto.response.NotificationResponseDTO;
import com.rutau.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // Mis notificaciones (US08: avisos de cancelación)
    @GetMapping("/me")
    public List<NotificationResponseDTO> myNotifications() {
        return notificationService.getMyNotifications();
    }
}
