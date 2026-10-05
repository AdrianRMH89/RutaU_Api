package com.rutau.dto.response;

public record VehicleResponseDTO(
        Long id,
        String brand,
        String model,
        String color,
        String plate,
        Integer capacity
) {}