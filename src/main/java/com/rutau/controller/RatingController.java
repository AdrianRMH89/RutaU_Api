package com.rutau.controller;

import com.rutau.dto.request.RatingRequestDTO;
import com.rutau.dto.response.RatingHistoryResponseDTO;
import com.rutau.dto.response.RatingResponseDTO;
import com.rutau.model.RatedRole;
import com.rutau.service.RatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "06. Calificaciones", description = "Calificar viajes e historial de calificaciones (US09, US16)")
@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    // US09 - Calificar a la otra parte de un viaje realizado (1 a 5 estrellas)
    @Operation(summary = "Calificar a la otra parte de un viaje realizado (US09)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RatingResponseDTO rate(@Valid @RequestBody RatingRequestDTO dto) {
        return ratingService.rate(dto);
    }

    // US16 - Mis calificaciones recibidas. Filtro opcional: ?role=DRIVER o ?role=PASSENGER
    @Operation(summary = "Ver las calificaciones que recibí, filtrables por rol (US16)")
    @GetMapping("/me")
    public RatingHistoryResponseDTO myRatings(@RequestParam(required = false) RatedRole role) {
        return ratingService.getMyRatings(role);
    }
}
