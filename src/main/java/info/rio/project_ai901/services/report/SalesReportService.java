package info.rio.project_ai901.services.report;

import info.rio.project_ai901.exceptions.InvalidRequestException;
import info.rio.project_ai901.exceptions.ResourceNotFoundException;
import info.rio.project_ai901.models.Branch;
import info.rio.project_ai901.models.enums.OrderStatus;
import info.rio.project_ai901.repositories.BranchRepository;
import info.rio.project_ai901.repositories.OrderItemRepository;
import info.rio.project_ai901.repositories.OrderRepository;
import info.rio.project_ai901.repositories.ReportRows;
import info.rio.project_ai901.services.report.components.GeneratedReportStore;
import info.rio.project_ai901.services.report.components.ReportPeriodResolver;
import info.rio.project_ai901.services.report.components.SalesReportWordBuilder;
import info.rio.project_ai901.services.report.dto.GeneratedReport;
import info.rio.project_ai901.services.report.dto.ReportPeriod;
import info.rio.project_ai901.services.report.dto.ReportRequester;
import info.rio.project_ai901.services.report.dto.SalesReportData;
import info.rio.project_ai901.services.warehouse.WarehouseService;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class SalesReportService {

    private static final Set<String> ALLOWED_ROLES = Set.of("ADMIN", "BOSS", "BRANCH_MANAGER");
    private static final int MAX_DETAIL_ROWS = 1000;
    private static final int TOP_PRODUCTS = 10;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final BranchRepository branchRepository;
    private final WarehouseService warehouseService;
    private final ReportPeriodResolver periodResolver;
    private final ReportAnalysisService analysisService;
    private final SalesReportWordBuilder wordBuilder;
    private final GeneratedReportStore store;

    public SalesReportService(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                              BranchRepository branchRepository, WarehouseService warehouseService,
                              ReportPeriodResolver periodResolver, ReportAnalysisService analysisService,
                              SalesReportWordBuilder wordBuilder, GeneratedReportStore store) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.branchRepository = branchRepository;
        this.warehouseService = warehouseService;
        this.periodResolver = periodResolver;
        this.analysisService = analysisService;
        this.wordBuilder = wordBuilder;
        this.store = store;
    }

    // Sin @Transactional a proposito: incluye una llamada a IA y no debe retener una conexion de BD.
    public GeneratedReport generate(ReportRequester who, UUID requestedBranchId,
                                    Integer month, Integer year, LocalDate from, LocalDate to) {

        // 1) Permisos
        if (who == null || !ALLOWED_ROLES.contains(who.role())) {
            throw new AccessDeniedException("Sin permiso para generar informes de ventas");
        }

        // 2) Alcance: el BRANCH_MANAGER queda atado a su sucursal (derivada de su almacen)
        UUID branchId = requestedBranchId;
        if ("BRANCH_MANAGER".equals(who.role())) {
            if (who.warehouseId() == null) {
                throw new InvalidRequestException(
                        "Tu usuario no tiene un almacen asignado, no puedo determinar tu sucursal.");
            }
            branchId = warehouseService.branchIdOf(who.warehouseId());
        }
        String scope = "Todas las sucursales";
        if (branchId != null) {
            scope = branchRepository.findById(branchId)
                    .map(Branch::getName)
                    .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));
        }

        // 3) Periodo
        ReportPeriod period = periodResolver.resolve(month, year, from, to);
        LocalDateTime start = period.from().atStartOfDay();
        LocalDateTime end = period.to().plusDays(1).atStartOfDay();   // limite superior exclusivo

        // 4) Cifras (todo sale de la base de datos, nunca del modelo)
        OrderStatus status = OrderStatus.DELIVERED;
        List<ReportRows.BranchSales> branchRows = orderRepository.salesByBranch(status, start, end, branchId);
        if (branchRows.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No hay pedidos entregados en " + period.label() + " para: " + scope + ".");
        }

        long totalOrders = branchRows.stream().mapToLong(ReportRows.BranchSales::getOrders).sum();
        BigDecimal totalSales = branchRows.stream()
                .map(ReportRows.BranchSales::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal averageTicket = average(totalSales, totalOrders);

        List<SalesReportData.BranchLine> branches = branchRows.stream()
                .map(b -> new SalesReportData.BranchLine(
                        b.getBranchCode(), b.getBranchName(), b.getOrders(),
                        b.getTotal().setScale(2, RoundingMode.HALF_UP),
                        average(b.getTotal(), b.getOrders()),
                        share(b.getTotal(), totalSales)))
                .toList();

        List<SalesReportData.ProductLine> products = orderItemRepository
                .topProducts(status, start, end, branchId, PageRequest.of(0, TOP_PRODUCTS)).stream()
                .map(p -> new SalesReportData.ProductLine(
                        p.getSku(), p.getName(), p.getUnits(),
                        p.getRevenue().setScale(2, RoundingMode.HALF_UP)))
                .toList();

        List<SalesReportData.OrderLine> orders = orderRepository
                .deliveredRows(status, start, end, branchId, PageRequest.of(0, MAX_DETAIL_ROWS)).stream()
                .map(o -> new SalesReportData.OrderLine(
                        o.getCode(), o.getOrderDate(), o.getBranchName(),
                        o.getTotal().setScale(2, RoundingMode.HALF_UP)))
                .toList();

        SalesReportData data = new SalesReportData(
                scope, period, who.name(), LocalDateTime.now(ReportPeriodResolver.ZONE),
                totalOrders, totalSales, averageTicket, branches, products, orders,
                totalOrders > MAX_DETAIL_ROWS, null);

        // 5) Analisis con IA (si falla, el informe se genera igual sin esa seccion)
        data = data.withAnalysis(analysisService.analyze(data));

        // 6) Word y almacenamiento temporal
        byte[] docx = wordBuilder.build(data);
        String filename = "informe-ventas-" + period.from() + "_" + period.to() + ".docx";
        String token = store.save(who.userId(), filename, docx);

        return new GeneratedReport(token, filename, period.label(), scope, totalOrders, totalSales);
    }

    private BigDecimal average(BigDecimal total, long count) {
        return count == 0 ? BigDecimal.ZERO
                : total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal share(BigDecimal part, BigDecimal whole) {
        return whole.signum() == 0 ? BigDecimal.ZERO
                : part.multiply(BigDecimal.valueOf(100)).divide(whole, 1, RoundingMode.HALF_UP);
    }
}