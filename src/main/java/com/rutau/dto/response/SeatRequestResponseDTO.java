package com.rutau.dto.response;

import com.rutau.model.RequestStatus;

import java.time.LocalDateTime;

public record SeatRequestResponseDTO(
        Long id,
        Long tripId,
        String origin,
        String destination,
        LocalDateTime departureTime,
        Integer tripAvailableSeats,
        Long passengerId,
        String passengerName,
        Long pickupStopId,          // US11 - null si el recojo es en el origen
        String pickupPoint,         // US11 - dirección del punto de recojo/bajada
        RequestStatus status,
        LocalDateTime requestedAt
) {}