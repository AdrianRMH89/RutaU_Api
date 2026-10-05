package com.rutau.dto.response;

import com.rutau.model.RequestStatus;

import java.time.LocalDateTime;

public record SeatRequestResponseDTO(
        Long id,
        Long tripId,
        String origin,
        String destination,
        LocalDateTime departureTime,
        Long passengerId,
        String passengerName,
        RequestStatus status,
        LocalDateTime requestedAt
) {}