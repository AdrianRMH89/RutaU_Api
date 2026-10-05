package com.rutau.service;

import com.rutau.dto.response.TripResponseDTO;
import com.rutau.dto.response.TripSearchResponseDTO;
import com.rutau.exception.BusinessRuleException;
import com.rutau.mapper.TripMapper;
import com.rutau.model.Trip;
import com.rutau.model.TripStatus;
import com.rutau.repository.TripRepository;
import com.rutau.util.LimaZones;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TripSearchService {

    // US13 - Un viaje "cercano" sale como máximo 15 minutos antes o después del rango buscado
    private static final int NEAR_MINUTES = 15;

    private final TripRepository tripRepository;
    private final TripMapper tripMapper;

    // ==================== US13 - Buscar viajes por origen, destino y horario ====================

    // Todos los parámetros son opcionales. from y to son horas del día (ej. 07:00 y 08:00).
    @Transactional(readOnly = true)
    public TripSearchResponseDTO search(String origin, String destination, LocalTime from, LocalTime to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessRuleException("La hora de inicio debe ser anterior a la hora de fin");
        }

        // Solo se buscan viajes activos: con asientos libres y que aún no salen
        List<Trip> active = tripRepository.findByStatusAndDepartureTimeAfterOrderByDepartureTimeAsc(
                TripStatus.SCHEDULED, LocalDateTime.now());

        // Primero se filtra por ruta (origen y destino)
        List<Trip> sameRoute = active.stream()
                .filter(t -> contains(t.getOrigin(), origin) && contains(t.getDestination(), destination))
                .toList();

        // US13 - Escenario exitoso: ruta + rango de horario
        List<TripResponseDTO> results = sameRoute.stream()
                .filter(t -> inRange(t.getDepartureTime().toLocalTime(), from, to))
                .map(tripMapper::toResponse)
                .toList();

        if (!results.isEmpty()) {
            return new TripSearchResponseDTO("Se encontraron " + results.size() + " viaje(s)",
                    results.size(), results, List.of());
        }

        // US13 - Escenario alternativo: viajes de la misma ruta con un horario cercano
        List<TripResponseDTO> alternatives = List.of();
        if (from != null || to != null) {
            LocalTime nearFrom = from == null ? null : from.minusMinutes(NEAR_MINUTES);
            LocalTime nearTo = to == null ? null : to.plusMinutes(NEAR_MINUTES);
            alternatives = sameRoute.stream()
                    .filter(t -> inRange(t.getDepartureTime().toLocalTime(), nearFrom, nearTo))
                    .map(tripMapper::toResponse)
                    .toList();
        }

        if (!alternatives.isEmpty()) {
            return new TripSearchResponseDTO("No hay viajes exactamente en ese horario, "
                    + "pero encontramos opciones con un horario cercano (±" + NEAR_MINUTES + " minutos)",
                    0, List.of(), alternatives);
        }

        // US13 - Escenario de error
        return new TripSearchResponseDTO("No se encontraron viajes disponibles para esta búsqueda",
                0, List.of(), List.of());
    }

    // ==================== Métodos de apoyo ====================

    // Coincidencia parcial, sin importar mayúsculas ni tildes ("monterrico" encuentra "UPC Campus Monterrico")
    private boolean contains(String value, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        return LimaZones.normalize(value).contains(LimaZones.normalize(search));
    }

    private boolean inRange(LocalTime time, LocalTime from, LocalTime to) {
        boolean afterFrom = from == null || !time.isBefore(from);
        boolean beforeTo = to == null || !time.isAfter(to);
        return afterFrom && beforeTo;
    }
}
