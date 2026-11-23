package com.rutau.agent.dto;

import java.math.BigDecimal;

// Lo que el agente "ve" de un viaje: solo los datos útiles para el pasajero (menos tokens, menos ruido).
// departure va como texto "AAAA-MM-DD HH:mm" para que el modelo no se equivoque con el formato.
public record TripSummary(
        Long id,
        String driverName,
        String origin,
        String destination,
        String zone,
        String departure,
        Integer availableSeats,
        BigDecimal pricePerSeat) {}