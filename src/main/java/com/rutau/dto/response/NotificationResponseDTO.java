package com.rutau.dto.response;

import com.rutau.model.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponseDTO(
        Long id,
        NotificationType type,
        String message,
        Boolean read,
        LocalDateTime createdAt
) {}
