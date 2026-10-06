package com.rutau.controller;

import com.rutau.dto.request.ReportCreateDTO;
import com.rutau.dto.response.ReportResponseDTO;
import com.rutau.service.ModerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ModerationService moderationService;

    // US19 - Cualquier estudiante puede reportar un viaje o a otro usuario
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReportResponseDTO create(@Valid @RequestBody ReportCreateDTO dto) {
        return moderationService.create(dto);
    }
}
