package com.rutau.controller;

import com.rutau.dto.response.ScheduleComparisonResponseDTO;
import com.rutau.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    // US12 - Panel: horarios más ofertados frente a los más solicitados
    // Filtro opcional por zona: /api/stats/schedule-comparison?zone=Surco
    @GetMapping("/schedule-comparison")
    public ScheduleComparisonResponseDTO scheduleComparison(@RequestParam(required = false) String zone) {
        return statsService.compareSchedules(zone);
    }
}
