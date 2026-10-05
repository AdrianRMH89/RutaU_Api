package com.rutau.dto.response;

import java.util.List;

// US15 - "Mi historial": ambos roles separados + viajes en curso
public record TripHistoryResponseDTO(
        String message,
        List<TripHistoryItemDTO> asDriver,      // viajes que publiqué y ya terminaron
        List<TripHistoryItemDTO> asPassenger,   // viajes en los que fui pasajero y ya terminaron
        List<TripHistoryItemDTO> inProgress     // US15 - Escenario alternativo: viajes en curso
) {}
