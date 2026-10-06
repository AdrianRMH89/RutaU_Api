package com.rutau.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RatingRequestDTO(

        @NotNull(message = "{validation.trip.required}")
        Long tripId,

        // A quién se califica: el conductor (si califica un pasajero) o un pasajero (si califica el conductor)
        @NotNull(message = "{validation.rated.user.required}")
        Long ratedUserId,

        @NotNull(message = "{validation.rating.required}")
        @Min(value = 1, message = "{validation.rating.range}")
        @Max(value = 5, message = "{validation.rating.range}")
        Integer score,

        @Size(max = 500, message = "{validation.comment.max}")
        String comment
) {}
