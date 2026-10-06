package com.rutau.service;

import com.rutau.dto.response.PublicProfileResponseDTO;
import com.rutau.exception.ResourceNotFoundException;
import com.rutau.model.RatedRole;
import com.rutau.model.TripStatus;
import com.rutau.model.User;
import com.rutau.repository.RatingRepository;
import com.rutau.repository.TripRepository;
import com.rutau.repository.UserRepository;
import com.rutau.util.Messages;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RatingRepository ratingRepository;
    private final TripRepository tripRepository;
    private final Messages messages;

    // ==================== US10 - Ver el perfil público del conductor ====================

    @Transactional(readOnly = true)
    public PublicProfileResponseDTO getPublicProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("user.not.found"));

        // Solo cuentan las calificaciones que recibió como CONDUCTOR
        long totalRatings = ratingRepository.countByRatedIdAndRatedRole(userId, RatedRole.DRIVER);
        Double average = ratingRepository.averageScore(userId, RatedRole.DRIVER);

        Double roundedAverage = null;
        String label;
        if (totalRatings == 0 || average == null) {
            // US10 - Escenario de error: sin viajes calificados no se muestra un promedio
            label = messages.get("profile.no.ratings");
        } else {
            roundedAverage = Math.round(average * 10) / 10.0;   // 1 decimal, ej. 4.3
            // US10 - Escenario alternativo: se muestra el total junto al promedio
            label = totalRatings == 1
                    ? messages.get("profile.rating.one", String.valueOf(roundedAverage))
                    : messages.get("profile.rating.many", String.valueOf(roundedAverage), totalRatings);
        }

        long completedTrips = tripRepository.countByDriverIdAndStatus(userId, TripStatus.COMPLETED);

        return new PublicProfileResponseDTO(user.getId(), user.getFullName(), user.getUniversity(),
                user.getAvatarUrl(), roundedAverage, label, totalRatings, completedTrips);
    }
}
