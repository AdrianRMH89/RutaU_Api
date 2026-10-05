package com.rutau.controller;

import com.rutau.dto.request.SeatRequestCreateDTO;
import com.rutau.dto.response.SeatRequestResponseDTO;
import com.rutau.service.SeatRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SeatRequestResponseDTO request(@Valid @RequestBody SeatRequestCreateDTO dto) {
        return seatRequestService.request(dto);
    }

    @GetMapping("/me")
    public List<SeatRequestResponseDTO> myRequests() {
        return seatRequestService.getMyRequests();
    }
}