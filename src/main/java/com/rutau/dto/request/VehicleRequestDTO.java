package com.rutau.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VehicleRequestDTO(

        @NotBlank(message = "{validation.brand.required}")
        @Size(max = 50)
        String brand,

        @NotBlank(message = "{validation.model.required}")
        @Size(max = 50)
        String model,

        @Size(max = 30)
        String color,

        @NotBlank(message = "{validation.plate.required}")
        @Size(max = 10)
        String plate,

        @NotNull(message = "{validation.capacity.required}")
        @Min(value = 1, message = "{validation.capacity.min}")
        @Max(value = 8, message = "{validation.capacity.max}")
        Integer capacity
) {}