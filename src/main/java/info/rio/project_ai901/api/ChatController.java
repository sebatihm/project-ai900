package info.rio.project_ai901.api;

import info.rio.project_ai901.aiTools.UserContext;
import info.rio.project_ai901.services.ai.AgentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final AgentService agentService;

    public ChatController(AgentService agentService) {
        this.agentService = agentService;
    }

    public record ChatRequest(String mensaje, String conversationId) {}
    public record ChatResponse(String respuesta, String conversationId) {}

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request,
                             @RequestAttribute("userContext") UserContext ctx) {
        validate(request);
        AgentService.AgentReply reply =
                agentService.responseInventory(request.mensaje(), ctx, request.conversationId());
        return new ChatResponse(reply.text(), reply.conversationId());
    }

    @PostMapping("/public")
    public ChatResponse chatPublic(@RequestBody ChatRequest request) {
        validate(request);
        AgentService.AgentReply reply =
                agentService.responseAssistant(request.mensaje(), request.conversationId());
        return new ChatResponse(reply.text(), reply.conversationId());
    }

    private void validate(ChatRequest request) {
        if (request == null || request.mensaje() == null || request.mensaje().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El mensaje es obligatorio");
        }
    }
}