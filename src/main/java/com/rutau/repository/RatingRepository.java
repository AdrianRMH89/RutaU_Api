package com.rutau.repository;

import com.rutau.model.RatedRole;
import com.rutau.model.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    List<Rating> findByRatedId(Long ratedId);

    // US16 - Calificaciones recibidas (todas o solo de un rol), las más recientes primero
    List<Rating> findByRatedIdOrderByCreatedAtDesc(Long ratedId);

    List<Rating> findByRatedIdAndRatedRoleOrderByCreatedAtDesc(Long ratedId, RatedRole ratedRole);

    boolean existsByTripIdAndRaterIdAndRatedId(Long tripId, Long raterId, Long ratedId);

    // US10 - Promedio de estrellas que recibió un usuario en un rol (null si no tiene calificaciones)
    @Query("SELECT AVG(r.score) FROM Rating r WHERE r.rated.id = :userId AND r.ratedRole = :role")
    Double averageScore(@Param("userId") Long userId, @Param("role") RatedRole role);

    // US10 - Escenario alternativo: cantidad total de calificaciones recibidas en ese rol
    long countByRatedIdAndRatedRole(Long ratedId, RatedRole ratedRole);
}
