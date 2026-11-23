package com.rutau.agent.dto;

import java.util.List;

// US22 - Recomendación de horario para publicar.
// status: ZONE (datos de la zona pedida), GENERAL (no hubo datos en la zona: se usa toda la universidad)
// o NOT_ENOUGH_DATA (no hay historial suficiente).
public record ScheduleRecommendation(
        String status,
        String zone,
        String mostRequestedHour,
        List<String> opportunityHours,
        String message) {}