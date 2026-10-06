package com.rutau.dto.response;

// US10 - Perfil público del conductor (sin datos privados como correo o teléfono)
public record PublicProfileResponseDTO(
        Long id,
        String fullName,
        String university,
        String avatarUrl,
        Double averageRating,     // null cuando aún no tiene calificaciones
        String ratingLabel,       // "4.5 / 5" o "Aún sin calificaciones"
        Long totalRatings,        // US10 - Escenario alternativo: da contexto al promedio
        Long completedTrips
) {}
