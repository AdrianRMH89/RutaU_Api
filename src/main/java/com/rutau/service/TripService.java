package com.rutau.service;

import com.rutau.dto.request.CancelRequestDTO;
import com.rutau.dto.request.CompleteTripRequestDTO;
import com.rutau.dto.request.TripRequestDTO;
import com.rutau.dto.request.TripStopRequestDTO;
import com.rutau.dto.response.SeatRequestResponseDTO;
import com.rutau.dto.response.TripCompletionResponseDTO;
import com.rutau.dto.response.TripResponseDTO;
import com.rutau.dto.response.TripStopResponseDTO;
import com.rutau.exception.BusinessRuleException;
import com.rutau.exception.ConflictException;
import com.rutau.exception.ResourceNotFoundException;
import com.rutau.mapper.SeatRequestMapper;
import com.rutau.mapper.TripMapper;
import com.rutau.mapper.TripStopMapper;
import com.rutau.model.NotificationType;
import com.rutau.model.RequestStatus;
import com.rutau.model.SeatRequest;
import com.rutau.model.Trip;
import com.rutau.model.TripStatus;
import com.rutau.model.TripStop;
import com.rutau.model.User;
import com.rutau.model.Vehicle;
import com.rutau.repository.SeatRequestRepository;
import com.rutau.repository.TripRepository;
import com.rutau.repository.TripStopRepository;
import com.rutau.repository.VehicleRepository;
import com.rutau.security.CurrentUser;
import com.rutau.util.LimaZones;
import com.rutau.util.Messages;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TripService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final TripRepository tripRepository;
    private final VehicleRepository vehicleRepository;
    private final TripMapper tripMapper;
    private final TripStopRepository tripStopRepository;
    private final TripStopMapper tripStopMapper;
    private final SeatRequestRepository seatRequestRepository;
    private final SeatRequestMapper seatRequestMapper;
    private final NotificationService notificationService;
    private final CurrentUser currentUser;
    private final Messages messages;

    // US02 - Publicar un viaje como conductor
    @Transactional
    public TripResponseDTO publish(TripRequestDTO dto) {
        User driver = currentUser.get();

        // US06 - Escenario alternativo: sin vehículo (ni su capacidad) no se puede publicar
        Vehicle vehicle = vehicleRepository.findByOwnerId(driver.getId())
                .orElseThrow(() -> new BusinessRuleException(
                        "trip.vehicle.required"));

        // US02 - Escenario alternativo: 0 asientos
        if (dto.seats() == 0) {
            throw new BusinessRuleException(
                    "trip.zero.seats");
        }

        // US06 - Escenario de error: no se pueden ofrecer más asientos que la capacidad del vehículo
        if (dto.seats() > vehicle.getCapacity()) {
            throw new BusinessRuleException("trip.seats.over.capacity", vehicle.getCapacity());
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

        // US11 - Puntos intermedios: cada uno debe estar dentro de la ruta del viaje
        if (dto.stops() != null) {
            int order = 1;
            for (TripStopRequestDTO stopDto : dto.stops()) {
                if (!LimaZones.isOnRoute(trip.getZone(), stopDto.zone())) {
                    // US11 - Escenario de error
                    throw new BusinessRuleException("trip.stop.off.route");
                }
                TripStop stop = new TripStop();
                stop.setTrip(trip);
                stop.setAddress(stopDto.address().trim());
                stop.setZone(stopDto.zone().trim());
                stop.setStopOrder(order++);
                trip.getStops().add(stop);
            }
        }

        return tripMapper.toResponse(tripRepository.save(trip));
    }

    // US11 - Puntos intermedios de un viaje (para que el pasajero elija dónde subir o bajar)
    @Transactional(readOnly = true)
    public List<TripStopResponseDTO> getStops(Long tripId) {
        if (!tripRepository.existsById(tripId)) {
            throw new ResourceNotFoundException("trip.not.found");
        }
        return tripStopRepository.findByTripIdOrderByStopOrderAsc(tripId).stream()
                .map(tripStopMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TripResponseDTO getById(Long id) {
        return tripRepository.findById(id)
                .map(tripMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("trip.not.found"));
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
        checkIsDriver(trip, driver, "trip.complete.only.driver");

        if (trip.getStatus() == TripStatus.COMPLETED) {
            throw new ConflictException("trip.complete.already");
        }
        if (trip.getStatus() == TripStatus.CANCELLED) {
            throw new ConflictException("trip.complete.cancelled");
        }

        boolean confirmEarly = dto != null && Boolean.TRUE.equals(dto.confirmEarly());
        List<Long> completedIds = dto != null ? dto.completedRequestIds() : null;

        // US07 - Escenario de error: aún no llega la hora programada -> se pide confirmación
        if (LocalDateTime.now().isBefore(trip.getDepartureTime()) && !confirmEarly) {
            throw new ConflictException("trip.complete.early", trip.getDepartureTime().toString());
        }

        List<SeatRequest> requests = seatRequestRepository.findByTripIdAndStatusIn(
                tripId, List.of(RequestStatus.ACCEPTED, RequestStatus.PENDING));

        // Los ids indicados deben ser pasajeros aceptados de este viaje
        if (completedIds != null) {
            for (Long id : completedIds) {
                boolean valid = requests.stream().anyMatch(r ->
                        r.getId().equals(id) && r.getStatus() == RequestStatus.ACCEPTED);
                if (!valid) {
                    throw new BusinessRuleException("trip.complete.request.invalid", id.toString());
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
                messages.get("trip.complete.success"),
                passengers);
    }

    // ==================== US08 - Cancelar un viaje ya confirmado (conductor) ====================

    @Transactional
    public TripResponseDTO cancel(Long tripId, CancelRequestDTO dto) {
        User driver = currentUser.get();
        Trip trip = findTripForUpdate(tripId);
        checkIsDriver(trip, driver, "trip.cancel.only.driver");

        // US08 - Escenario de error: un viaje realizado ya no se puede cancelar
        if (trip.getStatus() == TripStatus.COMPLETED) {
            throw new ConflictException("trip.already.completed");
        }
        if (trip.getStatus() == TripStatus.CANCELLED) {
            throw new ConflictException("trip.already.cancelled");
        }

        String reason = dto.reason().trim();

        // Todas las solicitudes activas se cancelan y se avisa a cada pasajero
        List<SeatRequest> requests = seatRequestRepository.findByTripIdAndStatusIn(
                tripId, List.of(RequestStatus.ACCEPTED, RequestStatus.PENDING));
        for (SeatRequest request : requests) {
            request.setStatus(RequestStatus.CANCELLED);
            request.setCancelReason("Viaje cancelado por el conductor: " + reason);
            notificationService.notify(request.getPassenger(), NotificationType.TRIP_CANCELLED,
                    "El viaje " + trip.getOrigin() + " → " + trip.getDestination() + " del "
                            + trip.getDepartureTime().format(DATE_FORMAT)
                            + " fue cancelado por el conductor. Motivo: " + reason);
        }
        seatRequestRepository.saveAll(requests);

        // US08 - Escenario exitoso: el viaje pasa a "Cancelado"
        trip.setStatus(TripStatus.CANCELLED);
        return tripMapper.toResponse(tripRepository.save(trip));
    }

    // ==================== Métodos de apoyo ====================

    private Trip findTripForUpdate(Long tripId) {
        return tripRepository.findByIdForUpdate(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("trip.not.found"));
    }

    // Control por PROPIEDAD: solo el conductor del viaje puede gestionarlo (403)
    private void checkIsDriver(Trip trip, User user, String message) {
        if (!trip.getDriver().getId().equals(user.getId())) {
            throw new AccessDeniedException(message);
        }
    }
}
