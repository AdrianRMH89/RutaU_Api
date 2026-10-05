package com.rutau.controller;

import com.rutau.dto.request.RatingRequestDTO;
import com.rutau.dto.response.RatingResponseDTO;
import com.rutau.service.RatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    // US09 - Calificar a la otra parte de un viaje realizado (1 a 5 estrellas)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RatingResponseDTO rate(@Valid @RequestBody RatingRequestDTO dto) {
        return ratingService.rate(dto);
    }
}
