package com.rutau.dto.response;

import com.rutau.model.TripStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TripResponseDTO(
        Long id,
        Long driverId,
        String driverName,
        String vehicleBrand,
        String vehicleModel,
        String vehiclePlate,
        String origin,
        String destination,
        String zone,
        LocalDateTime departureTime,
        Integer totalSeats,
        Integer availableSeats,
        BigDecimal pricePerSeat,
        TripStatus status
) {}