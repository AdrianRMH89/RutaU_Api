package com.rutau.dto.request;

import com.rutau.model.ModerationAction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// US19 - Acción del administrador sobre un reporte
public record ModerationRequestDTO(

        @NotNull(message = "{validation.moderation.action.required}")
        ModerationAction action,

        @Size(max = 200, message = "{validation.notes.max}")
        String notes
) {}
