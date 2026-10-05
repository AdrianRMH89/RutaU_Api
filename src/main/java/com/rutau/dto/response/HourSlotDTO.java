package com.rutau.dto.response;

// US12 - Una franja horaria del panel: oferta (viajes/asientos) frente a demanda (solicitudes)
public record HourSlotDTO(
        Integer hour,             // 7 -> franja de 07:00 a 07:59
        String label,             // "07:00 - 07:59"
        Long tripsOffered,
        Long seatsOffered,
        Long seatRequests,
        Boolean opportunity       // US12 - Escenario alternativo: alta demanda y baja oferta
) {}
