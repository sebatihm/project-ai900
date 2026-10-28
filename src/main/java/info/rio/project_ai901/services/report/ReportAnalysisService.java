package info.rio.project_ai901.services.report;


import info.rio.project_ai901.services.ai.AgentService;
import info.rio.project_ai901.services.report.dto.SalesReportData;

// copia aqui desde AgentService los imports de: ResponsesClient, AgentReference,
// AzureCreateResponseOptions, Response y ResponseCreateParams
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.azure.ai.agents.ResponsesClient;
import com.azure.ai.agents.models.AgentReference;
import com.azure.ai.agents.models.AzureCreateResponseOptions;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

@Service
public class ReportAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(ReportAnalysisService.class);
    private static final Locale ES = Locale.forLanguageTag("es-MX");
    private static final DateTimeFormatter DM = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String AGENT_NAME = "agente-inventario";

    private final ResponsesClient responses;
    private final AgentReference agent;

    public ReportAnalysisService(ResponsesClient responses) {
        this.responses = responses;
        this.agent = new AgentReference(AGENT_NAME);
    }

    /** Devuelve el analisis, o null si no se pudo generar. */
    public String analyze(SalesReportData d) {
        try {
            Response r = responses.createAzureResponse(
                    new AzureCreateResponseOptions().setAgentReference(agent),
                    ResponseCreateParams.builder().input(buildPrompt(d)));
            String text = AgentService.extraerTexto(r);
            return (text == null || text.isBlank()) ? null : text.trim();
        } catch (Exception e) {
            log.warn("No se pudo generar el analisis del informe: {}", e.toString());
            return null;
        }
    }

    private String buildPrompt(SalesReportData d) {
        NumberFormat money = NumberFormat.getCurrencyInstance(ES);
        StringBuilder sb = new StringBuilder();
        sb.append("MODO ANALISIS\n");
        sb.append("Redacta un analisis breve (maximo 200 palabras, en espanol, 2 a 4 parrafos de texto plano) ")
          .append("de las cifras de ventas que aparecen abajo.\n");
        sb.append("Reglas: describe solo lo que muestran las cifras (totales, comparacion entre sucursales, ")
          .append("productos destacados, mejores y peores dias). No hagas recomendaciones, predicciones ni ")
          .append("suposiciones sobre causas. No inventes datos y cita las cifras tal como aparecen. ")
          .append("No uses herramientas, listas, encabezados ni formato markdown.\n\n");

        sb.append("DATOS\n");
        sb.append("Alcance: ").append(d.scope()).append('\n');
        sb.append("Periodo: ").append(d.period().label()).append('\n');
        sb.append("Pedidos entregados: ").append(d.totalOrders()).append('\n');
        sb.append("Ventas totales: ").append(money.format(d.totalSales())).append('\n');
        sb.append("Promedio por pedido: ").append(money.format(d.averageTicket())).append("\n\n");

        sb.append("Por sucursal:\n");
        d.branches().forEach(b -> sb.append("- ").append(b.name())
                .append(": ").append(b.orders()).append(" pedidos, ")
                .append(money.format(b.total())).append(", promedio ")
                .append(money.format(b.average())).append(", ")
                .append(b.sharePercent()).append("% del total\n"));

        sb.append("\nProductos con mejor rendimiento (por ingresos):\n");
        d.topProducts().forEach(p -> sb.append("- ").append(p.name())
                .append(": ").append(p.units()).append(" unidades, ")
                .append(money.format(p.revenue())).append('\n'));

        if (!d.ordersTruncated() && !d.orders().isEmpty()) {
            Map<LocalDate, BigDecimal> perDay = new TreeMap<>();
            d.orders().forEach(o -> perDay.merge(o.date().toLocalDate(), o.total(), BigDecimal::add));
            var best = perDay.entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow();
            var worst = perDay.entrySet().stream().min(Map.Entry.comparingByValue()).orElseThrow();
            sb.append("\nDias con ventas: ").append(perDay.size()).append('\n');
            sb.append("Dia con mayores ventas: ").append(DM.format(best.getKey()))
              .append(" (").append(money.format(best.getValue())).append(")\n");
            sb.append("Dia con menores ventas (entre los dias con ventas): ").append(DM.format(worst.getKey()))
              .append(" (").append(money.format(worst.getValue())).append(")\n");
        }
        return sb.toString();
    }
}