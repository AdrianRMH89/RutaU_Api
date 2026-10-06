package com.rutau.service;

import com.rutau.dto.response.DemandReportResponseDTO;
import com.rutau.dto.response.ReportCountDTO;
import com.rutau.exception.BusinessRuleException;
import com.rutau.model.SeatRequest;
import com.rutau.model.Trip;
import com.rutau.repository.SeatRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminReportService {

    // US17 - Escenario alternativo: si el rango supera este número de días se agrupa por semana
    private static final long MAX_DAYS_DAILY = 31;
    private static final int TOP_LIMIT = 10;
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final SeatRequestRepository seatRequestRepository;

    // ==================== US17 - Reporte de rutas y horarios con mayor demanda ====================

    // from y to son opcionales: por defecto, desde hace 30 días hasta dentro de 30 días
    @Transactional(readOnly = true)
    public DemandReportResponseDTO demandReport(LocalDate from, LocalDate to) {
        LocalDate start = from != null ? from : LocalDate.now().minusDays(30);
        LocalDate end = to != null ? to : LocalDate.now().plusDays(30);
        if (start.isAfter(end)) {
            throw new BusinessRuleException("La fecha de inicio debe ser anterior a la fecha de fin");
        }

        // La demanda son las solicitudes de asiento de los viajes que salen en ese período
        List<SeatRequest> requests = seatRequestRepository.findByTripDepartureTimeBetween(
                start.atStartOfDay(), end.plusDays(1).atStartOfDay().minusNanos(1));

        boolean byWeek = ChronoUnit.DAYS.between(start, end) + 1 > MAX_DAYS_DAILY;
        String groupedBy = byWeek ? "WEEK" : "DAY";

        // US17 - Escenario de error: el período no tiene datos
        if (requests.isEmpty()) {
            return new DemandReportResponseDTO("No hay datos disponibles para el período seleccionado",
                    start, end, groupedBy, 0L, List.of(), List.of(), List.of());
        }

        // US17 - Escenario exitoso: rutas y horarios ordenados de mayor a menor demanda
        List<ReportCountDTO> topRoutes = rank(requests,
                r -> r.getTrip().getOrigin() + " → " + r.getTrip().getDestination());
        List<ReportCountDTO> topHours = rank(requests, r -> hourLabel(r.getTrip()));

        // Línea de tiempo: día por día, o por semana si el rango es muy amplio (escenario alternativo)
        Map<LocalDate, Long> perPeriod = new TreeMap<>(requests.stream().collect(Collectors.groupingBy(
                r -> periodStart(r.getTrip().getDepartureTime().toLocalDate(), byWeek),
                Collectors.counting())));
        List<ReportCountDTO> timeline = perPeriod.entrySet().stream()
                .map(e -> new ReportCountDTO(periodLabel(e.getKey(), byWeek), e.getValue()))
                .toList();

        String message = byWeek
                ? "Reporte generado (agrupado por semana porque el rango supera los " + MAX_DAYS_DAILY + " días)"
                : "Reporte generado";

        return new DemandReportResponseDTO(message, start, end, groupedBy, (long) requests.size(),
                topRoutes, topHours, timeline);
    }

    // ==================== Métodos de apoyo ====================

    // Cuenta por la clave indicada y ordena de mayor a menor (máximo 10 filas)
    private List<ReportCountDTO> rank(List<SeatRequest> requests, Function<SeatRequest, String> key) {
        Map<String, Long> counts = requests.stream()
                .collect(Collectors.groupingBy(key, LinkedHashMap::new, Collectors.counting()));
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(TOP_LIMIT)
                .map(e -> new ReportCountDTO(e.getKey(), e.getValue()))
                .toList();
    }

    private String hourLabel(Trip trip) {
        int hour = trip.getDepartureTime().getHour();
        return String.format("%02d:00 - %02d:59", hour, hour);
    }

    private LocalDate periodStart(LocalDate date, boolean byWeek) {
        return byWeek ? date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) : date;
    }

    private String periodLabel(LocalDate start, boolean byWeek) {
        return byWeek
                ? "Semana del " + start.format(DAY) + " al " + start.plusDays(6).format(DAY)
                : start.format(DAY);
    }
}
