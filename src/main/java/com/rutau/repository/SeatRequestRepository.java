package com.rutau.repository;

import com.rutau.model.RequestStatus;
import com.rutau.model.SeatRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface SeatRequestRepository extends JpaRepository<SeatRequest, Long> {

    List<SeatRequest> findByTripId(Long tripId);

    List<SeatRequest> findByPassengerId(Long passengerId);

    boolean existsByTripIdAndPassengerId(Long tripId, Long passengerId);

    // US03 - ¿El pasajero tiene otra solicitud activa en un viaje que sale entre "start" y "end"?
    boolean existsByPassengerIdAndStatusInAndTripDepartureTimeBetween(
            Long passengerId,
            Collection<RequestStatus> statuses,
            LocalDateTime start,
            LocalDateTime end);

    // US07 / US08 - Solicitudes de un viaje que están en ciertos estados (ej. PENDING y ACCEPTED)
    List<SeatRequest> findByTripIdAndStatusIn(Long tripId, Collection<RequestStatus> statuses);

    // US09 - ¿El pasajero completó este viaje? (requisito para calificar o ser calificado)
    boolean existsByTripIdAndPassengerIdAndStatus(Long tripId, Long passengerId, RequestStatus status);
}
