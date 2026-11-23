package com.rutau.agent.dto;

import com.rutau.dto.response.ReportCountDTO;

import java.time.LocalDate;
import java.util.List;

// US23 - Rutas y horarios más pedidos de una semana. status: DATA o NO_DATA.
public record RouteSummary(
        String status,
        LocalDate from,
        LocalDate to,
        Long totalRequests,
        List<ReportCountDTO> topRoutes,
        List<ReportCountDTO> topHours) {}