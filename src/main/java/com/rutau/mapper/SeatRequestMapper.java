package com.rutau.mapper;

import com.rutau.dto.response.SeatRequestResponseDTO;
import com.rutau.model.SeatRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SeatRequestMapper {

    @Mapping(source = "trip.id", target = "tripId")
    @Mapping(source = "trip.origin", target = "origin")
    @Mapping(source = "trip.destination", target = "destination")
    @Mapping(source = "trip.departureTime", target = "departureTime")
    @Mapping(source = "passenger.id", target = "passengerId")
    @Mapping(source = "passenger.fullName", target = "passengerName")
    SeatRequestResponseDTO toResponse(SeatRequest seatRequest);
}