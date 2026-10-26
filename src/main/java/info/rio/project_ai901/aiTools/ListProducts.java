package info.rio.project_ai901.aiTools;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import info.rio.project_ai901.services.product.ProductService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
public class ListProducts implements AgentTool {

    // Lo único que ve el modelo: sin precio ni campos internos
    private record ProductoDto(UUID id, String sku, String nombre) {}

    private final ProductService productService;
    private final JsonMapper mapper;

    public ListProducts(ProductService productService, JsonMapper mapper) {
        this.productService = productService;
        this.mapper = mapper;
    }

    @Override
    public String name() { return "buscar_producto"; }

    @Override
    public String description() {
        return "Busca productos del catálogo por nombre o SKU y devuelve su id (UUID), SKU y nombre. "
             + "Úsala siempre antes de consultar inventario para obtener el id real del producto. "
             + "Usa una sola palabra clave (por ejemplo 'jabón'), no frases largas. "
             + "Si no se da texto, lista los productos.";
    }

    @Override
    public Map<String, Object> properties() {
        return Map.of(
            "texto",  Map.of("type", "string",  "description", "Palabra clave del nombre o SKU. Opcional"),
            "offset", Map.of("type", "integer", "description", "Posición inicial, empieza en 0"),
            "limit",  Map.of("type", "integer", "description", "Cantidad de resultados, máximo 20"));
    }

    @Override
    public List<String> required() { return List.of(); } 

    @Override
    public String execute(JsonNode args, UserContext ctx) {
        String texto = args.hasNonNull("texto") ? args.get("texto").asText() : null;
        long offset = args.path("offset").asLong(0);
        int limit = Math.min(args.path("limit").asInt(20), 20);   // tope real

        var page = productService.list(offset, limit, texto);

        if (page.items().isEmpty()) {       
            return "Sin resultados";
        }

        var productos = page.items().stream()
                .map(p -> new ProductoDto(p.id(), p.sku(), p.name()))   
                .toList();

        return mapper.writeValueAsString(Map.of("productos", productos, "total", page.total()));
    }
}