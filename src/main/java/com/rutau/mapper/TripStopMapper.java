package com.rutau.mapper;

import com.rutau.dto.response.TripStopResponseDTO;
import com.rutau.model.TripStop;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TripStopMapper {

    TripStopResponseDTO toResponse(TripStop tripStop);
}
