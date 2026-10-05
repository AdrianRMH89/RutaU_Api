package com.rutau.dto.response;

import java.util.List;

// US12 - Datos para el gráfico de horarios ofertados vs. solicitados
public record ScheduleComparisonResponseDTO(
        Boolean enoughData,
        String message,
        String zone,
        Long totalTrips,
        Long totalRequests,
        String mostOfferedHour,         // franja con más asientos ofrecidos
        String mostRequestedHour,       // franja con más solicitudes
        List<String> opportunityHours,  // franjas resaltadas como oportunidad
        List<HourSlotDTO> slots         // una fila por franja (para dibujar el gráfico)
) {}
