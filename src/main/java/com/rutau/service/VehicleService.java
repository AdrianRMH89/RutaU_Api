package com.rutau.service;

import com.rutau.dto.request.VehicleRequestDTO;
import com.rutau.dto.response.VehicleResponseDTO;
import com.rutau.exception.ConflictException;
import com.rutau.exception.ResourceNotFoundException;
import com.rutau.mapper.VehicleMapper;
import com.rutau.model.User;
import com.rutau.model.Vehicle;
import com.rutau.repository.VehicleRepository;
import com.rutau.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleMapper vehicleMapper;
    private final CurrentUser currentUser;

    @Transactional
    public VehicleResponseDTO register(VehicleRequestDTO dto) {
        User owner = currentUser.get();   // el usuario logueado es el dueño

        if (vehicleRepository.findByOwnerId(owner.getId()).isPresent()) {
            throw new ConflictException("vehicle.already.registered");
        }

        String plate = dto.plate().trim().toUpperCase();
        if (vehicleRepository.existsByPlate(plate)) {
            throw new ConflictException("vehicle.plate.duplicate", plate);
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setOwner(owner);
        vehicle.setBrand(dto.brand().trim());
        vehicle.setModel(dto.model().trim());
        vehicle.setColor(dto.color());
        vehicle.setPlate(plate);
        vehicle.setCapacity(dto.capacity());

        return vehicleMapper.toResponse(vehicleRepository.save(vehicle));
    }

    @Transactional(readOnly = true)
    public VehicleResponseDTO getMyVehicle() {
        User owner = currentUser.get();
        return vehicleRepository.findByOwnerId(owner.getId())
                .map(vehicleMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("vehicle.not.found"));
    }

    // US06 - El conductor actualiza los datos de su vehículo (por ejemplo, la capacidad)
    @Transactional
    public VehicleResponseDTO updateMyVehicle(VehicleRequestDTO dto) {
        User owner = currentUser.get();
        Vehicle vehicle = vehicleRepository.findByOwnerId(owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("vehicle.not.found"));

        String plate = dto.plate().trim().toUpperCase();
        if (!plate.equals(vehicle.getPlate()) && vehicleRepository.existsByPlate(plate)) {
            throw new ConflictException("vehicle.plate.duplicate", plate);
        }

        vehicle.setBrand(dto.brand().trim());
        vehicle.setModel(dto.model().trim());
        vehicle.setColor(dto.color());
        vehicle.setPlate(plate);
        vehicle.setCapacity(dto.capacity());

        return vehicleMapper.toResponse(vehicleRepository.save(vehicle));
    }
}
