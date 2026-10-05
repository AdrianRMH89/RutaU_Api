package com.rutau.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelRequestDTO(

        @NotBlank(message = "Debes indicar el motivo de la cancelación")
        @Size(max = 255, message = "El motivo no puede superar los 255 caracteres")
        String reason
) {}
