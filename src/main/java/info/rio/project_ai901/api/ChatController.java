package info.rio.project_ai901.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import info.rio.project_ai901.aiTools.UserContext;
import info.rio.project_ai901.services.ai.AgentService;


@RestController
@RequestMapping("/chat")
public class ChatController {

    private final AgentService agentService;

    public ChatController(AgentService agentService) {
        this.agentService = agentService;
    }

    public record ChatRequest(String mensaje) {}
    public record ChatResponse(String respuesta) {}

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request, @RequestAttribute("userContext") UserContext ctx) {
        System.out.println("REQUEST=" + request);
        System.out.println("MENSAJE=" + (request == null ? "request null" : request.mensaje()));
        return new ChatResponse(agentService.responseInventory(request.mensaje(), ctx));
    }
    @PostMapping("/public")
    public ChatResponse chatAssistant(@RequestBody ChatRequest request) {
        return new ChatResponse(agentService.responseAssistant(request.mensaje()));
    }
}