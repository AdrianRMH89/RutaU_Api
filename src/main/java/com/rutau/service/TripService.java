package com.rutau.service;

import com.rutau.dto.request.TripRequestDTO;
import com.rutau.dto.response.TripResponseDTO;
import com.rutau.exception.BusinessRuleException;
import com.rutau.exception.ResourceNotFoundException;
import com.rutau.mapper.TripMapper;
import com.rutau.model.Trip;
import com.rutau.model.TripStatus;
import com.rutau.model.User;
import com.rutau.model.Vehicle;
import com.rutau.repository.TripRepository;
import com.rutau.repository.VehicleRepository;
import com.rutau.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TripService {

    private final TripRepository tripRepository;
    private final VehicleRepository vehicleRepository;
    private final TripMapper tripMapper;
    private final CurrentUser currentUser;

    // US02 - Publicar un viaje como conductor
    @Transactional
    public TripResponseDTO publish(TripRequestDTO dto) {
        User driver = currentUser.get();

        // US06 - Escenario alternativo: sin vehículo (ni su capacidad) no se puede publicar
        Vehicle vehicle = vehicleRepository.findByOwnerId(driver.getId())
                .orElseThrow(() -> new BusinessRuleException(
                        "Debes registrar tu vehículo y su capacidad antes de publicar un viaje"));

        // US02 - Escenario alternativo: 0 asientos
        if (dto.seats() == 0) {
            throw new BusinessRuleException(
                    "Un viaje con 0 asientos no puede publicarse como disponible; márcalo como 'solo referencial'");
        }

        // US06 - Escenario de error: no se pueden ofrecer más asientos que la capacidad del vehículo
        if (dto.seats() > vehicle.getCapacity()) {
            throw new BusinessRuleException(
                    "El número de asientos supera la capacidad de tu vehículo (máximo "
                    + vehicle.getCapacity() + ")");
        }

        Trip trip = new Trip();
        trip.setDriver(driver);
        trip.setVehicle(vehicle);
        trip.setOrigin(dto.origin().trim());
        trip.setDestination(dto.destination().trim());
        trip.setZone(dto.zone().trim());
        trip.setDepartureTime(dto.departureTime());
        trip.setTotalSeats(dto.seats());
        trip.setAvailableSeats(dto.seats());   // al publicar, todos los asientos están libres
        trip.setPricePerSeat(dto.pricePerSeat());
        trip.setStatus(TripStatus.SCHEDULED);

        return tripMapper.toResponse(tripRepository.save(trip));
    }

    @Transactional(readOnly = true)
    public TripResponseDTO getById(Long id) {
        return tripRepository.findById(id)
                .map(tripMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Viaje no encontrado"));
    }

    // Viajes que publicó el usuario logueado como conductor
    @Transactional(readOnly = true)
    public List<TripResponseDTO> getMyTrips() {
        User driver = currentUser.get();
        return tripRepository.findByDriverId(driver.getId()).stream()
                .map(tripMapper::toResponse)
                .toList();
    }

    // US05 - Viajes activos: solo los que tienen asientos libres (SCHEDULED) y aún no salen.
    // Un viaje FULL deja de mostrarse aquí.
    @Transactional(readOnly = true)
    public List<TripResponseDTO> getActiveTrips() {
        return tripRepository.findByStatusAndDepartureTimeAfterOrderByDepartureTimeAsc(
                        TripStatus.SCHEDULED, LocalDateTime.now()).stream()
                .map(tripMapper::toResponse)
                .toList();
    }
}
