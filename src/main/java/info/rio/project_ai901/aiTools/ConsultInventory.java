package info.rio.project_ai901.aiTools;

import info.rio.project_ai901.api.dto.InventorySummaryResponse;
import info.rio.project_ai901.api.dto.PaginatedResponse;
import info.rio.project_ai901.services.inventory.InventoryService;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class ConsultInventory implements AgentTool {

    private final InventoryService inventoryService;
    private final JsonMapper jsonMapper;

    public ConsultInventory(InventoryService inventoryService, JsonMapper jsonMapper) {
        this.inventoryService = inventoryService;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public String name() {
        return "consultar_inventario";
    }

    @Override
    public String description() {
        return "Consulta el inventario de la bodega del usuario autenticado. "
             + "Sin productId devuelve el inventario completo de la bodega, paginado. "
             + "Con productId devuelve solo las existencias de ese producto. "
             + "El productId se obtiene primero con buscar_producto; nunca lo inventes.";
    }

    @Override
    public Map<String, Object> properties() {
        return Map.of(
            "productId", Map.of(
                "type", "string",
                "description", "UUID del producto, obtenido con buscar_producto. Opcional."),
            "offset", Map.of(
                "type", "integer",
                "description", "Desplazamiento de paginacion; debe ser multiplo de limit. Por defecto 0."),
            "limit", Map.of(
                "type", "integer",
                "description", "Maximo de resultados (1 a 20). Por defecto 10.")
        );
    }

    @Override
    public List<String> required() {
        return List.of(); // todos opcionales
    }

    @Override
    public Set<String> allowedRoles() {
        return Set.of("WAREHOUSE_HEAD", "ADMIN");
    }

    @Override
    public String execute(JsonNode args, UserContext ctx) {
        // La bodega SIEMPRE sale del token, nunca de los argumentos del modelo.
        if (ctx.warehouseId() == null) {
            return "Tu usuario no tiene una bodega asignada.";
        }

        UUID productId = null;
        if (args.hasNonNull("productId")) {
            try {
                productId = UUID.fromString(args.get("productId").asString());
            } catch (IllegalArgumentException e) {
                return "El productId no es un UUID valido. Usa buscar_producto para obtenerlo.";
            }
        }

        long offset = Math.max(args.path("offset").asLong(0), 0);
        int limit = Math.min(Math.max(args.path("limit").asInt(10), 1), 20);

        PaginatedResponse<InventorySummaryResponse> page =
                inventoryService.list(ctx.warehouseId(), productId, offset, limit);

        if (page.items().isEmpty()) {
            return "No hay registros de inventario para esa consulta en tu bodega.";
        }

        return jsonMapper.writeValueAsString(Map.of(
                "inventario", page.items(),
                "total", page.total()));
    }
}