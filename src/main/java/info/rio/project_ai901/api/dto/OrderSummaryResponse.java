package info.rio.project_ai901.api.dto;

import info.rio.project_ai901.models.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderSummaryResponse(
        UUID id,
        String code,
        OrderStatus status,
        UUID customerId,
        String customerName,
        UUID branchId,
        String branchCode,
        LocalDateTime orderDate,
        LocalDate estimatedDeliveryDate,
        BigDecimal total) {
}
