package com.rutau.repository;

import com.rutau.model.Rating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    List<Rating> findByRatedId(Long ratedId);

    boolean existsByTripIdAndRaterIdAndRatedId(Long tripId, Long raterId, Long ratedId);
}
