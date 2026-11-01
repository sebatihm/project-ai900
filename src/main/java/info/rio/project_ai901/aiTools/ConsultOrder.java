package info.rio.project_ai901.aiTools;

import info.rio.project_ai901.api.dto.OrderTrackingResponse;
import info.rio.project_ai901.services.order.OrderService;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
public class ConsultOrder implements AgentTool {

    // private static final Pattern CODE_FORMAT = Pattern.compile("^ORD-\\d{8}-\\d{4,}$");

    private final OrderService orderService;
    private final JsonMapper jsonMapper;

    public ConsultOrder(OrderService orderService, JsonMapper jsonMapper) {
        this.orderService = orderService;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public String name() {
        return "consultar_pedido";
    }

    @Override
    public String description() {
        return "Consulta el estado de UN pedido a partir de su codigo exacto. "
             + "Solo funciona si el usuario proporciono el codigo; nunca lo inventes, completes ni adivines.";
    }

    @Override
    public Map<String, Object> properties() {
        return Map.of(
            "code", Map.of(
                "type", "string",
                "description", "Codigo del pedido tal como lo escribio el usuario"));
    }

    @Override
    public List<String> required() {
        return List.of("code");
    }

    @Override
    public boolean isPublic() {
        return true;
    }

    @Override
    public String execute(JsonNode args, UserContext ctx) {
        String code = args.hasNonNull("code") ? args.get("code").asString().trim().toUpperCase() : "";

        // if (!CODE_FORMAT.matcher(code).matches()) {
        //     return "El codigo no tiene un formato valido. Debe verse como ORD-20261026-0001. "
        //          + "Pide al usuario que lo revise.";
        // }

        Optional<OrderTrackingResponse> found = orderService.trackByCode(code);
        if (found.isEmpty()) {
            return "No se encontro un pedido con ese codigo. Pide al usuario que lo verifique.";
        }

        OrderTrackingResponse o = found.get();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("codigo", o.code());
        out.put("estado", describe(o.status()));
        out.put("estado_descripcion", describe(o.status()));
        out.put("fecha_pedido", o.orderDate());
        out.put("fecha_estimada_entrega", o.estimatedDeliveryDate());
        return jsonMapper.writeValueAsString(out);
    }

    private static String describe(String status) {
        return switch (status) {
            case "RECEIVED" -> "Recibido";
            case "PREPARING" -> "En preparacion";
            case "SHIPPED" -> "Enviado";
            case "DELIVERED" -> "Entregado";
            case "CANCELED" -> "Cancelado";
            default -> status;
        };
    }
}