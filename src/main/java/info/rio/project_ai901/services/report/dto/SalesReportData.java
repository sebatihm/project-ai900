package info.rio.project_ai901.services.report.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SalesReportData(
        String scope,
        ReportPeriod period,
        String generatedBy,
        LocalDateTime generatedAt,
        long totalOrders,
        BigDecimal totalSales,
        BigDecimal averageTicket,
        List<BranchLine> branches,
        List<ProductLine> topProducts,
        List<OrderLine> orders,
        boolean ordersTruncated,
        String analysis) {

    public record BranchLine(String code, String name, long orders, BigDecimal total,
                             BigDecimal average, BigDecimal sharePercent) {}

    public record ProductLine(String sku, String name, long units, BigDecimal revenue) {}

    public record OrderLine(String code, LocalDateTime date, String branch, BigDecimal total) {}

    public SalesReportData withAnalysis(String text) {
        return new SalesReportData(scope, period, generatedBy, generatedAt, totalOrders, totalSales,
                averageTicket, branches, topProducts, orders, ordersTruncated, text);
    }
}