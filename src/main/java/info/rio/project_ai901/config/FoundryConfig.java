package info.rio.project_ai901.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import com.azure.ai.agents.AgentsClient;
import com.azure.ai.agents.AgentsClientBuilder;
import com.azure.ai.agents.ResponsesClient;
import com.azure.ai.agents.models.AgentReference;
import com.azure.ai.agents.models.AgentVersionDetails;
import com.azure.ai.agents.models.PromptAgentDefinition;
import com.azure.ai.agents.models.Tool;
import com.azure.identity.DefaultAzureCredentialBuilder;

@Configuration
public class FoundryConfig {

    @Bean
    AgentsClientBuilder agentsBuilder(@Value("${foundry.project-endpoint}") String endpoint) {
        return new AgentsClientBuilder()
                .credential(new DefaultAzureCredentialBuilder().build())
                .endpoint(endpoint);
    }

    @Bean AgentsClient agentsClient(AgentsClientBuilder b) { return b.buildAgentsClient(); }
    @Bean ResponsesClient responsesClient(AgentsClientBuilder b) { return b.buildResponsesClient(); }

        @Bean("agente-inventario")
    AgentReference internalAgent(AgentsClient client, ToolRegistry registry,
                                 @Value("${foundry.deployment}") String deployment,
                                 @Value("${foundry.agent.publish:false}") boolean publish) {
        return agent(client, "agente-inventario", "prompts/agente-inventario.txt",
                deployment, publish, registry.definitions(false).stream()
                .map(tool -> (Tool) tool)
                .collect(Collectors.toList()));
    }

    @Bean("asistente-rio-claro")
    AgentReference publicAgent(AgentsClient client, ToolRegistry registry,
                               @Value("${foundry.deployment}") String deployment,
                               @Value("${foundry.agent.publish:false}") boolean publish) {
        return agent(client, "asistente-rio-claro", "prompts/asistente-rio-claro.txt",
                deployment, publish, registry.definitions(true)
                .stream()
                .map(tool -> (Tool) tool)
                .collect(Collectors.toList()));
    }

    private AgentReference agent(AgentsClient client, String name, String promptPath,
                                 String deployment, boolean publish, List<Tool> tools) {
        if (!publish) {
            return new AgentReference(name);       
        }
        PromptAgentDefinition definition = new PromptAgentDefinition(deployment)
                .setInstructions(readPrompt(promptPath))
                .setTools(tools);

        AgentVersionDetails created = client.createAgentVersion(name, definition);
        return new AgentReference(created.getName()).setVersion(created.getVersion());
    }

    private String readPrompt(String path) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el prompt: " + path, e);
        }
    }
}