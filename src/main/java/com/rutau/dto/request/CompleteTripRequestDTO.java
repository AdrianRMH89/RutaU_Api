package com.rutau.dto.request;

import java.util.List;

public record CompleteTripRequestDTO(

        // US07 - Escenario de error: confirmar aunque el viaje aún no llegue a su hora programada
        Boolean confirmEarly,

        // US07 - Escenario alternativo: ids de las solicitudes (pasajeros) que SÍ llegaron a su
        // punto acordado. Si se omite, el viaje se marca como completo para todos los aceptados.
        List<Long> completedRequestIds
) {}
