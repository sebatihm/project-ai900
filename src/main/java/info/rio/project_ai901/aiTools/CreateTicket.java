package info.rio.project_ai901.aiTools;

import info.rio.project_ai901.exceptions.ResourceNotFoundException;
import info.rio.project_ai901.models.enums.TicketChannel;
import info.rio.project_ai901.models.enums.TicketSentiment;
import info.rio.project_ai901.models.enums.TicketTopic;
import info.rio.project_ai901.services.ticket.TicketService;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class CreateTicket implements AgentTool {

    private final TicketService ticketService;
    private final JsonMapper jsonMapper;

    public CreateTicket(TicketService ticketService, JsonMapper jsonMapper) {
        this.ticketService = ticketService;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public String name() {
        return "crear_ticket";
    }

    @Override
    public String description() {
        return "Registra un ticket de soporte ligado a un pedido, a partir del codigo del pedido. "
             + "Si ya existe un ticket abierto del mismo pedido y tema, agrega el seguimiento a ese ticket. "
             + "Usala SOLO despues de resumirle el problema al usuario y de que el usuario haya aceptado "
             + "expresamente que se registre. Si no tiene el codigo del pedido, pidelo antes. "
             + "Nunca inventes datos ni codigos.";
    }

    @Override
    public Map<String, Object> properties() {
        return Map.of(
            "codigo_pedido", Map.of(
                "type", "string",
                "description", "Codigo del pedido tal como lo escribio el usuario."),
            "asunto", Map.of(
                "type", "string",
                "description", "Titulo breve del problema (maximo 200 caracteres)."),
            "tema", Map.of(
                "type", "string",
                "enum", Arrays.stream(TicketTopic.values()).map(Enum::name).toList(),
                "description", "Categoria interna del problema. Si ninguna encaja, usa OTHER. "
                 + "Nunca se la menciones al usuario."),
            "sentimiento", Map.of(
                "type", "string",
                "enum", Arrays.stream(TicketSentiment.values()).map(Enum::name).toList(),
                "description", "Tono del usuario en la conversacion. Opcional."),
            "resumen", Map.of(
                "type", "string",
                "description", "Resumen objetivo de la conversacion y del problema, sin inventar datos."),
            "requiere_revision_humana", Map.of(
                "type", "boolean",
                "description", "true si el usuario pidio hablar con una persona o esta muy molesto."),
            "confirmado_por_usuario", Map.of(
                "type", "boolean",
                "description", "true SOLO si el usuario acepto claramente que se registre el ticket.")
        );
    }

    @Override
    public List<String> required() {
        return List.of("codigo_pedido", "asunto", "tema", "resumen", "confirmado_por_usuario");
    }

    @Override
    public boolean isPublic() {
        return true; // la usan el agente publico y el interno
    }

    @Override
    public String execute(JsonNode args, UserContext ctx) {
        if (!args.path("confirmado_por_usuario").asBoolean(false)) {
            return "No se creo el ticket: primero resume el problema al usuario y pidele confirmacion expresa.";
        }

        String codigo = text(args, "codigo_pedido");
        String asunto = text(args, "asunto");
        String resumen = text(args, "resumen");
        if (codigo.isEmpty() || asunto.isEmpty() || resumen.isEmpty()) {
            return "Faltan datos: se requiere codigo de pedido, asunto y resumen.";
        }

        TicketTopic tema;
        try {
            tema = TicketTopic.valueOf(text(args, "tema").toUpperCase());
        } catch (IllegalArgumentException e) {
            return "Tema no valido. Usa uno de: " + Arrays.toString(TicketTopic.values());
        }

        TicketSentiment sentimiento = null;
        if (args.hasNonNull("sentimiento")) {
            try {
                sentimiento = TicketSentiment.valueOf(text(args, "sentimiento").toUpperCase());
            } catch (IllegalArgumentException e) {
                // es opcional: si viene mal, se ignora
            }
        }

        boolean revision = args.path("requiere_revision_humana").asBoolean(false);
        String source = ctx.isAuthenticated() ? "chat-interno:" + ctx.userId() : "chat-publico";
        TicketChannel channel = ctx.channel() == InputChannel.VOICE ? TicketChannel.VOICE : TicketChannel.PORTAL;

        try {
            TicketService.AgentTicketResult r = ticketService.createFromAgent(
                    codigo, cut(asunto, 200), tema, sentimiento, cut(resumen, 1800), revision, channel, source);

            Map<String, Object> out = new LinkedHashMap<>();
            out.put("ticket_creado", r.created());
            out.put("seguimiento_agregado_a_ticket_existente", !r.created());
            out.put("folio", r.ticketId().toString());
            out.put("requiere_revision_humana", revision || !r.created());
            return jsonMapper.writeValueAsString(out);

        } catch (ResourceNotFoundException e) {
            return e.getMessage();
        }
    }

    private static String text(JsonNode args, String field) {
        return args.hasNonNull(field) ? args.get(field).asString().trim() : "";
    }

    private static String cut(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
