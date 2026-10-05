package com.rutau.repository;

import com.rutau.model.Trip;
import com.rutau.model.TripStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {

    List<Trip> findByDriverId(Long driverId);

    // US05 - Viajes activos: con asientos libres y que aún no han salido
    List<Trip> findByStatusAndDepartureTimeAfterOrderByDepartureTimeAsc(TripStatus status,
                                                                        LocalDateTime now);

    // US05 - Bloqueo pesimista: si dos operaciones cambian los asientos del mismo viaje
    // a la vez, la segunda espera a que termine la primera (evita asientos negativos)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Trip t WHERE t.id = :id")
    Optional<Trip> findByIdForUpdate(@Param("id") Long id);
}
