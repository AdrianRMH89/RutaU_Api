package com.rutau.dto.request;

import jakarta.validation.constraints.NotNull;

public record SeatRequestCreateDTO(

        @NotNull(message = "Debes indicar el viaje")
        Long tripId,

        // US03 - Escenario alternativo: el pasajero confirma aunque haya cruce de horarios
        Boolean confirmOverlap
) {}