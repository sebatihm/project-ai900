package info.rio.project_ai901.api;

import info.rio.project_ai901.aiTools.InputChannel;
import info.rio.project_ai901.aiTools.UserContext;
import info.rio.project_ai901.services.ai.AgentService;
import info.rio.project_ai901.services.ai.SpeechTranscriptionService;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final AgentService agentService;
    private final SpeechTranscriptionService speechService;

    public ChatController(AgentService agentService, SpeechTranscriptionService speechService) {
        this.agentService = agentService;
        this.speechService = speechService;
    }

    private static final String VOICE_PREFIX = "[Mensaje transcrito de voz] ";
    public record ChatRequest(String mensaje, String conversationId) {}
    public record ChatResponse(String respuesta, String conversationId, String transcripcion) {}
    private record Input(String text, boolean voice, String transcription) {}

   // ===== Agente interno (requiere JWT) =====

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ChatResponse chat(@RequestBody ChatRequest request,
                             @RequestAttribute("userContext") UserContext ctx) {
        Input in = textInput(request == null ? null : request.mensaje());
        return answerInternal(in, request.conversationId(), ctx);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ChatResponse chatMultipart(
            @RequestParam(value = "audio", required = false) MultipartFile audio,
            @RequestParam(value = "mensaje", required = false) String mensaje,
            @RequestParam(value = "conversationId", required = false) String conversationId,
            @RequestAttribute("userContext") UserContext ctx) {
        return answerInternal(resolveInput(audio, mensaje), conversationId, ctx);
    }

    // ===== Agente publico (sin JWT) =====

    @PostMapping(value = "/public", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ChatResponse chatPublic(@RequestBody ChatRequest request) {
        Input in = textInput(request == null ? null : request.mensaje());
        return answerPublic(in, request.conversationId());
    }

    @PostMapping(value = "/public", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ChatResponse chatPublicMultipart(
            @RequestParam(value = "audio", required = false) MultipartFile audio,
            @RequestParam(value = "mensaje", required = false) String mensaje,
            @RequestParam(value = "conversationId", required = false) String conversationId) {
        return answerPublic(resolveInput(audio, mensaje), conversationId);
    }

    // ===== Logica compartida =====

    private ChatResponse answerInternal(Input in, String conversationId, UserContext ctx) {
        UserContext effective = in.voice() ? ctx.withChannel(InputChannel.VOICE) : ctx;
        AgentService.AgentReply r = agentService.responseInventory(in.text(), effective, conversationId);
        return new ChatResponse(r.text(), r.conversationId(), in.transcription());
    }

    private ChatResponse answerPublic(Input in, String conversationId) {
        UserContext ctx = in.voice()
                ? UserContext.anonymous().withChannel(InputChannel.VOICE)
                : UserContext.anonymous();
        AgentService.AgentReply r = agentService.responseAssistant(in.text(), ctx, conversationId);
        return new ChatResponse(r.text(), r.conversationId(), in.transcription());
    }

    private Input textInput(String mensaje) {
        if (mensaje == null || mensaje.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El mensaje es obligatorio");
        }
        return new Input(mensaje, false, null);
    }

    /** Multipart: o viene audio o viene texto, nunca ambos. */
    private Input resolveInput(MultipartFile audio, String mensaje) {
        boolean hasAudio = audio != null && !audio.isEmpty();
        boolean hasText = mensaje != null && !mensaje.isBlank();

        if (hasAudio && hasText) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Envia un audio o un mensaje de texto, no ambos.");
        }
        if (!hasAudio && !hasText) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debes enviar un mensaje de texto o un audio.");
        }
        if (hasText) {
            return new Input(mensaje, false, null);
        }

        SpeechTranscriptionService.Transcription t = speechService.transcribe(audio);
        // La marca avisa al agente de que el texto puede tener errores de transcripcion
        return new Input(VOICE_PREFIX + t.text(), true, t.text());
    }
}