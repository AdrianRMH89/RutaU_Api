package com.rutau.controller;

import com.rutau.dto.request.CancelRequestDTO;
import com.rutau.dto.request.CompleteTripRequestDTO;
import com.rutau.dto.request.TripRequestDTO;
import com.rutau.dto.response.TripCompletionResponseDTO;
import com.rutau.dto.response.TripResponseDTO;
import com.rutau.dto.response.TripSearchResponseDTO;
import com.rutau.dto.response.TripStopResponseDTO;
import com.rutau.service.TripSearchService;
import com.rutau.service.TripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

@Tag(name = "04. Viajes", description = "Publicar, buscar, completar y cancelar viajes (US02, US05-US08, US11, US13, US14)")
@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;
    private final TripSearchService tripSearchService;

    @Operation(summary = "Publicar un viaje, con puntos intermedios opcionales (US02, US11)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TripResponseDTO publish(@Valid @RequestBody TripRequestDTO dto) {
        return tripService.publish(dto);
    }

    // US05 - Viajes activos (los viajes completos no aparecen)
    @Operation(summary = "Listar viajes activos con asientos libres (US05)")
    @GetMapping
    public List<TripResponseDTO> activeTrips() {
        return tripService.getActiveTrips();
    }

    // US13 - Buscar viajes: /api/trips/search?origin=Surco&destination=Monterrico&from=07:00&to=08:00
    // US14 - Filtros opcionales: &zone=Surco&maxPrice=6
    @Operation(summary = "Buscar viajes por origen, destino, horario, zona y precio (US13, US14)")
    @GetMapping("/search")
    public TripSearchResponseDTO search(@RequestParam(required = false) String origin,
                                        @RequestParam(required = false) String destination,
                                        @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime from,
                                        @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime to,
                                        @RequestParam(required = false) String zone,
                                        @RequestParam(required = false) BigDecimal maxPrice) {
        return tripSearchService.search(origin, destination, from, to, zone, maxPrice);
    }

    @Operation(summary = "Listar mis viajes como conductor")
    @GetMapping("/me")
    public List<TripResponseDTO> myTrips() {
        return tripService.getMyTrips();
    }

    @Operation(summary = "Ver un viaje")
    @GetMapping("/{id}")
    public TripResponseDTO getById(@PathVariable Long id) {
        return tripService.getById(id);
    }

    // US11 - Puntos intermedios de recojo o bajada de un viaje
    @Operation(summary = "Ver los puntos intermedios de un viaje (US11)")
    @GetMapping("/{id}/stops")
    public List<TripStopResponseDTO> stops(@PathVariable Long id) {
        return tripService.getStops(id);
    }

    // US07 - Marcar un viaje como realizado (conductor). El body es opcional.
    @Operation(summary = "Marcar un viaje como realizado (US07)")
    @PatchMapping("/{id}/complete")
    public TripCompletionResponseDTO complete(@PathVariable Long id,
                                              @RequestBody(required = false) CompleteTripRequestDTO dto) {
        return tripService.complete(id, dto);
    }

    // US08 - El conductor cancela un viaje (se notifica a los pasajeros)
    @Operation(summary = "Cancelar un viaje y notificar a los pasajeros (US08)")
    @PatchMapping("/{id}/cancel")
    public TripResponseDTO cancel(@PathVariable Long id, @Valid @RequestBody CancelRequestDTO dto) {
        return tripService.cancel(id, dto);
    }
}
