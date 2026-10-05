package com.rutau.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RatingRequestDTO(

        @NotNull(message = "Debes indicar el viaje")
        Long tripId,

        // A quién se califica: el conductor (si califica un pasajero) o un pasajero (si califica el conductor)
        @NotNull(message = "Debes indicar a quién calificas")
        Long ratedUserId,

        @NotNull(message = "Debes indicar una calificación")
        @Min(value = 1, message = "La calificación debe estar entre 1 y 5 estrellas")
        @Max(value = 5, message = "La calificación debe estar entre 1 y 5 estrellas")
        Integer score,

        @Size(max = 500, message = "El comentario no puede superar los 500 caracteres")
        String comment
) {}
