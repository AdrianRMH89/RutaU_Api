package com.rutau.controller;

import com.rutau.dto.response.TripHistoryResponseDTO;
import com.rutau.service.HistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class HistoryController {

    private final HistoryService historyService;

    // US15 - "Mi historial": viajes como conductor, como pasajero y en curso
    @GetMapping("/trips")
    public TripHistoryResponseDTO myTrips() {
        return historyService.getMyHistory();
    }
}
