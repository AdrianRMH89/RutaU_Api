package com.rutau.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record TripRequestDTO(

        @NotBlank(message = "{validation.origin.required}")
        @Size(max = 200)
        String origin,

        @NotBlank(message = "{validation.destination.required}")
        @Size(max = 200)
        String destination,

        @NotBlank(message = "{validation.zone.required}")
        @Size(max = 100)
        String zone,

        // US02 - Escenario de error
        @NotNull(message = "{validation.departure.invalid}")
        @Future(message = "{validation.departure.invalid}")
        LocalDateTime departureTime,

        @NotNull(message = "{validation.seats.required}")
        @Min(value = 0, message = "{validation.seats.negative}")
        Integer seats,

        @NotNull(message = "{validation.price.required}")
        @DecimalMin(value = "0.0", message = "{validation.price.negative}")
        BigDecimal pricePerSeat,

        // US11 - Opcional: puntos intermedios de recojo o bajada (en el orden de la ruta)
        @Size(max = 5, message = "{validation.stops.max}")
        List<@Valid TripStopRequestDTO> stops
) {}