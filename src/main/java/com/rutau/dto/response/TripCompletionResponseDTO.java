package com.rutau.dto.response;

import com.rutau.model.TripStatus;

import java.util.List;

public record TripCompletionResponseDTO(
        Long tripId,
        TripStatus status,
        String message,
        List<SeatRequestResponseDTO> passengers
) {}
