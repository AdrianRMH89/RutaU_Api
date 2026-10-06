package com.rutau.service;

import com.rutau.dto.request.RatingRequestDTO;
import com.rutau.dto.response.RatingHistoryResponseDTO;
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
import com.rutau.util.Messages;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;
    private final TripRepository tripRepository;
    private final SeatRequestRepository seatRequestRepository;
    private final UserRepository userRepository;
    private final RatingMapper ratingMapper;
    private final CurrentUser currentUser;
    private final Messages messages;

    // ==================== US09 - Calificar el viaje al finalizar ====================

    @Transactional
    public RatingResponseDTO rate(RatingRequestDTO dto) {
        User rater = currentUser.get();

        Trip trip = tripRepository.findById(dto.tripId())
                .orElseThrow(() -> new ResourceNotFoundException("trip.not.found"));

        // US09 - Escenario alternativo: un viaje cancelado no habilita la calificación
        if (trip.getStatus() == TripStatus.CANCELLED) {
            throw new BusinessRuleException("rating.trip.cancelled");
        }

        // US09 - Escenario de error: solo se califican viajes ya finalizados
        if (trip.getStatus() != TripStatus.COMPLETED) {
            throw new BusinessRuleException("rating.trip.not.completed");
        }

        if (rater.getId().equals(dto.ratedUserId())) {
            throw new BusinessRuleException("rating.self");
        }

        User rated = userRepository.findById(dto.ratedUserId())
                .orElseThrow(() -> new ResourceNotFoundException("rating.user.not.found"));

        Long driverId = trip.getDriver().getId();
        RatedRole ratedRole;

        if (rater.getId().equals(driverId)) {
            // El conductor califica a un pasajero que completó el viaje
            if (!completedTrip(trip, rated)) {
                throw new BusinessRuleException("rating.only.completed.passengers");
            }
            ratedRole = RatedRole.PASSENGER;
        } else if (completedTrip(trip, rater)) {
            // Un pasajero que completó el viaje califica al conductor
            if (!rated.getId().equals(driverId)) {
                throw new BusinessRuleException("rating.passenger.only.driver");
            }
            ratedRole = RatedRole.DRIVER;
        } else {
            throw new AccessDeniedException("rating.not.participant");
        }

        if (ratingRepository.existsByTripIdAndRaterIdAndRatedId(trip.getId(), rater.getId(), rated.getId())) {
            throw new ConflictException("rating.duplicate");
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

    // ==================== US16 - Consultar historial de calificaciones ====================

    // role es opcional: DRIVER (como conductor), PASSENGER (como pasajero) o null (todas)
    @Transactional(readOnly = true)
    public RatingHistoryResponseDTO getMyRatings(RatedRole role) {
        User user = currentUser.get();

        // US16 - Escenario alternativo: filtrar por rol
        List<Rating> ratings = role == null
                ? ratingRepository.findByRatedIdOrderByCreatedAtDesc(user.getId())
                : ratingRepository.findByRatedIdAndRatedRoleOrderByCreatedAtDesc(user.getId(), role);

        String roleLabel = role == null ? messages.get("ratings.all.roles") : role.name();

        if (ratings.isEmpty()) {
            // US16 - Escenario de error
            String message = role == null
                    ? messages.get("ratings.empty")
                    : messages.get(role == RatedRole.DRIVER ? "ratings.empty.driver" : "ratings.empty.passenger");
            return new RatingHistoryResponseDTO(message, roleLabel, 0L, null, List.of());
        }

        double average = ratings.stream().mapToInt(Rating::getScore).average().orElse(0);
        double rounded = Math.round(average * 10) / 10.0;

        // US16 - Escenario exitoso: fecha, puntaje y comentario de cada calificación
        return new RatingHistoryResponseDTO(messages.get("ratings.ok"), roleLabel,
                (long) ratings.size(), rounded,
                ratings.stream().map(ratingMapper::toResponse).toList());
    }
}
