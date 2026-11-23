package com.rutau.agent;

import com.rutau.agent.tools.DemandAgentTools;
import com.rutau.agent.tools.DriverAgentTools;
import com.rutau.agent.tools.TripAgentTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class AgentConfig {

    // Memoria: últimos 20 mensajes de cada conversación, guardados en PostgreSQL (no se pierden al reiniciar)
    @Bean
    ChatMemory chatMemory(JdbcChatMemoryRepository repository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(20)
                .build();
    }

    @Bean
    ChatClient chatClient(ChatClient.Builder builder,
                          ChatMemory chatMemory,
                          TripAgentTools tripTools,
                          DemandAgentTools demandTools,
                          DriverAgentTools driverTools,
                          ObjectProvider<ToolCallbackProvider> mcpTools,
                          @Value("classpath:prompts/system-prompt.md") Resource systemPrompt) {
        // Tools locales (@Tool) + las del servidor MCP, si está activo (MCP_ENABLED=true)
        List<Object> tools = new ArrayList<>(List.of(tripTools, demandTools, driverTools));
        mcpTools.ifAvailable(tools::add);

        return builder
                .defaultSystem(systemPrompt)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultTools(tools.toArray())
                .build();
    }
}