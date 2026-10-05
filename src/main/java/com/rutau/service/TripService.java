package com.rutau.service;

import com.rutau.dto.request.CompleteTripRequestDTO;
import com.rutau.dto.request.TripRequestDTO;
import com.rutau.dto.response.SeatRequestResponseDTO;
import com.rutau.dto.response.TripCompletionResponseDTO;
import com.rutau.dto.response.TripResponseDTO;
import com.rutau.exception.BusinessRuleException;
import com.rutau.exception.ConflictException;
import com.rutau.exception.ResourceNotFoundException;
import com.rutau.mapper.SeatRequestMapper;
import com.rutau.mapper.TripMapper;
import com.rutau.model.RequestStatus;
import com.rutau.model.SeatRequest;
import com.rutau.model.Trip;
import com.rutau.model.TripStatus;
import com.rutau.model.User;
import com.rutau.model.Vehicle;
import com.rutau.repository.SeatRequestRepository;
import com.rutau.repository.TripRepository;
import com.rutau.repository.VehicleRepository;
import com.rutau.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TripService {

    private final TripRepository tripRepository;
    private final VehicleRepository vehicleRepository;
    private final TripMapper tripMapper;
    private final SeatRequestRepository seatRequestRepository;
    private final SeatRequestMapper seatRequestMapper;
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

    // ==================== US07 - Marcar un viaje como realizado ====================

    @Transactional
    public TripCompletionResponseDTO complete(Long tripId, CompleteTripRequestDTO dto) {
        User driver = currentUser.get();
        Trip trip = findTripForUpdate(tripId);
        checkIsDriver(trip, driver, "Solo el conductor del viaje puede marcarlo como realizado");

        if (trip.getStatus() == TripStatus.COMPLETED) {
            throw new ConflictException("Este viaje ya fue marcado como realizado");
        }
        if (trip.getStatus() == TripStatus.CANCELLED) {
            throw new ConflictException("No se puede marcar como realizado un viaje cancelado");
        }

        boolean confirmEarly = dto != null && Boolean.TRUE.equals(dto.confirmEarly());
        List<Long> completedIds = dto != null ? dto.completedRequestIds() : null;

        // US07 - Escenario de error: aún no llega la hora programada -> se pide confirmación
        if (LocalDateTime.now().isBefore(trip.getDepartureTime()) && !confirmEarly) {
            throw new ConflictException("El viaje aún no llega a su hora programada ("
                    + trip.getDepartureTime() + "). Si ya lo realizaste, confirma la acción "
                    + "enviando confirmEarly: true");
        }

        List<SeatRequest> requests = seatRequestRepository.findByTripIdAndStatusIn(
                tripId, List.of(RequestStatus.ACCEPTED, RequestStatus.PENDING));

        // Los ids indicados deben ser pasajeros aceptados de este viaje
        if (completedIds != null) {
            for (Long id : completedIds) {
                boolean valid = requests.stream().anyMatch(r ->
                        r.getId().equals(id) && r.getStatus() == RequestStatus.ACCEPTED);
                if (!valid) {
                    throw new BusinessRuleException("La solicitud " + id
                            + " no corresponde a un pasajero aceptado de este viaje");
                }
            }
        }

        List<SeatRequestResponseDTO> passengers = new ArrayList<>();
        for (SeatRequest request : requests) {
            if (request.getStatus() == RequestStatus.PENDING) {
                // Solicitudes que nunca se respondieron ya no tienen sentido
                request.setStatus(RequestStatus.CANCELLED);
                request.setCancelReason("El viaje ya se realizó");
            } else if (completedIds == null || completedIds.contains(request.getId())) {
                // US07 - El viaje queda completo para el pasajero (y se habilita la calificación)
                request.setStatus(RequestStatus.COMPLETED);
                passengers.add(seatRequestMapper.toResponse(request));
            } else {
                // US07 - Escenario alternativo: el pasajero no llegó a su punto acordado
                request.setStatus(RequestStatus.CANCELLED);
                request.setCancelReason("No completó el viaje hasta su punto acordado");
                passengers.add(seatRequestMapper.toResponse(request));
            }
        }
        seatRequestRepository.saveAll(requests);

        trip.setStatus(TripStatus.COMPLETED);
        tripRepository.save(trip);

        return new TripCompletionResponseDTO(trip.getId(), trip.getStatus(),
                "Viaje marcado como realizado. Calificación habilitada para el conductor y los pasajeros que completaron el viaje.",
                passengers);
    }

    // ==================== Métodos de apoyo ====================

    private Trip findTripForUpdate(Long tripId) {
        return tripRepository.findByIdForUpdate(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Viaje no encontrado"));
    }

    // Control por PROPIEDAD: solo el conductor del viaje puede gestionarlo (403)
    private void checkIsDriver(Trip trip, User user, String message) {
        if (!trip.getDriver().getId().equals(user.getId())) {
            throw new AccessDeniedException(message);
        }
    }
}
