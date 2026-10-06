package com.rutau.controller;

import com.rutau.dto.response.DemandReportResponseDTO;
import com.rutau.service.AdminReportService;
import com.rutau.service.ReportExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

// Solo el ADMINISTRADOR puede usar /api/admin/** (ver SecurityConfig)
@Tag(name = "11. Admin - Reportes de demanda", description = "Reportes y exportación a Excel/PDF, solo ADMIN (US17, US18)")
@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final AdminReportService adminReportService;
    private final ReportExportService reportExportService;

    // US17 - /api/admin/reports/demand?from=2026-11-01&to=2026-11-30
    @Operation(summary = "Reporte de rutas y horarios con mayor demanda (US17)")
    @GetMapping("/demand")
    public DemandReportResponseDTO demand(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return adminReportService.demandReport(from, to);
    }

    // US18 - Descargar el reporte: /api/admin/reports/demand/export?format=xlsx (o pdf)&from=...&to=...
    @Operation(summary = "Exportar el reporte de demanda a Excel o PDF (US18)")
    @GetMapping("/demand/export")
    public ResponseEntity<byte[]> export(
            @RequestParam String format,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        ReportExportService.ExportedFile file = reportExportService.exportDemandReport(format, from, to);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.fileName() + "\"")
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.content());
    }
}
