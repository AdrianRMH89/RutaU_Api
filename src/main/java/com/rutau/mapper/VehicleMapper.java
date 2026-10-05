package com.rutau.mapper;

import com.rutau.dto.response.VehicleResponseDTO;
import com.rutau.model.Vehicle;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VehicleMapper {

    VehicleResponseDTO toResponse(Vehicle vehicle);
}