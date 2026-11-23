package com.rutau.agent;

import com.rutau.agent.dto.ChatRequest;
import com.rutau.agent.dto.ChatResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

// US21-US24 - Endpoint del agente de IA. Requiere token (anyRequest().authenticated()).
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;
    private final ObjectProvider<ToolCallbackProvider> mcpTools;

    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request, Authentication auth) {
        return agentService.chat(auth.getName(), request.message());
    }

    @DeleteMapping("/chat")
    public ResponseEntity<Void> reset(Authentication auth) {
        agentService.reset(auth.getName());
        return ResponseEntity.noContent().build();
    }

    // Semana 13 - Lista las herramientas que trajo el servidor MCP (vacía si MCP está apagado)
    @GetMapping("/mcp/tools")
    public List<String> mcpTools() {
        ToolCallbackProvider provider = mcpTools.getIfAvailable();
        if (provider == null) {
            return List.of();
        }
        ToolCallback[] callbacks = provider.getToolCallbacks();
        return Arrays.stream(callbacks)
                .map(tool -> tool.getToolDefinition().name())
                .sorted()
                .toList();
    }
}