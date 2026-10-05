package com.rutau.dto.response;

public record TripStopResponseDTO(
        Long id,
        String address,
        String zone,
        Integer stopOrder
) {}
