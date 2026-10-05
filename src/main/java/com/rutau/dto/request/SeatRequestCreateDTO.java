package com.rutau.dto.request;

import jakarta.validation.constraints.NotNull;

public record SeatRequestCreateDTO(

        @NotNull(message = "Debes indicar el viaje")
        Long tripId,

        // US03 - Escenario alternativo: el pasajero confirma aunque haya cruce de horarios
        Boolean confirmOverlap,

        // US11 - Opcional: punto intermedio donde el pasajero sube o baja.
        // Si no se envía, el recojo es en el origen del conductor.
        Long pickupStopId
) {}