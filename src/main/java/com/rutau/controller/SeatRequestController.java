package com.rutau.controller;

import com.rutau.dto.request.CancelRequestDTO;
import com.rutau.dto.request.SeatRequestCreateDTO;
import com.rutau.dto.response.SeatRequestResponseDTO;
import com.rutau.service.SeatRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "05. Solicitudes de asiento", description = "Solicitar, aceptar, rechazar y cancelar asientos (US03-US05, US08, US11)")
@RestController
@RequestMapping("/api/seat-requests")
@RequiredArgsConstructor
public class SeatRequestController {

    private final SeatRequestService seatRequestService;

    // US03 - Solicitar asiento (pasajero)
    @Operation(summary = "Solicitar un asiento (US03, US11)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SeatRequestResponseDTO request(@Valid @RequestBody SeatRequestCreateDTO dto) {
        return seatRequestService.request(dto);
    }

    // US03 - Mis solicitudes (pasajero)
    @Operation(summary = "Ver mis solicitudes como pasajero")
    @GetMapping("/me")
    public List<SeatRequestResponseDTO> myRequests() {
        return seatRequestService.getMyRequests();
    }

    // US04 - Solicitudes recibidas en un viaje (conductor)
    @Operation(summary = "Ver las solicitudes de uno de mis viajes (conductor)")
    @GetMapping("/trip/{tripId}")
    public List<SeatRequestResponseDTO> requestsForTrip(@PathVariable Long tripId) {
        return seatRequestService.getRequestsForMyTrip(tripId);
    }

    // US04 - Aceptar solicitud (conductor)
    @Operation(summary = "Aceptar una solicitud (US04, US05, US20)")
    @PatchMapping("/{id}/accept")
    public SeatRequestResponseDTO accept(@PathVariable Long id) {
        return seatRequestService.accept(id);
    }

    // US04 - Rechazar solicitud (conductor)
    @Operation(summary = "Rechazar una solicitud (US04, US20)")
    @PatchMapping("/{id}/reject")
    public SeatRequestResponseDTO reject(@PathVariable Long id) {
        return seatRequestService.reject(id);
    }

    // US05 - El pasajero cancela su solicitud o reserva (libera el asiento si estaba aceptada)
    @Operation(summary = "Cancelar mi asiento como pasajero (US05, US08)")
    @PatchMapping("/{id}/cancel")
    public SeatRequestResponseDTO cancel(@PathVariable Long id,
                                         @Valid @RequestBody CancelRequestDTO dto) {
        return seatRequestService.cancelByPassenger(id, dto);
    }
}
