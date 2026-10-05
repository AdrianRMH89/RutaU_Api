package com.rutau.service;

import com.rutau.dto.request.SeatRequestCreateDTO;
import com.rutau.dto.response.SeatRequestResponseDTO;
import com.rutau.exception.BusinessRuleException;
import com.rutau.exception.ConflictException;
import com.rutau.exception.ResourceNotFoundException;
import com.rutau.mapper.SeatRequestMapper;
import com.rutau.model.RequestStatus;
import com.rutau.model.SeatRequest;
import com.rutau.model.Trip;
import com.rutau.model.TripStatus;
import com.rutau.model.User;
import com.rutau.repository.SeatRequestRepository;
import com.rutau.repository.TripRepository;
import com.rutau.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SeatRequestService {

    // Dos viajes "se cruzan" si salen con 60 minutos o menos de diferencia
    private static final long OVERLAP_MINUTES = 60;

    private final SeatRequestRepository seatRequestRepository;
    private final TripRepository tripRepository;
    private final SeatRequestMapper seatRequestMapper;
    private final CurrentUser currentUser;

    // US03 - Solicitar un asiento en un viaje
    @Transactional
    public SeatRequestResponseDTO request(SeatRequestCreateDTO dto) {
        User passenger = currentUser.get();

        Trip trip = tripRepository.findById(dto.tripId())
                .orElseThrow(() -> new ResourceNotFoundException("Viaje no encontrado"));

        if (trip.getDriver().getId().equals(passenger.getId())) {
            throw new BusinessRuleException("No puedes solicitar un asiento en tu propio viaje");
        }

        if (trip.getStatus() == TripStatus.CANCELLED || trip.getStatus() == TripStatus.COMPLETED) {
            throw new BusinessRuleException("Este viaje ya no está disponible para solicitudes");
        }

        // US03 - Escenario de error
        if (trip.getAvailableSeats() <= 0) {
            throw new ConflictException("Este viaje ya no tiene cupos disponibles");
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
        seatRequest.setStatus(RequestStatus.PENDING);   // "Pendiente de confirmación"

        return seatRequestMapper.toResponse(seatRequestRepository.save(seatRequest));
    }

    // Solicitudes que hizo el usuario logueado como pasajero
    @Transactional(readOnly = true)
    public List<SeatRequestResponseDTO> getMyRequests() {
        User passenger = currentUser.get();
        return seatRequestRepository.findByPassengerId(passenger.getId()).stream()
                .map(seatRequestMapper::toResponse)
                .toList();
    }
}