package com.rutau.agent.dto;

// Resultado de una acción del agente (publicar viaje o responder una solicitud).
public record ActionResult(
        String type,
        String status,
        String detail,
        Long id) {}