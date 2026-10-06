package com.rutau.controller;

import com.rutau.dto.response.DemandReportResponseDTO;
import com.rutau.service.AdminReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

// Solo el ADMINISTRADOR puede usar /api/admin/** (ver SecurityConfig)
@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final AdminReportService adminReportService;

    // US17 - /api/admin/reports/demand?from=2026-11-01&to=2026-11-30
    @GetMapping("/demand")
    public DemandReportResponseDTO demand(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return adminReportService.demandReport(from, to);
    }
}
