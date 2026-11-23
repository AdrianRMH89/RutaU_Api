package com.rutau.agent.dto;

import java.util.List;

// US21 - Resultado de searchTrips.
// status: FOUND (hay viajes), NEARBY (solo horarios cercanos), NONE (nada), MISSING_DESTINATION (falta el destino)
// o ERROR (filtros inválidos; message explica por qué).
public record TripSearchResult(
        String status,
        String message,
        List<TripSummary> results,
        List<TripSummary> nearby) {}