package info.rio.project_ai901.repositories;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public final class ReportRows {

    private ReportRows() {}

    public interface BranchSales {
        UUID getBranchId();
        String getBranchCode();
        String getBranchName();
        Long getOrders();
        BigDecimal getTotal();
    }

    public interface OrderRow {
        String getCode();
        LocalDateTime getOrderDate();
        String getBranchName();
        BigDecimal getTotal();
    }

    public interface ProductSales {
        String getSku();
        String getName();
        Long getUnits();
        BigDecimal getRevenue();
    }
}