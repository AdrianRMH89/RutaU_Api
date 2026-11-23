package com.rutau.agent.tools;

import com.rutau.agent.dto.ScheduleRecommendation;
import com.rutau.dto.response.ScheduleComparisonResponseDTO;
import com.rutau.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

// US22 - Tool de solo lectura sobre la demanda. Reutiliza StatsService (US12).
@Component
@RequiredArgsConstructor
public class DemandAgentTools {

    private final StatsService statsService;

    @Tool(description = """
            Recomienda los mejores horarios para publicar un viaje según la demanda registrada.
            Úsala cuando un conductor pida una recomendación de horario. La zona es opcional.
            Si la zona no tiene datos suficientes, devuelve la recomendación general (status GENERAL).
            """)
    public ScheduleRecommendation recommendSchedule(
            @ToolParam(description = "Distrito o zona del conductor, solo si la menciona", required = false)
            String zone) {

        ScheduleComparisonResponseDTO byZone = statsService.compareSchedules(zone);
        if (Boolean.TRUE.equals(byZone.enoughData())) {
            return toRecommendation("ZONE", byZone);   // US22 - Escenario exitoso
        }

        // US22 - Escenario alternativo: sin patrón en esa zona, se usa el horario general de la universidad
        if (zone != null && !zone.isBlank()) {
            ScheduleComparisonResponseDTO general = statsService.compareSchedules(null);
            if (Boolean.TRUE.equals(general.enoughData())) {
                return toRecommendation("GENERAL", general);
            }
        }

        // US22 - Escenario de error: no hay datos suficientes
        return new ScheduleRecommendation("NOT_ENOUGH_DATA", zone, null, List.of(), byZone.message());
    }

    private ScheduleRecommendation toRecommendation(String status, ScheduleComparisonResponseDTO s) {
        return new ScheduleRecommendation(status, s.zone(), s.mostRequestedHour(), s.opportunityHours(), s.message());
    }
}
