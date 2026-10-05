package com.rutau.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TripRequestDTO(

        @NotBlank(message = "El origen es obligatorio")
        @Size(max = 200)
        String origin,

        @NotBlank(message = "El destino es obligatorio")
        @Size(max = 200)
        String destination,

        @NotBlank(message = "La zona es obligatoria")
        @Size(max = 100)
        String zone,

        // US02 - Escenario de error
        @NotNull(message = "Debes indicar un horario válido")
        @Future(message = "Debes indicar un horario válido")
        LocalDateTime departureTime,

        @NotNull(message = "Debes indicar el número de asientos")
        @Min(value = 0, message = "El número de asientos no puede ser negativo")
        Integer seats,

        @NotNull(message = "Debes indicar el precio por asiento")
        @DecimalMin(value = "0.0", message = "El precio no puede ser negativo")
        BigDecimal pricePerSeat
) {}