package com.rutau.repository;

import com.rutau.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    Optional<Vehicle> findByOwnerId(Long ownerId);

    boolean existsByPlate(String plate);
}