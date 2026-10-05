package com.rutau.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// US11 - Punto intermedio de recojo o bajada
public record TripStopRequestDTO(

        @NotBlank(message = "La dirección del punto intermedio es obligatoria")
        @Size(max = 200)
        String address,

        @NotBlank(message = "El distrito del punto intermedio es obligatorio")
        @Size(max = 100)
        String zone
) {}
