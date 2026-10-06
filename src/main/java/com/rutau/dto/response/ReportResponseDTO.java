package com.rutau.dto.response;

import com.rutau.model.ReportStatus;

import java.time.LocalDateTime;

// US19 - Detalle de un reporte
public record ReportResponseDTO(
        Long id,
        Long reporterId,
        String reporterName,
        Long reportedUserId,
        String reportedUserName,
        Long reportedTripId,
        String reportedTripRoute,
        String reason,
        ReportStatus status,
        String actionTaken,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt,
        String reportedUserStatus    // "Activa", "En observación" o "Suspendida"
) {}
