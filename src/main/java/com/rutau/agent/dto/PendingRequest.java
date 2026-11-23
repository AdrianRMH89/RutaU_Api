package com.rutau.agent.dto;

// Una solicitud pendiente en uno de mis viajes (lo que el agente necesita para identificarla)
public record PendingRequest(
        Long requestId,
        String passengerName,
        String route,
        String departure,
        String pickupPoint) {}