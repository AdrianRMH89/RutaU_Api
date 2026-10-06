package com.rutau.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// US19 - Un usuario reporta a otro usuario, un viaje, o ambos
public record ReportCreateDTO(

        Long reportedUserId,     // opcional si se indica el viaje (se reporta a su conductor)

        Long tripId,             // opcional si se indica el usuario

        @NotBlank(message = "Debes indicar el motivo del reporte")
        @Size(max = 500, message = "El motivo no puede superar los 500 caracteres")
        String reason
) {}
