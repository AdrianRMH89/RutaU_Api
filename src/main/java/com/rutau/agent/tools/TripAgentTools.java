package com.rutau.agent.tools;

import com.rutau.agent.dto.TripSearchResult;
import com.rutau.agent.dto.TripSummary;
import com.rutau.dto.response.TripResponseDTO;
import com.rutau.dto.response.TripSearchResponseDTO;
import com.rutau.exception.BusinessRuleException;
import com.rutau.security.CurrentUser;
import com.rutau.service.TripSearchService;
import com.rutau.util.Messages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

// US21 - El pasajero busca viajes escribiendo en lenguaje natural.
// La tool reutiliza TripSearchService (US13/US14): no toca la base de datos por su cuenta.
@Slf4j
@Component
@RequiredArgsConstructor
public class TripAgentTools {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int MAX_RESULTS = 5;   // nunca devolver listas enormes al modelo

    private final TripSearchService tripSearchService;
    private final CurrentUser currentUser;
    private final Messages messages;

    @Tool(description = """
            Busca viajes disponibles por origen, destino, fecha, rango de hora, zona y precio máximo.
            Úsala siempre que el usuario pregunte por viajes para ir a algún lugar.
            Necesita el destino. Si no hay viajes exactos devuelve horarios cercanos en nearby.
            """)
    public TripSearchResult searchTrips(
            @ToolParam(description = "Lugar de salida, palabra clave corta (ej. 'San Borja'). Solo si el usuario lo menciona",
                    required = false) String origin,
            @ToolParam(description = "Destino, palabra clave corta (ej. 'UPC')", required = false) String destination,
            @ToolParam(description = "Fecha del viaje en formato AAAA-MM-DD, solo si el usuario la indica",
                    required = false) LocalDate date,
            @ToolParam(description = "Hora mínima de salida HH:mm (ej. 'en la mañana' = 06:00)",
                    required = false) LocalTime fromTime,
            @ToolParam(description = "Hora máxima de salida HH:mm (ej. 'en la mañana' = 12:00)",
                    required = false) LocalTime toTime,
            @ToolParam(description = "Distrito o zona de salida, solo si el usuario la menciona",
                    required = false) String zone,
            @ToolParam(description = "Precio máximo por asiento en soles, solo si el usuario lo menciona",
                    required = false) BigDecimal maxPrice) {

        // Diagnóstico: confirma en la consola que el modelo llamó a la tool
        log.info(">>> TOOL searchTrips llamada: origen={}, destino={}, fecha={}", origin, destination, date);

        // US21 - Escenario de error: sin destino no se busca; el agente debe preguntarlo
        if (isBlank(destination)) {
            return new TripSearchResult("MISSING_DESTINATION", "Falta el destino del viaje", List.of(), List.of());
        }

        TripSearchResponseDTO response;
        try {
            response = tripSearchService.search(origin, destination, fromTime, toTime, zone, maxPrice);
        } catch (BusinessRuleException ex) {
            // Las excepciones traen una clave de i18n: se traduce para que el modelo la explique
            return new TripSearchResult("ERROR", messages.get(ex.getMessage(), ex.getArgs()), List.of(), List.of());
        }

        Long me = currentUser.get().getId();
        List<TripSummary> results = filter(response.results().stream(), date, me);
        List<TripSummary> nearby = filter(response.alternatives().stream(), date, me);

        // US21 - Escenario alternativo: ese día no hay nada; se sugiere el viaje más próximo de la misma ruta
        if (results.isEmpty() && nearby.isEmpty() && (date != null || fromTime != null || toTime != null)) {
            TripSearchResponseDTO anyDay = tripSearchService.search(origin, destination, null, null, zone, maxPrice);
            nearby = filter(Stream.concat(anyDay.results().stream(), anyDay.alternatives().stream()), null, me);
        }

        String status = !results.isEmpty() ? "FOUND" : !nearby.isEmpty() ? "NEARBY" : "NONE";
        return new TripSearchResult(status, response.message(), results, nearby);
    }

    // Quita mis propios viajes, filtra por fecha si se pidió y deja los 5 más próximos
    private List<TripSummary> filter(Stream<TripResponseDTO> trips, LocalDate date, Long me) {
        return trips
                .filter(t -> !t.driverId().equals(me))
                .filter(t -> date == null || t.departureTime().toLocalDate().equals(date))
                .sorted(Comparator.comparing(TripResponseDTO::departureTime))
                .limit(MAX_RESULTS)
                .map(this::toSummary)
                .toList();
    }

    private TripSummary toSummary(TripResponseDTO t) {
        return new TripSummary(t.id(), t.driverName(), t.origin(), t.destination(), t.zone(),
                t.departureTime().format(FMT), t.availableSeats(), t.pricePerSeat());
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}