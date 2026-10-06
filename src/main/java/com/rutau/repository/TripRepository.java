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

    // US10 - Cantidad de viajes realizados por un conductor
    long countByDriverIdAndStatus(Long driverId, TripStatus status);

    // US12 - Oferta por hora de salida: [hora, cantidad de viajes, asientos ofrecidos]
    // (los viajes cancelados no cuentan como oferta). zone = "" significa todas las zonas
    @Query("SELECT EXTRACT(HOUR FROM t.departureTime), COUNT(t), SUM(t.totalSeats) FROM Trip t "
            + "WHERE t.status <> com.rutau.model.TripStatus.CANCELLED "
            + "AND (:zone = '' OR LOWER(t.zone) = LOWER(:zone)) "
            + "GROUP BY EXTRACT(HOUR FROM t.departureTime)")
    List<Object[]> countOfferByHour(@Param("zone") String zone);

    // US05 - Viajes activos: con asientos libres y que aún no han salido
    List<Trip> findByStatusAndDepartureTimeAfterOrderByDepartureTimeAsc(TripStatus status,
                                                                        LocalDateTime now);

    // US05 - Bloqueo pesimista: si dos operaciones cambian los asientos del mismo viaje
    // a la vez, la segunda espera a que termine la primera (evita asientos negativos)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Trip t WHERE t.id = :id")
    Optional<Trip> findByIdForUpdate(@Param("id") Long id);
}
