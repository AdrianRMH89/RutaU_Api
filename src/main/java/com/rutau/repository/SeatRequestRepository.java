package com.rutau.repository;

import com.rutau.model.SeatRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatRequestRepository extends JpaRepository<SeatRequest, Long> {

    List<SeatRequest> findByTripId(Long tripId);

    List<SeatRequest> findByPassengerId(Long passengerId);

    boolean existsByTripIdAndPassengerId(Long tripId, Long passengerId);
}
