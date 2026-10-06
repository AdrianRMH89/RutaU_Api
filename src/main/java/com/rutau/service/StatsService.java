package com.rutau.service;

import com.rutau.dto.response.HourSlotDTO;
import com.rutau.dto.response.ScheduleComparisonResponseDTO;
import com.rutau.repository.SeatRequestRepository;
import com.rutau.repository.TripRepository;
import com.rutau.util.Messages;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class StatsService {

    // Mínimo de historial para que la comparación tenga sentido
    private static final long MIN_TRIPS = 5;
    private static final long MIN_REQUESTS = 5;

    // Una franja es "oportunidad" si tiene al menos 2 solicitudes y más solicitudes que asientos
    private static final long MIN_REQUESTS_FOR_OPPORTUNITY = 2;

    private final TripRepository tripRepository;
    private final SeatRequestRepository seatRequestRepository;
    private final Messages messages;

    // ==================== US12 - Comparar horarios ofertados y solicitados ====================

    @Transactional(readOnly = true)
    // zone es opcional: null o vacío = todas las zonas
    public ScheduleComparisonResponseDTO compareSchedules(String zone) {
        String zoneFilter = zone == null ? "" : zone.trim();

        // hora (0-23) -> {viajes, asientos, solicitudes}. TreeMap: queda ordenado por hora
        Map<Integer, long[]> byHour = new TreeMap<>();

        for (Object[] row : tripRepository.countOfferByHour(zoneFilter)) {
            long[] values = byHour.computeIfAbsent(toInt(row[0]), h -> new long[3]);
            values[0] = toLong(row[1]);
            values[1] = toLong(row[2]);
        }
        for (Object[] row : seatRequestRepository.countDemandByHour(zoneFilter)) {
            long[] values = byHour.computeIfAbsent(toInt(row[0]), h -> new long[3]);
            values[2] = toLong(row[1]);
        }

        long totalTrips = byHour.values().stream().mapToLong(v -> v[0]).sum();
        long totalRequests = byHour.values().stream().mapToLong(v -> v[2]).sum();

        // US12 - Escenario de error: no hay suficiente historial
        if (totalTrips < MIN_TRIPS || totalRequests < MIN_REQUESTS) {
            return new ScheduleComparisonResponseDTO(false,
                    messages.get("stats.not.enough"),
                    zoneLabel(zoneFilter), totalTrips, totalRequests, null, null, List.of(), List.of());
        }

        // US12 - Escenario exitoso: una fila por franja horaria
        List<HourSlotDTO> slots = new ArrayList<>();
        for (Map.Entry<Integer, long[]> entry : byHour.entrySet()) {
            long[] v = entry.getValue();
            // US12 - Escenario alternativo: alta demanda pero baja oferta
            boolean opportunity = v[2] >= MIN_REQUESTS_FOR_OPPORTUNITY && v[2] > v[1];
            slots.add(new HourSlotDTO(entry.getKey(), label(entry.getKey()), v[0], v[1], v[2], opportunity));
        }

        String mostOffered = slots.stream()
                .max(Comparator.comparing(HourSlotDTO::seatsOffered))
                .map(HourSlotDTO::label).orElse(null);
        String mostRequested = slots.stream()
                .max(Comparator.comparing(HourSlotDTO::seatRequests))
                .map(HourSlotDTO::label).orElse(null);
        List<String> opportunities = slots.stream()
                .filter(HourSlotDTO::opportunity)
                .map(HourSlotDTO::label)
                .toList();

        String message = opportunities.isEmpty()
                ? messages.get("stats.ok")
                : messages.get("stats.opportunities", String.join(", ", opportunities));

        return new ScheduleComparisonResponseDTO(true, message, zoneLabel(zoneFilter), totalTrips, totalRequests,
                mostOffered, mostRequested, opportunities, slots);
    }

    private String zoneLabel(String zoneFilter) {
        return zoneFilter.isEmpty() ? messages.get("stats.all.zones") : zoneFilter;
    }

    private static String label(int hour) {
        return String.format("%02d:00 - %02d:59", hour, hour);
    }

    private static int toInt(Object value) {
        return ((Number) value).intValue();
    }

    private static long toLong(Object value) {
        return value == null ? 0 : ((Number) value).longValue();
    }
}
