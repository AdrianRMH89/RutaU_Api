package com.rutau.dto.response;

import java.util.List;

// US19 - Historial de reportes de un usuario en el panel de administración
public record UserModerationResponseDTO(
        Long userId,
        String fullName,
        String email,
        String accountStatus,       // "Activa", "En observación" o "Suspendida"
        Long totalReports,
        Long validReports,          // reportes atendidos con advertencia o suspensión
        List<ReportResponseDTO> reports
) {}
