package com.rutau.controller;

import com.rutau.dto.request.SeatRequestCreateDTO;
import com.rutau.dto.response.SeatRequestResponseDTO;
import com.rutau.service.SeatRequestService;
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
@RequestMapping("/api/seat-requests")
@RequiredArgsConstructor
public class SeatRequestController {

    private final SeatRequestService seatRequestService;

    // US03 - Solicitar asiento (pasajero)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SeatRequestResponseDTO request(@Valid @RequestBody SeatRequestCreateDTO dto) {
        return seatRequestService.request(dto);
    }

    // US03 - Mis solicitudes (pasajero)
    @GetMapping("/me")
    public List<SeatRequestResponseDTO> myRequests() {
        return seatRequestService.getMyRequests();
    }

    // US04 - Solicitudes recibidas en un viaje (conductor)
    @GetMapping("/trip/{tripId}")
    public List<SeatRequestResponseDTO> requestsForTrip(@PathVariable Long tripId) {
        return seatRequestService.getRequestsForMyTrip(tripId);
    }

    // US04 - Aceptar solicitud (conductor)
    @PatchMapping("/{id}/accept")
    public SeatRequestResponseDTO accept(@PathVariable Long id) {
        return seatRequestService.accept(id);
    }

    // US04 - Rechazar solicitud (conductor)
    @PatchMapping("/{id}/reject")
    public SeatRequestResponseDTO reject(@PathVariable Long id) {
        return seatRequestService.reject(id);
    }
}