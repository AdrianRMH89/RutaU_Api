package com.rutau.service;

import com.rutau.dto.response.NotificationResponseDTO;
import com.rutau.exception.ResourceNotFoundException;
import com.rutau.mapper.NotificationMapper;
import com.rutau.model.Notification;
import com.rutau.model.NotificationType;
import com.rutau.model.User;
import com.rutau.repository.NotificationRepository;
import com.rutau.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
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

    // US20 - Marcar una notificación como leída (solo su dueño)
    @Transactional
    public NotificationResponseDTO markAsRead(Long id) {
        User user = currentUser.get();
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notificación no encontrada"));
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Solo puedes marcar tus propias notificaciones");
        }
        notification.setRead(true);
        return notificationMapper.toResponse(notificationRepository.save(notification));
    }

    // US20 - Cantidad de notificaciones sin leer (para el contador de la app)
    @Transactional(readOnly = true)
    public long countUnread() {
        return notificationRepository.countByUserIdAndReadFalse(currentUser.get().getId());
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
