package com.rutau.controller;

import com.rutau.dto.request.ReportCreateDTO;
import com.rutau.dto.response.ReportResponseDTO;
import com.rutau.service.ModerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "10. Reportes de usuarios", description = "Reportar a un usuario o un viaje (US19)")
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ModerationService moderationService;

    // US19 - Cualquier estudiante puede reportar un viaje o a otro usuario
    @Operation(summary = "Reportar a un usuario o un viaje (US19)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReportResponseDTO create(@Valid @RequestBody ReportCreateDTO dto) {
        return moderationService.create(dto);
    }
}
