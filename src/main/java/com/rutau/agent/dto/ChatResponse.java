package com.rutau.agent.dto;

import java.util.List;

public record ChatResponse(
        String message,
        List<TripSummary> trips,
        ActionResult action) {}