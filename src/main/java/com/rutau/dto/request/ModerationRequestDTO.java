package com.rutau.dto.request;

import com.rutau.model.ModerationAction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// US19 - Acción del administrador sobre un reporte
public record ModerationRequestDTO(

        @NotNull(message = "Debes indicar la acción: WARNING, SUSPENSION o DISMISS")
        ModerationAction action,

        @Size(max = 200, message = "La nota no puede superar los 200 caracteres")
        String notes
) {}
