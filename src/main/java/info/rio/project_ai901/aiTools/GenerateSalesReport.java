package info.rio.project_ai901.aiTools;

import info.rio.project_ai901.exceptions.InvalidRequestException;
import info.rio.project_ai901.exceptions.ResourceNotFoundException;
import info.rio.project_ai901.services.report.SalesReportService;
import info.rio.project_ai901.services.report.components.GeneratedReportStore;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class GenerateSalesReport implements AgentTool {

    private final GeneratedReportStore generatedReportStore;
    private final SalesReportService reportService;
    private final JsonMapper jsonMapper;

    public GenerateSalesReport(SalesReportService reportService, JsonMapper jsonMapper, GeneratedReportStore generatedReportStore) {
        this.reportService = reportService;
        this.jsonMapper = jsonMapper;
        this.generatedReportStore = generatedReportStore;
    }

    @Override
    public String name() {
        return "generar_informe_ventas";
    }

    @Override
    public String description() {
        return "Genera un informe en Word de las ventas (pedidos entregados) por sucursal y periodo, "
             + "con cifras, productos destacados y analisis. Devuelve una ruta de descarga. "
             + "Pasa solo lo que el usuario dijo: no calcules fechas ni adivines el anio.";
    }

    @Override
    public Map<String, Object> properties() {
        return Map.of(
            "branchId", Map.of(
                "type", "string",
                "description", "UUID de la sucursal, obtenido con listar_sucursales. Opcional; sin el se "
                             + "incluyen todas las sucursales a las que el usuario tiene acceso."),
            "mes", Map.of(
                "type", "integer",
                "description", "Numero de mes (1 a 12) si el usuario menciono un mes. Opcional."),
            "anio", Map.of(
                "type", "integer",
                "description", "Anio, SOLO si el usuario lo dijo expresamente. Opcional."),
            "desde", Map.of(
                "type", "string",
                "description", "Fecha inicial AAAA-MM-DD, solo si el usuario pidio un rango. Opcional."),
            "hasta", Map.of(
                "type", "string",
                "description", "Fecha final AAAA-MM-DD, solo si el usuario pidio un rango. Opcional.")
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
        if (args.hasNonNull("branchId")) {
            try {
                branchId = UUID.fromString(args.get("branchId").asString());
            } catch (IllegalArgumentException e) {
                return "El branchId no es un UUID valido. Usa listar_sucursales para obtenerlo.";
            }
        }

        Integer mes = args.hasNonNull("mes") ? args.get("mes").asInt() : null;
        Integer anio = args.hasNonNull("anio") ? args.get("anio").asInt() : null;

        LocalDate desde;
        LocalDate hasta;
        try {
            desde = args.hasNonNull("desde") ? LocalDate.parse(args.get("desde").asString()) : null;
            hasta = args.hasNonNull("hasta") ? LocalDate.parse(args.get("hasta").asString()) : null;
        } catch (DateTimeParseException e) {
            return "Fecha no valida. Usa el formato AAAA-MM-DD.";
        }

        info.rio.project_ai901.services.report.dto.ReportRequester who = new info.rio.project_ai901.services.report.dto.ReportRequester(ctx.userId(), ctx.name(), ctx.role(), ctx.warehouseId());

        try {
            info.rio.project_ai901.services.report.dto.GeneratedReport r = reportService.generate(who, branchId, mes, anio, desde, hasta);

            Map<String, Object> out = new LinkedHashMap<>();
            out.put("informe_generado", true);
            out.put("periodo", r.periodLabel());
            out.put("alcance", r.scope());
            out.put("pedidos_entregados", r.orders());
            out.put("ventas_totales", NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-MX")).format(r.total()));
            out.put("archivo", r.filename());
            out.put("ruta_descarga", generatedReportStore.downloadPath(r.token()));
            out.put("caduca_en_minutos", GeneratedReportStore.TTL.toMinutes());
            return jsonMapper.writeValueAsString(out);

        } catch (InvalidRequestException | ResourceNotFoundException | AccessDeniedException e) {
            return e.getMessage();
        }
    }
}