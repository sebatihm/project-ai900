package info.rio.project_ai901.aiTools;

import info.rio.project_ai901.api.dto.BranchSummaryResponse;
import info.rio.project_ai901.api.dto.PaginatedResponse;
import info.rio.project_ai901.services.branch.BranchService;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

@Component
public class ListBranch implements AgentTool {

    private final BranchService branchService;
    private final JsonMapper jsonMapper;

    public ListBranch(BranchService branchService, JsonMapper jsonMapper) {
        this.branchService = branchService;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public String name() {
        return "listar_sucursales";
    }

    @Override
    public String description() {
        return "Lista las sucursales activas de la empresa con su codigo, nombre y ciudad. "
             + "Usala cuando pregunten donde hay sucursales, en que ciudades estamos o como se llama una sucursal.";
    }

    @Override
    public Map<String, Object> properties() {
        return Map.of(
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
    public boolean isPublic() {
        return true;
    }

    @Override
    public String execute(tools.jackson.databind.JsonNode args, UserContext ctx) {
        long offset = Math.max(args.path("offset").asLong(0), 0);
        int limit = Math.min(Math.max(args.path("limit").asInt(20), 1), 20);

        PaginatedResponse<BranchSummaryResponse> page = branchService.list(false, offset, limit);

        if (page.items().isEmpty()) {
            return "No hay sucursales registradas.";
        }

        return jsonMapper.writeValueAsString(Map.of(
                "sucursales", page.items(),
                "total", page.total()));
    }
}