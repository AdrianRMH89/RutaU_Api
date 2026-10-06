package com.rutau.dto.response;

import java.util.List;

// US13 / US14 - Resultado de una búsqueda de viajes
public record TripSearchResponseDTO(
        String message,
        Integer totalResults,
        List<TripResponseDTO> results,        // viajes que cumplen todos los criterios
        List<TripResponseDTO> alternatives    // US13 - Escenario alternativo: horarios cercanos (±15 min)
) {}
