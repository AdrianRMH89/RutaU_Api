package com.rutau.agent.tools;

import com.rutau.agent.dto.RouteSummary;
import com.rutau.agent.dto.ScheduleRecommendation;
import com.rutau.dto.response.DemandReportResponseDTO;
import com.rutau.dto.response.ReportCountDTO;
import com.rutau.dto.response.ScheduleComparisonResponseDTO;
import com.rutau.service.AdminReportService;
import com.rutau.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

// US22 y US23 - Tools de solo lectura sobre la demanda. Reutilizan StatsService (US12) y AdminReportService (US17).
@Component
@RequiredArgsConstructor
public class DemandAgentTools {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    private final StatsService statsService;
    private final AdminReportService adminReportService;

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

    @Tool(description = """
            Resume las rutas y los horarios más solicitados de una semana.
            Sin fecha usa los últimos 7 días. Con weekStart usa los 7 días desde esa fecha.
            """)
    public RouteSummary weeklyRouteSummary(
            @ToolParam(description = "Primer día de la semana en formato AAAA-MM-DD, solo si el usuario pide una semana concreta",
                    required = false) LocalDate weekStart) {

        // US23 - Escenario alternativo: semana específica del pasado
        LocalDate from = weekStart != null ? weekStart : LocalDate.now(LIMA).minusDays(6);
        LocalDate to = from.plusDays(6);

        DemandReportResponseDTO report = adminReportService.demandReport(from, to);

        // US23 - Escenario de error: no hay datos en ese período
        if (report.totalRequests() == null || report.totalRequests() == 0) {
            return new RouteSummary("NO_DATA", from, to, 0L, List.of(), List.of());
        }
        return new RouteSummary("DATA", from, to, report.totalRequests(),
                top(report.topRoutes(), 5), top(report.topHours(), 3));
    }

    private ScheduleRecommendation toRecommendation(String status, ScheduleComparisonResponseDTO s) {
        return new ScheduleRecommendation(status, s.zone(), s.mostRequestedHour(), s.opportunityHours(), s.message());
    }

    private List<ReportCountDTO> top(List<ReportCountDTO> list, int n) {
        return list == null ? List.of() : list.stream().limit(n).toList();
    }
}