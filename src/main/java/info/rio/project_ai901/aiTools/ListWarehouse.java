package info.rio.project_ai901.aiTools;

import info.rio.project_ai901.api.dto.PaginatedResponse;
import info.rio.project_ai901.api.dto.WarehouseSummaryResponse;
import info.rio.project_ai901.services.warehouse.WarehouseService;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class ListWarehouse implements AgentTool {

    private final WarehouseService warehouseService;
    private final JsonMapper jsonMapper;

    public ListWarehouse(WarehouseService warehouseService, JsonMapper jsonMapper) {
        this.warehouseService = warehouseService;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public String name() {
        return "listar_almacenes";
    }

    @Override
    public String description() {
        return "Lista los almacenes activos por sucursal, con su codigo y nombre. "
             + "Para filtrar por sucursal pasa su branchId, obtenido con listar_sucursales; nunca lo inventes. "
             + "Un gerente de sucursal solo ve los almacenes de su propia sucursal.";
    }

    @Override
    public Map<String, Object> properties() {
        return Map.of(
            "branchId", Map.of(
                "type", "string",
                "description", "UUID de la sucursal, obtenido con listar_sucursales. Opcional."),
            "offset", Map.of(
                "type", "integer",
                "description", "Desplazamiento de paginacion; debe ser multiplo de limit. Por defecto 0."),
            "limit", Map.of(
                "type", "integer",
                "description", "Maximo de resultados (1 a 20). Por defecto 20.")
        );
    }

    @Override
    public List<String> required() {
        return List.of();
    }

    @Override
    public Set<String> allowedRoles() {
        return Set.of("BRANCH_MANAGER", "ADMIN", "BOSS");
    }

    @Override
    public String execute(JsonNode args, UserContext ctx) {
        UUID branchId;

        if ("BRANCH_MANAGER".equals(ctx.role())) {
            // La sucursal se deriva de la bodega del token; se ignora lo que mande el modelo.
            if (ctx.warehouseId() == null) {
                return "Tu usuario no tiene un almacen asignado, no puedo determinar tu sucursal.";
            }
            branchId = warehouseService.branchIdOf(ctx.warehouseId());
        } else {
            branchId = null; // ADMIN y BOSS: todas las sucursales, salvo que filtren
            if (args.hasNonNull("branchId")) {
                try {
                    branchId = UUID.fromString(args.get("branchId").asString());
                } catch (IllegalArgumentException e) {
                    return "El branchId no es un UUID valido. Usa listar_sucursales para obtenerlo.";
                }
            }
        }

        long offset = Math.max(args.path("offset").asLong(0), 0);
        int limit = Math.min(Math.max(args.path("limit").asInt(20), 1), 20);

        // includeInactive = false SIEMPRE: no es decision del modelo.
        PaginatedResponse<WarehouseSummaryResponse> page =
                warehouseService.list(false, branchId, offset, limit);

        if (page.items().isEmpty()) {
            return "No hay almacenes activos para esa consulta.";
        }

        return jsonMapper.writeValueAsString(Map.of(
                "almacenes", page.items(),
                "total", page.total()));
    }
}