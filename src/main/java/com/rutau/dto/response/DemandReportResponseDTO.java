package com.rutau.dto.response;

import java.time.LocalDate;
import java.util.List;

// US17 - Panel de reportes: rutas y horarios con mayor demanda
public record DemandReportResponseDTO(
        String message,
        LocalDate from,
        LocalDate to,
        String groupedBy,               // DAY (día por día) o WEEK (por semana, si el rango es amplio)
        Long totalRequests,
        List<ReportCountDTO> topRoutes, // de mayor a menor demanda
        List<ReportCountDTO> topHours,  // de mayor a menor demanda
        List<ReportCountDTO> timeline   // solicitudes por día o por semana
) {}
