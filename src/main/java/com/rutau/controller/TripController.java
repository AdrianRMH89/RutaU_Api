package com.rutau.controller;

import com.rutau.dto.request.CancelRequestDTO;
import com.rutau.dto.request.CompleteTripRequestDTO;
import com.rutau.dto.request.TripRequestDTO;
import com.rutau.dto.response.TripCompletionResponseDTO;
import com.rutau.dto.response.TripResponseDTO;
import com.rutau.dto.response.TripStopResponseDTO;
import com.rutau.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TripResponseDTO publish(@Valid @RequestBody TripRequestDTO dto) {
        return tripService.publish(dto);
    }

    // US05 - Viajes activos (los viajes completos no aparecen)
    @GetMapping
    public List<TripResponseDTO> activeTrips() {
        return tripService.getActiveTrips();
    }

    @GetMapping("/me")
    public List<TripResponseDTO> myTrips() {
        return tripService.getMyTrips();
    }

    @GetMapping("/{id}")
    public TripResponseDTO getById(@PathVariable Long id) {
        return tripService.getById(id);
    }

    // US11 - Puntos intermedios de recojo o bajada de un viaje
    @GetMapping("/{id}/stops")
    public List<TripStopResponseDTO> stops(@PathVariable Long id) {
        return tripService.getStops(id);
    }

    // US07 - Marcar un viaje como realizado (conductor). El body es opcional.
    @PatchMapping("/{id}/complete")
    public TripCompletionResponseDTO complete(@PathVariable Long id,
                                              @RequestBody(required = false) CompleteTripRequestDTO dto) {
        return tripService.complete(id, dto);
    }

    // US08 - El conductor cancela un viaje (se notifica a los pasajeros)
    @PatchMapping("/{id}/cancel")
    public TripResponseDTO cancel(@PathVariable Long id, @Valid @RequestBody CancelRequestDTO dto) {
        return tripService.cancel(id, dto);
    }
}
