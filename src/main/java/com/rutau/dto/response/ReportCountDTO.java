package com.rutau.dto.response;

// US17 - Una fila del reporte: una ruta, un horario o un período con su cantidad de solicitudes
public record ReportCountDTO(
        String label,
        Long requests
) {}
