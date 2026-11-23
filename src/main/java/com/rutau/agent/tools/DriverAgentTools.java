package com.rutau.agent.tools;

import com.rutau.agent.dto.ActionResult;
import com.rutau.agent.dto.PendingRequest;
import com.rutau.dto.request.TripRequestDTO;
import com.rutau.dto.response.SeatRequestResponseDTO;
import com.rutau.dto.response.TripResponseDTO;
import com.rutau.exception.BusinessRuleException;
import com.rutau.model.RequestStatus;
import com.rutau.model.TripStatus;
import com.rutau.service.SeatRequestService;
import com.rutau.service.TripService;
import com.rutau.util.Messages;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.rutau.model.Vehicle;
import com.rutau.repository.VehicleRepository;
import com.rutau.security.CurrentUser;
import com.rutau.agent.AgentActionTracker;

// US24 - El conductor publica un viaje o responde una solicitud con un comando en lenguaje natural.
// Las tools reutilizan TripService y SeatRequestService: las reglas de negocio (vehículo, capacidad,
// bloqueo de asientos, notificaciones) siguen en los servicios.
@Component
@RequiredArgsConstructor
public class DriverAgentTools {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String PUBLISH = "PUBLISH_TRIP";

    private final TripService tripService;
    private final SeatRequestService seatRequestService;
    private final Messages messages;
    private final VehicleRepository vehicleRepository;
    private final CurrentUser currentUser;
    private final AgentActionTracker tracker;

    // ==================== Publicar un viaje ====================

    @Tool(description = """
            Inicia la publicación de un viaje como conductor.
            NO lo publica: devuelve CONFIRMATION_REQUIRED con el resumen para que el usuario confirme,
            o MISSING_DATA con los datos que faltan. Llámala aunque falten datos.
            """)
    public ActionResult requestPublishTrip(
            @ToolParam(description = "Lugar de salida", required = false) String origin,
            @ToolParam(description = "Destino", required = false) String destination,
            @ToolParam(description = "Distrito de salida; si no lo dice, el distrito del origen", required = false) String zone,
            @ToolParam(description = "Fecha de salida AAAA-MM-DD", required = false) LocalDate date,
            @ToolParam(description = "Hora de salida HH:mm", required = false) LocalTime time,
            @ToolParam(description = "Asientos que ofrece", required = false) Integer seats,
            @ToolParam(description = "Precio por asiento en soles", required = false) BigDecimal pricePerSeat) {
        return tracker.record(publish(origin, destination, zone, date, time, seats, pricePerSeat, false));
    }

    @Tool(description = """
            Publica el viaje cuando el usuario YA confirmó (dijo sí, confirmo, dale).
            Es la única forma de publicar. Usa exactamente los mismos datos que confirmó. Devuelve DONE si quedó publicado.
            """)
    public ActionResult confirmPublishTrip(
            @ToolParam(description = "Lugar de salida") String origin,
            @ToolParam(description = "Destino") String destination,
            @ToolParam(description = "Distrito de salida") String zone,
            @ToolParam(description = "Fecha de salida AAAA-MM-DD") LocalDate date,
            @ToolParam(description = "Hora de salida HH:mm") LocalTime time,
            @ToolParam(description = "Asientos que ofrece") Integer seats,
            @ToolParam(description = "Precio por asiento en soles") BigDecimal pricePerSeat) {
        return tracker.record(publish(origin, destination, zone, date, time, seats, pricePerSeat, true));
    }

    private ActionResult publish(String origin, String destination, String zone, LocalDate date, LocalTime time,
                                 Integer seats, BigDecimal price, boolean confirmed) {
        requireStudent();

        // US06 - Sin vehículo registrado no se puede publicar (se avisa antes de pedir confirmación)
        Vehicle vehicle = vehicleRepository.findByOwnerId(currentUser.get().getId()).orElse(null);
        if (vehicle == null) {
            return new ActionResult(PUBLISH, "ERROR",
                    "No tienes un vehículo registrado. Regístralo primero en «Mi vehículo».", null);
        }

        // US24 - Escenario de error: falta información obligatoria
        List<String> missing = new ArrayList<>();
        if (isBlank(origin)) missing.add("origen");
        if (isBlank(destination)) missing.add("destino");
        if (date == null) missing.add("fecha");
        if (time == null) missing.add("hora");
        if (seats == null) missing.add("asientos");
        if (price == null) missing.add("precio por asiento");
        if (!missing.isEmpty()) {
            return new ActionResult(PUBLISH, "MISSING_DATA", "Falta: " + String.join(", ", missing), null);
        }

        String tripZone = isBlank(zone) ? origin.trim() : zone.trim();
        LocalDateTime departure = LocalDateTime.of(date, time);
        if (!departure.isAfter(LocalDateTime.now())) {
            return new ActionResult(PUBLISH, "ERROR", "La fecha y hora de salida deben ser futuras", null);
        }
        if (seats < 1 || price.signum() < 0) {
            return new ActionResult(PUBLISH, "ERROR", "Los asientos deben ser al menos 1 y el precio no puede ser negativo", null);
        }

        // US06 - No se pueden ofrecer más asientos que la capacidad del vehículo
        if (seats > vehicle.getCapacity()) {
            return new ActionResult(PUBLISH, "ERROR",
                    "Tu vehículo tiene capacidad para " + vehicle.getCapacity() + " asiento(s)", null);
        }

        String summary = "%s → %s (zona %s), %s, %d asiento(s), S/ %s por asiento".formatted(
                origin.trim(), destination.trim(), tripZone, departure.format(FMT), seats, price);

        if (!confirmed) {
            return new ActionResult(PUBLISH, "CONFIRMATION_REQUIRED", summary, null);
        }
        try {
            TripResponseDTO trip = tripService.publish(new TripRequestDTO(
                    origin.trim(), destination.trim(), tripZone, departure, seats, price, List.of()));
            return new ActionResult(PUBLISH, "DONE", summary, trip.id());
        } catch (RuntimeException ex) {
            return error(PUBLISH, ex);   // ej. sin vehículo o más asientos que la capacidad (US06)
        }
    }

    // ==================== Responder solicitudes ====================

    @Tool(description = """
            Lista las solicitudes pendientes de los viajes activos del conductor autenticado.
            Úsala para mostrar opciones cuando haya varias coincidencias o el usuario pregunte qué solicitudes tiene.
            """)
    public List<PendingRequest> myPendingRequests() {
        requireStudent();
        return findPending(null, null);
    }

    @Tool(description = """
            Inicia la respuesta (aceptar o rechazar) a la solicitud de un pasajero en uno de mis viajes.
            NO la ejecuta: devuelve CONFIRMATION_REQUIRED con el resumen, AMBIGUOUS si varias coinciden,
            NOT_FOUND si ninguna coincide o MISSING_DATA si falta el pasajero.
            """)
    public ActionResult requestRespondToRequest(
            @ToolParam(description = "Nombre o parte del nombre del pasajero", required = false) String passengerName,
            @ToolParam(description = "ACCEPT para aceptar o REJECT para rechazar") String decision,
            @ToolParam(description = "Fecha del viaje AAAA-MM-DD, solo si el usuario la indica", required = false) LocalDate tripDate) {
        return tracker.record(respond(passengerName, decision, tripDate, false));
    }

    @Tool(description = """
            Acepta o rechaza la solicitud cuando el usuario YA confirmó (dijo sí, confirmo, dale).
            Es la única forma de responder una solicitud. Usa los mismos datos que confirmó. Devuelve DONE si se ejecutó.
            """)
    public ActionResult confirmRespondToRequest(
            @ToolParam(description = "Nombre o parte del nombre del pasajero") String passengerName,
            @ToolParam(description = "ACCEPT para aceptar o REJECT para rechazar") String decision,
            @ToolParam(description = "Fecha del viaje AAAA-MM-DD, solo si se indicó", required = false) LocalDate tripDate) {
        return tracker.record(respond(passengerName, decision, tripDate, true));
    }

    private ActionResult respond(String passengerName, String decision, LocalDate tripDate, boolean confirmed) {
        requireStudent();
        boolean accept = !"REJECT".equalsIgnoreCase(decision == null ? "" : decision.trim());
        String type = accept ? "ACCEPT_REQUEST" : "REJECT_REQUEST";

        if (isBlank(passengerName)) {
            return new ActionResult(type, "MISSING_DATA", "Falta el nombre del pasajero", null);
        }
        List<PendingRequest> matches = findPending(passengerName, tripDate);
        if (matches.isEmpty()) {
            return new ActionResult(type, "NOT_FOUND", "No hay solicitudes pendientes de " + passengerName, null);
        }
        if (matches.size() > 1) {
            return new ActionResult(type, "AMBIGUOUS", "Hay " + matches.size() + " solicitudes que coinciden", null);
        }

        PendingRequest r = matches.get(0);
        String summary = "%s la solicitud de %s para el viaje %s del %s".formatted(
                accept ? "Aceptar" : "Rechazar", r.passengerName(), r.route(), r.departure());

        if (!confirmed) {
            return new ActionResult(type, "CONFIRMATION_REQUIRED", summary, null);
        }
        try {
            if (accept) {
                seatRequestService.accept(r.requestId());   // US04/US05: descuenta el asiento con bloqueo
            } else {
                seatRequestService.reject(r.requestId());
            }
            return new ActionResult(type, "DONE", summary, r.requestId());
        } catch (RuntimeException ex) {
            return error(type, ex);   // ej. el viaje ya está completo
        }
    }

    // El conductor no conoce ids: el servidor busca la solicitud por el nombre del pasajero (y la fecha, si la dijo)
    private List<PendingRequest> findPending(String passengerName, LocalDate tripDate) {
        String wanted = isBlank(passengerName) ? null : normalize(passengerName);
        return tripService.getMyTrips().stream()
                .filter(t -> t.status() == TripStatus.SCHEDULED || t.status() == TripStatus.FULL)
                .filter(t -> tripDate == null || t.departureTime().toLocalDate().equals(tripDate))
                .flatMap(t -> seatRequestService.getRequestsForMyTrip(t.id()).stream())
                .filter(r -> r.status() == RequestStatus.PENDING)
                .filter(r -> wanted == null || normalize(r.passengerName()).contains(wanted))
                .map(this::toPending)
                .toList();
    }

    private PendingRequest toPending(SeatRequestResponseDTO r) {
        return new PendingRequest(r.id(), r.passengerName(), r.origin() + " → " + r.destination(),
                r.departureTime().format(FMT), r.pickupPoint() != null ? r.pickupPoint() : "origen");
    }

    // Solo un estudiante (rol USER) publica viajes o responde solicitudes; el administrador no.
    private void requireStudent() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isStudent = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER"));
        if (!isStudent) {
            throw new AccessDeniedException("Solo un estudiante puede publicar viajes o responder solicitudes");
        }
    }

    // Las excepciones de los servicios traen una clave de i18n: se traduce para que el modelo la explique
    private ActionResult error(String type, RuntimeException ex) {
        Object[] args = ex instanceof BusinessRuleException b ? b.getArgs() : new Object[0];
        return new ActionResult(type, "ERROR", messages.get(ex.getMessage(), args), null);
    }

    private static String normalize(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase().trim();
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}