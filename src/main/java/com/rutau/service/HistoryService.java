package com.rutau.service;

import com.rutau.dto.response.TripHistoryItemDTO;
import com.rutau.dto.response.TripHistoryResponseDTO;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class HistoryService {

    // Estados que significan "el viaje sigue en curso"
    private static final Set<TripStatus> TRIP_IN_PROGRESS = Set.of(TripStatus.SCHEDULED, TripStatus.FULL);
    private static final Set<RequestStatus> REQUEST_IN_PROGRESS = Set.of(RequestStatus.PENDING, RequestStatus.ACCEPTED);

    private final TripRepository tripRepository;
    private final SeatRequestRepository seatRequestRepository;
    private final CurrentUser currentUser;

    // ==================== US15 - Consultar el historial de viajes ====================

    @Transactional(readOnly = true)
    public TripHistoryResponseDTO getMyHistory() {
        User user = currentUser.get();

        List<TripHistoryItemDTO> asDriver = new ArrayList<>();
        List<TripHistoryItemDTO> asPassenger = new ArrayList<>();
        List<TripHistoryItemDTO> inProgress = new ArrayList<>();

        // Como CONDUCTOR: los viajes que publiqué
        for (Trip trip : tripRepository.findByDriverId(user.getId())) {
            long passengers = seatRequestRepository.findByTripIdAndStatusIn(trip.getId(),
                    List.of(RequestStatus.ACCEPTED, RequestStatus.COMPLETED)).size();
            TripHistoryItemDTO item = new TripHistoryItemDTO(trip.getId(), "DRIVER", trip.getDepartureTime(),
                    trip.getOrigin(), trip.getDestination(), trip.getStatus().name(), passengers + " pasajero(s)");
            if (TRIP_IN_PROGRESS.contains(trip.getStatus())) {
                inProgress.add(item);
            } else {
                asDriver.add(item);
            }
        }

        // Como PASAJERO: mis solicitudes de asiento
        for (SeatRequest request : seatRequestRepository.findByPassengerId(user.getId())) {
            Trip trip = request.getTrip();
            TripHistoryItemDTO item = new TripHistoryItemDTO(trip.getId(), "PASSENGER", trip.getDepartureTime(),
                    trip.getOrigin(), trip.getDestination(), request.getStatus().name(),
                    "Conductor: " + trip.getDriver().getFullName());
            if (REQUEST_IN_PROGRESS.contains(request.getStatus())) {
                inProgress.add(item);
            } else {
                asPassenger.add(item);
            }
        }

        // Historial: lo más reciente primero. En curso: lo más próximo primero.
        Comparator<TripHistoryItemDTO> byDate = Comparator.comparing(TripHistoryItemDTO::departureTime);
        asDriver.sort(byDate.reversed());
        asPassenger.sort(byDate.reversed());
        inProgress.sort(byDate);

        String message;
        if (asDriver.isEmpty() && asPassenger.isEmpty() && inProgress.isEmpty()) {
            // US15 - Escenario de error
            message = "Aún no tienes viajes registrados";
        } else if (asDriver.isEmpty() && asPassenger.isEmpty()) {
            message = "Aún no tienes viajes terminados; tienes " + inProgress.size() + " viaje(s) en curso";
        } else {
            message = "Historial de viajes";
        }

        return new TripHistoryResponseDTO(message, asDriver, asPassenger, inProgress);
    }
}
