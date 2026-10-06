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
    @Mapping(source = "trip.availableSeats", target = "tripAvailableSeats")
    @Mapping(source = "passenger.id", target = "passengerId")
    @Mapping(source = "passenger.fullName", target = "passengerName")
    @Mapping(source = "pickupStop.id", target = "pickupStopId")
    @Mapping(target = "pickupPoint", expression = "java(pickupPoint(seatRequest))")
    SeatRequestResponseDTO toResponse(SeatRequest seatRequest);

    // US11 - Escenario alternativo: sin punto intermedio se asume el origen del conductor
    default String pickupPoint(SeatRequest seatRequest) {
        if (seatRequest.getPickupStop() != null) {
            return seatRequest.getPickupStop().getAddress()
                    + " (" + seatRequest.getPickupStop().getZone() + ")";
        }
        return "Origen del conductor: " + seatRequest.getTrip().getOrigin();
    }
}