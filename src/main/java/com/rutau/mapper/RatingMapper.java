package com.rutau.mapper;

import com.rutau.dto.response.RatingResponseDTO;
import com.rutau.model.Rating;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RatingMapper {

    @Mapping(source = "trip.id", target = "tripId")
    @Mapping(source = "rater.id", target = "raterId")
    @Mapping(source = "rater.fullName", target = "raterName")
    @Mapping(source = "rated.id", target = "ratedId")
    @Mapping(source = "rated.fullName", target = "ratedName")
    RatingResponseDTO toResponse(Rating rating);
}
