package com.rutau.dto.response;

import com.rutau.model.RatedRole;

import java.time.LocalDateTime;

public record RatingResponseDTO(
        Long id,
        Long tripId,
        Long raterId,
        String raterName,
        Long ratedId,
        String ratedName,
        RatedRole ratedRole,
        Integer score,
        String comment,
        LocalDateTime createdAt
) {}
