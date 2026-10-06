package com.rutau.controller;

import com.rutau.dto.request.ModerationRequestDTO;
import com.rutau.dto.response.ReportResponseDTO;
import com.rutau.dto.response.UserModerationResponseDTO;
import com.rutau.model.ReportStatus;
import com.rutau.service.ModerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// US19 - Panel de moderación (solo ADMIN, ver SecurityConfig)
@Tag(name = "12. Admin - Moderación", description = "Panel de moderación de reportes, solo ADMIN (US19)")
@RestController
@RequestMapping("/api/admin/moderation")
@RequiredArgsConstructor
public class AdminModerationController {

    private final ModerationService moderationService;

    // Lista de reportes; filtro opcional ?status=PENDING
    @Operation(summary = "Listar reportes, filtrables por estado (US19)")
    @GetMapping("/reports")
    public List<ReportResponseDTO> list(@RequestParam(required = false) ReportStatus status) {
        return moderationService.list(status);
    }

    @Operation(summary = "Ver el detalle de un reporte (US19)")
    @GetMapping("/reports/{id}")
    public ReportResponseDTO detail(@PathVariable Long id) {
        return moderationService.getById(id);
    }

    // Tomar una acción: { "action": "WARNING" | "SUSPENSION" | "DISMISS", "notes": "..." }
    @Operation(summary = "Tomar una acción sobre un reporte (US19)")
    @PatchMapping("/reports/{id}")
    public ReportResponseDTO moderate(@PathVariable Long id, @Valid @RequestBody ModerationRequestDTO dto) {
        return moderationService.moderate(id, dto);
    }

    // Historial de reportes de un usuario (muestra si está "En observación")
    @Operation(summary = "Ver el historial de reportes de un usuario (US19)")
    @GetMapping("/users/{id}")
    public UserModerationResponseDTO userHistory(@PathVariable Long id) {
        return moderationService.userHistory(id);
    }
}
