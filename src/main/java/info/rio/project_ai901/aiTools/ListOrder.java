package info.rio.project_ai901.aiTools;

import info.rio.project_ai901.api.dto.OrderSummaryResponse;
import info.rio.project_ai901.api.dto.PaginatedResponse;
import info.rio.project_ai901.models.enums.OrderStatus;
import info.rio.project_ai901.services.order.OrderService;
import info.rio.project_ai901.services.warehouse.WarehouseService;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class ListOrder implements AgentTool {

    private final OrderService orderService;
    private final WarehouseService warehouseService;
    private final JsonMapper jsonMapper;

    public ListOrder(OrderService orderService, WarehouseService warehouseService,
                             JsonMapper jsonMapper) {
        this.orderService = orderService;
        this.warehouseService = warehouseService;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public String name() {
        return "listar_pedidos";
    }

    @Override
    public String description() {
        return "Lista pedidos, del mas reciente al mas antiguo, con filtros opcionales por estado y sucursal. "
             + "Solo para personal autenticado. Un gerente de sucursal solo ve los pedidos de su sucursal.";
    }

    @Override
    public Map<String, Object> properties() {
        return Map.of(
            "status", Map.of(
                "type", "string",
                "enum", List.of("RECEIVED", "PREPARING", "SHIPPED", "DELIVERED", "CANCELED"),
                "description", "Filtra por estado del pedido. Opcional."),
            "branchId", Map.of(
                "type", "string",
                "description", "UUID de la sucursal, obtenido con listar_sucursales. Opcional."),
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
        return List.of();
    }

    @Override
    public Set<String> allowedRoles() {
        return Set.of("ADMIN", "BOSS", "BRANCH_MANAGER");
    }

    @Override
    public String execute(JsonNode args, UserContext ctx) {
        UUID branchId = null;

        if ("BRANCH_MANAGER".equals(ctx.role())) {
            // La sucursal se deriva de la bodega del token; se ignora lo que mande el modelo.
            if (ctx.warehouseId() == null) {
                return "Tu usuario no tiene un almacen asignado, no puedo determinar tu sucursal.";
            }
            branchId = warehouseService.branchIdOf(ctx.warehouseId());
        } else if (args.hasNonNull("branchId")) {
            try {
                branchId = UUID.fromString(args.get("branchId").asString());
            } catch (IllegalArgumentException e) {
                return "El branchId no es un UUID valido. Usa listar_sucursales para obtenerlo.";
            }
        }

        OrderStatus status = null;
        if (args.hasNonNull("status")) {
            try {
                status = OrderStatus.valueOf(args.get("status").asString().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return "Estado no valido. Usa RECEIVED, PREPARING, SHIPPED, DELIVERED o CANCELED.";
            }
        }

        long offset = Math.max(args.path("offset").asLong(0), 0);
        int limit = Math.min(Math.max(args.path("limit").asInt(10), 1), 20);

        PaginatedResponse<OrderSummaryResponse> page =
                orderService.list(null, branchId, status, offset, limit);

        if (page.items().isEmpty()) {
            return "No hay pedidos para esa consulta.";
        }

        return jsonMapper.writeValueAsString(Map.of(
                "pedidos", page.items(),
                "total", page.total()));
    }
}