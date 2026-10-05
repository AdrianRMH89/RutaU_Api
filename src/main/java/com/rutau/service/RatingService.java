package com.rutau.service;

import com.rutau.dto.request.RatingRequestDTO;
import com.rutau.dto.response.RatingResponseDTO;
import com.rutau.exception.BusinessRuleException;
import com.rutau.exception.ConflictException;
import com.rutau.exception.ResourceNotFoundException;
import com.rutau.mapper.RatingMapper;
import com.rutau.model.RatedRole;
import com.rutau.model.Rating;
import com.rutau.model.RequestStatus;
import com.rutau.model.Trip;
import com.rutau.model.TripStatus;
import com.rutau.model.User;
import com.rutau.repository.RatingRepository;
import com.rutau.repository.SeatRequestRepository;
import com.rutau.repository.TripRepository;
import com.rutau.repository.UserRepository;
import com.rutau.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;
    private final TripRepository tripRepository;
    private final SeatRequestRepository seatRequestRepository;
    private final UserRepository userRepository;
    private final RatingMapper ratingMapper;
    private final CurrentUser currentUser;

    // ==================== US09 - Calificar el viaje al finalizar ====================

    @Transactional
    public RatingResponseDTO rate(RatingRequestDTO dto) {
        User rater = currentUser.get();

        Trip trip = tripRepository.findById(dto.tripId())
                .orElseThrow(() -> new ResourceNotFoundException("Viaje no encontrado"));

        // US09 - Escenario alternativo: un viaje cancelado no habilita la calificación
        if (trip.getStatus() == TripStatus.CANCELLED) {
            throw new BusinessRuleException("La calificación no está habilitada para viajes cancelados");
        }

        // US09 - Escenario de error: solo se califican viajes ya finalizados
        if (trip.getStatus() != TripStatus.COMPLETED) {
            throw new BusinessRuleException("Solo puedes calificar viajes ya finalizados");
        }

        if (rater.getId().equals(dto.ratedUserId())) {
            throw new BusinessRuleException("No puedes calificarte a ti mismo");
        }

        User rated = userRepository.findById(dto.ratedUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario a calificar no encontrado"));

        Long driverId = trip.getDriver().getId();
        RatedRole ratedRole;

        if (rater.getId().equals(driverId)) {
            // El conductor califica a un pasajero que completó el viaje
            if (!completedTrip(trip, rated)) {
                throw new BusinessRuleException("Solo puedes calificar a pasajeros que completaron este viaje");
            }
            ratedRole = RatedRole.PASSENGER;
        } else if (completedTrip(trip, rater)) {
            // Un pasajero que completó el viaje califica al conductor
            if (!rated.getId().equals(driverId)) {
                throw new BusinessRuleException("Como pasajero solo puedes calificar al conductor del viaje");
            }
            ratedRole = RatedRole.DRIVER;
        } else {
            throw new AccessDeniedException("Solo los participantes de este viaje pueden calificarlo");
        }

        if (ratingRepository.existsByTripIdAndRaterIdAndRatedId(trip.getId(), rater.getId(), rated.getId())) {
            throw new ConflictException("Ya calificaste a este usuario en este viaje");
        }

        // US09 - Escenario exitoso: la calificación queda en el perfil del otro usuario
        Rating rating = new Rating();
        rating.setTrip(trip);
        rating.setRater(rater);
        rating.setRated(rated);
        rating.setRatedRole(ratedRole);
        rating.setScore(dto.score());
        rating.setComment(dto.comment() != null ? dto.comment().trim() : null);

        return ratingMapper.toResponse(ratingRepository.save(rating));
    }

    // ¿El usuario viajó como pasajero y completó este viaje?
    private boolean completedTrip(Trip trip, User user) {
        return seatRequestRepository.existsByTripIdAndPassengerIdAndStatus(
                trip.getId(), user.getId(), RequestStatus.COMPLETED);
    }
}
