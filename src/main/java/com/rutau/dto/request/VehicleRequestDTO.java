package com.rutau.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VehicleRequestDTO(

        @NotBlank(message = "La marca es obligatoria")
        @Size(max = 50)
        String brand,

        @NotBlank(message = "El modelo es obligatorio")
        @Size(max = 50)
        String model,

        @Size(max = 30)
        String color,

        @NotBlank(message = "La placa es obligatoria")
        @Size(max = 10)
        String plate,

        @NotNull(message = "La capacidad es obligatoria")
        @Min(value = 1, message = "La capacidad mínima es 1 pasajero")
        @Max(value = 8, message = "La capacidad máxima es 8 pasajeros")
        Integer capacity
) {}