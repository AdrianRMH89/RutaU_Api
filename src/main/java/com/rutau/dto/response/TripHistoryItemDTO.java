package com.rutau.dto.response;

import java.time.LocalDateTime;

// US15 - Un viaje dentro de "Mi historial"
public record TripHistoryItemDTO(
        Long tripId,
        String role,              // DRIVER (conductor) o PASSENGER (pasajero)
        LocalDateTime departureTime,
        String origin,
        String destination,
        String status,            // conductor: estado del viaje / pasajero: estado de su solicitud
        String otherParty         // conductor: cantidad de pasajeros / pasajero: nombre del conductor
) {}
