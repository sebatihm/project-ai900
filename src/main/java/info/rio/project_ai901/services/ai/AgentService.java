package info.rio.project_ai901.services.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.azure.ai.agents.ResponsesClient;
import com.azure.ai.agents.models.AgentReference;
import com.azure.ai.agents.models.AzureCreateResponseOptions;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseInputItem;
import com.openai.models.responses.ResponseOutputItem;
import com.openai.models.responses.ResponseOutputMessage;

import info.rio.project_ai901.aiTools.UserContext;
import info.rio.project_ai901.config.ToolRegistry;

@Service
public class AgentService {

    private static final int MAX_STEPS_INTERNAL = 5;
    private static final int MAX_STEPS_PUBLIC = 3;

    private final ResponsesClient responses;
    private final ToolRegistry registry;
    private final AgentReference inventoryAgent;
    private final AgentReference assistantAgent;

    public AgentService(ResponsesClient responses,
                        ToolRegistry registry,
                        @Qualifier("agente-inventario") AgentReference inventoryAgent,
                        @Qualifier("asistente-rio-claro") AgentReference assistantAgent) {
        this.responses = responses;
        this.registry = registry;
        this.inventoryAgent = inventoryAgent;
        this.assistantAgent = assistantAgent;
    }

    // Usuarios autenticados: el contexto sale del JWT
    public String responseInventory(String texto, UserContext ctx) {
        return run(inventoryAgent, texto, ctx, MAX_STEPS_INTERNAL);
    }

    // Clientes sin login: contexto anónimo, menos pasos
    public String responseAssistant(String texto) {
        return run(assistantAgent, texto, UserContext.anonymous(), MAX_STEPS_PUBLIC);
    }

    private String run(AgentReference agent, String texto, UserContext ctx, int maxSteps) {
        ResponseCreateParams.Builder params = ResponseCreateParams.builder().input(texto);

        for (int i = 0; i < maxSteps; i++) {
            Response r = responses.createAzureResponse(
                    new AzureCreateResponseOptions().setAgentReference(agent), params);

            List<ResponseInputItem> outputs = new ArrayList<>();
            for (ResponseOutputItem item : r.output()) {
                if (item.isFunctionCall()) {
                    var call = item.asFunctionCall();
                    // el contexto lo pone el backend; el modelo solo manda los argumentos
                    String result = registry.execute(call.name(), call.arguments(), ctx);
                    outputs.add(ResponseInputItem.ofFunctionCallOutput(
                            ResponseInputItem.FunctionCallOutput.builder()
                                    .callId(call.callId())
                                    .output(result)
                                    .build()));
                }
            }

            if (outputs.isEmpty()) {
                return extraerTexto(r);          // respuesta final en texto
            }

            // siguiente vuelta: devolvemos los resultados encadenados a la respuesta anterior
            params = ResponseCreateParams.builder()
                    .previousResponseId(r.id())
                    .inputOfResponse(outputs);
        }
        return "No pude completar la consulta. Intenta reformular la pregunta.";
    }

    private String extraerTexto(Response r) {
        return r.output().stream()
                .filter(ResponseOutputItem::isMessage)
                .flatMap(i -> i.asMessage().content().stream())
                .filter(ResponseOutputMessage.Content::isOutputText)
                .map(c -> c.asOutputText().text())
                .collect(Collectors.joining("\n"));
    }
}