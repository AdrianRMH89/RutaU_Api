package com.rutau.dto.response;

import java.util.List;

// US16 - "Mis calificaciones": lo que otros usuarios opinan de mí
public record RatingHistoryResponseDTO(
        String message,
        String roleFilter,          // "Todos los roles", "DRIVER" o "PASSENGER"
        Long total,
        Double average,             // null si no hay calificaciones
        List<RatingResponseDTO> ratings
) {}
