package com.rutau.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// US11 - Punto intermedio de recojo o bajada
public record TripStopRequestDTO(

        @NotBlank(message = "{validation.stop.address.required}")
        @Size(max = 200)
        String address,

        @NotBlank(message = "{validation.stop.zone.required}")
        @Size(max = 100)
        String zone
) {}
