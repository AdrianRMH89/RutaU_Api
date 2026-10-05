package com.rutau.service;

import com.rutau.dto.response.NotificationResponseDTO;
import com.rutau.mapper.NotificationMapper;
import com.rutau.model.Notification;
import com.rutau.model.NotificationType;
import com.rutau.model.User;
import com.rutau.repository.NotificationRepository;
import com.rutau.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final int MAX_MESSAGE_LENGTH = 255;   // notifications.message es VARCHAR(255)

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final CurrentUser currentUser;

    // Registra una notificación para un usuario (la usan los demás servicios)
    @Transactional
    public void notify(User user, NotificationType type, String message) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setMessage(message.length() > MAX_MESSAGE_LENGTH
                ? message.substring(0, MAX_MESSAGE_LENGTH - 3) + "..."
                : message);
        notificationRepository.save(notification);
    }

    // Notificaciones del usuario logueado, de la más reciente a la más antigua
    @Transactional(readOnly = true)
    public List<NotificationResponseDTO> getMyNotifications() {
        User user = currentUser.get();
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(notificationMapper::toResponse)
                .toList();
    }
}
