package com.rutau.service;

import com.rutau.dto.request.CancelRequestDTO;
import com.rutau.dto.request.SeatRequestCreateDTO;
import com.rutau.dto.response.SeatRequestResponseDTO;
import com.rutau.exception.BusinessRuleException;
import com.rutau.exception.ConflictException;
import com.rutau.exception.ResourceNotFoundException;
import com.rutau.mapper.SeatRequestMapper;
import com.rutau.model.NotificationType;
import com.rutau.model.RequestStatus;
import com.rutau.model.SeatRequest;
import com.rutau.model.Trip;
import com.rutau.model.TripStatus;
import com.rutau.model.TripStop;
import com.rutau.model.User;
import com.rutau.repository.SeatRequestRepository;
import com.rutau.repository.TripRepository;
import com.rutau.repository.TripStopRepository;
import com.rutau.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeatRequestService {

    // Dos viajes "se cruzan" si salen con 60 minutos o menos de diferencia
    private static final long OVERLAP_MINUTES = 60;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final SeatRequestRepository seatRequestRepository;
    private final TripRepository tripRepository;
    private final TripStopRepository tripStopRepository;
    private final SeatRequestMapper seatRequestMapper;
    private final CurrentUser currentUser;
    private final NotificationService notificationService;

    // ==================== US03 - Solicitar un asiento ====================

    @Transactional
    public SeatRequestResponseDTO request(SeatRequestCreateDTO dto) {
        User passenger = currentUser.get();

        Trip trip = findTrip(dto.tripId());

        if (trip.getDriver().getId().equals(passenger.getId())) {
            throw new BusinessRuleException("No puedes solicitar un asiento en tu propio viaje");
        }

        if (trip.getStatus() == TripStatus.CANCELLED || trip.getStatus() == TripStatus.COMPLETED) {
            throw new BusinessRuleException("Este viaje ya no está disponible para solicitudes");
        }

        // US03 - Escenario de error / US05 - Escenario de error
        if (trip.getAvailableSeats() <= 0) {
            throw new ConflictException("Viaje completo: este viaje ya no tiene cupos disponibles");
        }

        if (seatRequestRepository.existsByTripIdAndPassengerId(trip.getId(), passenger.getId())) {
            throw new ConflictException("Ya solicitaste un asiento en este viaje");
        }

        // US03 - Escenario alternativo: cruce de horarios con otra solicitud activa
        boolean overlap = seatRequestRepository.existsByPassengerIdAndStatusInAndTripDepartureTimeBetween(
                passenger.getId(),
                List.of(RequestStatus.PENDING, RequestStatus.ACCEPTED),
                trip.getDepartureTime().minusMinutes(OVERLAP_MINUTES),
                trip.getDepartureTime().plusMinutes(OVERLAP_MINUTES));

        if (overlap && !Boolean.TRUE.equals(dto.confirmOverlap())) {
            throw new ConflictException(
                    "Ya tienes una solicitud en otro viaje que se cruza con este horario. "
                    + "Si deseas continuar, vuelve a enviar la solicitud con confirmOverlap: true");
        }

        SeatRequest seatRequest = new SeatRequest();
        seatRequest.setTrip(trip);
        seatRequest.setPassenger(passenger);

        // US11 - Escenario exitoso: el pasajero indica un punto intermedio de recojo o bajada.
        // Escenario alternativo: si no lo indica, queda null y se usa el origen del conductor.
        if (dto.pickupStopId() != null) {
            TripStop stop = tripStopRepository.findById(dto.pickupStopId())
                    .orElseThrow(() -> new ResourceNotFoundException("Punto intermedio no encontrado"));
            if (!stop.getTrip().getId().equals(trip.getId())) {
                throw new BusinessRuleException("El punto intermedio indicado no pertenece a este viaje");
            }
            seatRequest.setPickupStop(stop);
        }
        seatRequest.setStatus(RequestStatus.PENDING);   // "Pendiente de confirmación"

        return seatRequestMapper.toResponse(seatRequestRepository.save(seatRequest));
    }

    @Transactional(readOnly = true)
    public List<SeatRequestResponseDTO> getMyRequests() {
        User passenger = currentUser.get();
        return seatRequestRepository.findByPassengerId(passenger.getId()).stream()
                .map(seatRequestMapper::toResponse)
                .toList();
    }

    // ==================== US04 - Aceptar o rechazar una solicitud ====================

    // El conductor ve las solicitudes que recibió en uno de sus viajes
    @Transactional(readOnly = true)
    public List<SeatRequestResponseDTO> getRequestsForMyTrip(Long tripId) {
        User driver = currentUser.get();
        Trip trip = findTrip(tripId);
        checkIsDriver(trip, driver);

        return seatRequestRepository.findByTripId(tripId).stream()
                .map(seatRequestMapper::toResponse)
                .toList();
    }

    // US04 - Escenario exitoso y de error
    @Transactional
    public SeatRequestResponseDTO accept(Long requestId) {
        User driver = currentUser.get();
        SeatRequest seatRequest = findRequest(requestId);

        // US05 - Se bloquea el viaje para que dos aceptaciones simultáneas no se pisen
        Trip trip = findTripForUpdate(seatRequest.getTrip().getId());

        checkIsDriver(trip, driver);
        checkIsPending(seatRequest);

        // US04 - Escenario de error: el viaje ya está completo
        if (trip.getAvailableSeats() <= 0) {
            throw new ConflictException("Ya no hay asientos disponibles para aceptar esta solicitud");
        }

        // US04 - Escenario exitoso: se reserva el asiento y se descuenta
        trip.setAvailableSeats(trip.getAvailableSeats() - 1);

        // US05 - Escenario exitoso: al llegar a 0 asientos el viaje pasa a FULL
        // y deja de mostrarse en los viajes activos
        if (trip.getAvailableSeats() == 0) {
            trip.setStatus(TripStatus.FULL);
        }

        seatRequest.setStatus(RequestStatus.ACCEPTED);

        tripRepository.save(trip);
        return seatRequestMapper.toResponse(seatRequestRepository.save(seatRequest));
    }

    // US04 - Escenario alternativo: rechazar (el asiento sigue disponible)
    @Transactional
    public SeatRequestResponseDTO reject(Long requestId) {
        User driver = currentUser.get();
        SeatRequest seatRequest = findRequest(requestId);

        checkIsDriver(seatRequest.getTrip(), driver);
        checkIsPending(seatRequest);

        seatRequest.setStatus(RequestStatus.REJECTED);
        return seatRequestMapper.toResponse(seatRequestRepository.save(seatRequest));
    }

    // ==================== US05 / US08 - Cancelación por parte del pasajero ====================

    // US05 - Escenario alternativo: el pasajero cancela su reserva y el asiento se libera solo
    // US08 - Escenario alternativo: el viaje sigue activo para los demás pasajeros
    @Transactional
    public SeatRequestResponseDTO cancelByPassenger(Long requestId, CancelRequestDTO dto) {
        User passenger = currentUser.get();
        SeatRequest seatRequest = findRequest(requestId);

        // Control por PROPIEDAD: solo el pasajero que hizo la solicitud puede cancelarla
        if (!seatRequest.getPassenger().getId().equals(passenger.getId())) {
            throw new AccessDeniedException("Solo el pasajero que hizo la solicitud puede cancelarla");
        }

        // US08 - Escenario de error: no se puede cancelar un viaje ya realizado
        if (seatRequest.getTrip().getStatus() == TripStatus.COMPLETED) {
            throw new ConflictException("Este viaje ya fue completado");
        }

        if (seatRequest.getStatus() != RequestStatus.PENDING
                && seatRequest.getStatus() != RequestStatus.ACCEPTED) {
            throw new ConflictException("Esta solicitud ya no se puede cancelar (estado: "
                    + seatRequest.getStatus() + ")");
        }

        Trip trip = findTripForUpdate(seatRequest.getTrip().getId());

        // Si el asiento ya estaba reservado, se libera automáticamente
        if (seatRequest.getStatus() == RequestStatus.ACCEPTED) {
            trip.setAvailableSeats(trip.getAvailableSeats() + 1);
            if (trip.getStatus() == TripStatus.FULL) {
                trip.setStatus(TripStatus.SCHEDULED);   // vuelve a aparecer en los viajes activos
            }
            tripRepository.save(trip);
        }

        seatRequest.setStatus(RequestStatus.CANCELLED);
        seatRequest.setCancelReason(dto.reason().trim());

        // US08 - Se notifica a la otra parte (el conductor)
        notificationService.notify(trip.getDriver(), NotificationType.REQUEST_CANCELLED,
                passenger.getFullName() + " canceló su asiento en tu viaje " + describe(trip)
                        + ". Motivo: " + dto.reason().trim());

        return seatRequestMapper.toResponse(seatRequestRepository.save(seatRequest));
    }

    // ==================== Métodos de apoyo ====================

    private String describe(Trip trip) {
        return trip.getOrigin() + " → " + trip.getDestination()
                + " del " + trip.getDepartureTime().format(DATE_FORMAT);
    }

    private Trip findTrip(Long tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Viaje no encontrado"));
    }

    private Trip findTripForUpdate(Long tripId) {
        return tripRepository.findByIdForUpdate(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Viaje no encontrado"));
    }

    private SeatRequest findRequest(Long requestId) {
        return seatRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada"));
    }

    // Control por PROPIEDAD: solo el conductor del viaje puede gestionar sus solicitudes (403)
    private void checkIsDriver(Trip trip, User user) {
        if (!trip.getDriver().getId().equals(user.getId())) {
            throw new AccessDeniedException("Solo el conductor del viaje puede gestionar sus solicitudes");
        }
    }

    private void checkIsPending(SeatRequest seatRequest) {
        if (seatRequest.getStatus() != RequestStatus.PENDING) {
            throw new ConflictException("Esta solicitud ya fue procesada (estado: "
                    + seatRequest.getStatus() + ")");
        }
    }
}
