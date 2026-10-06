package com.rutau.controller;

import com.rutau.dto.response.TripHistoryResponseDTO;
import com.rutau.service.HistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "07. Historial", description = "Historial de viajes del usuario (US15)")
@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class HistoryController {

    private final HistoryService historyService;

    // US15 - "Mi historial": viajes como conductor, como pasajero y en curso
    @Operation(summary = "Ver mi historial de viajes (US15)")
    @GetMapping("/trips")
    public TripHistoryResponseDTO myTrips() {
        return historyService.getMyHistory();
    }
}
