package com.rutau.agent;

import com.rutau.agent.dto.ChatResponse;
import com.rutau.model.User;
import com.rutau.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.Locale;
import com.rutau.agent.dto.ActionResult;
import lombok.extern.slf4j.Slf4j;
import com.rutau.model.Vehicle;
import com.rutau.repository.VehicleRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class AgentService {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final CurrentUser currentUser;
    private final AgentActionTracker tracker;
    private final VehicleRepository vehicleRepository;
    // username sale del JWT: cada usuario tiene su propia conversación y nadie lee la de otro
    // username sale del JWT: cada usuario tiene su propia conversación y nadie lee la de otro
    public ChatResponse chat(String username, String message) {
        User user = currentUser.get();
        LocalDate today = LocalDate.now(LIMA);
        String todayText = today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.of("es", "PE")) + " " + today;

        // US06 - El agente sabe desde el inicio si el usuario tiene vehículo (y su capacidad)
        String vehicleText = vehicleRepository.findByOwnerId(user.getId())
                .map(v -> "sí, " + v.getBrand() + " " + v.getModel() + " con capacidad de " + v.getCapacity() + " asiento(s)")
                .orElse("NO tiene vehículo registrado");
        // El agente responde con un objeto (salida estructurada), no con texto libre
        ChatResponse response = chatClient.prompt()
                .system(s -> s.param("today", todayText)
                        .param("name", user.getFullName())
                        .param("role", user.getRole().name())
                        .param("vehicle", vehicleText))
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, username))
                .call()
                .entity(ChatResponse.class);

        // Control: action es lo que de verdad hizo la tool, no lo que escribió el modelo
        ActionResult realAction = tracker.getLastAction();
        String text = response.message();
        boolean claimsDone = response.action() != null && "DONE".equals(response.action().status());
        if (realAction == null && claimsDone) {
            // El modelo dijo que ejecutó una acción sin llamar a la tool: no se hizo nada
            log.warn("El modelo reportó DONE sin ejecutar ninguna tool");
            text = "No pude completar la acción. Repite tu pedido, por favor.";
        }
        return new ChatResponse(text, response.trips(), realAction);
    }

    // Botón "Nueva conversación": borra la memoria de este usuario
    public void reset(String username) {
        chatMemory.clear(username);
    }
}