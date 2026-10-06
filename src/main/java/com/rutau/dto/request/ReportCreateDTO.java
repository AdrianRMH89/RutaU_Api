package com.rutau.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// US19 - Un usuario reporta a otro usuario, un viaje, o ambos
public record ReportCreateDTO(

        Long reportedUserId,     // opcional si se indica el viaje (se reporta a su conductor)

        Long tripId,             // opcional si se indica el usuario

        @NotBlank(message = "{validation.report.reason.required}")
        @Size(max = 500, message = "{validation.report.reason.max}")
        String reason
) {}
