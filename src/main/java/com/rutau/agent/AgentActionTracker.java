package com.rutau.agent;

import com.rutau.agent.dto.ActionResult;
import lombok.Getter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import lombok.extern.slf4j.Slf4j;
// Guarda el resultado REAL de la última tool de acción de esta petición.
// @RequestScope: se crea uno nuevo por cada mensaje del chat, así no se mezclan usuarios.
@Component
@RequestScope
@Getter
@Slf4j
public class AgentActionTracker {

    private ActionResult lastAction;

    public ActionResult record(ActionResult result) {
        log.info("Tool de acción ejecutada: {} -> {}", result.type(), result.status());
        this.lastAction = result;
        return result;
    }
}