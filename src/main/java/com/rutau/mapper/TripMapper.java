package com.rutau.mapper;

import com.rutau.dto.response.TripResponseDTO;
import com.rutau.model.Trip;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = TripStopMapper.class)
public interface TripMapper {

    @Mapping(source = "driver.id", target = "driverId")
    @Mapping(source = "driver.fullName", target = "driverName")
    @Mapping(source = "vehicle.brand", target = "vehicleBrand")
    @Mapping(source = "vehicle.model", target = "vehicleModel")
    @Mapping(source = "vehicle.plate", target = "vehiclePlate")
    TripResponseDTO toResponse(Trip trip);
}