package com.rutau.repository;

import com.rutau.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    // US20 - Notificaciones sin leer
    long countByUserIdAndReadFalse(Long userId);
}