package com.rutau.controller;

import com.rutau.dto.request.VehicleRequestDTO;
import com.rutau.dto.response.VehicleResponseDTO;
import com.rutau.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "03. Vehículos", description = "Registro del vehículo y su capacidad (US06)")
@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @Operation(summary = "Registrar mi vehículo (US06)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleResponseDTO register(@Valid @RequestBody VehicleRequestDTO dto) {
        return vehicleService.register(dto);
    }

    @Operation(summary = "Ver mi vehículo")
    @GetMapping("/me")
    public VehicleResponseDTO myVehicle() {
        return vehicleService.getMyVehicle();
    }

    // US06 - Actualizar los datos (y la capacidad) de mi vehículo
    @Operation(summary = "Actualizar mi vehículo (US06)")
    @PutMapping("/me")
    public VehicleResponseDTO updateMyVehicle(@Valid @RequestBody VehicleRequestDTO dto) {
        return vehicleService.updateMyVehicle(dto);
    }
}
