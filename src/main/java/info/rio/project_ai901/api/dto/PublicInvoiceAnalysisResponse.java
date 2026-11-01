package info.rio.project_ai901.api.dto;

import info.rio.project_ai901.models.enums.InvoiceStatus;
import info.rio.project_ai901.services.invoice.dto.SuspicionReason;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PublicInvoiceAnalysisResponse(
        UUID invoiceId,
        InvoiceStatus status,
        int suspicionScore,
        List<SuspicionReason> reasons,
        UUID supplierId,
        String supplierName,
        String invoiceNumber,
        LocalDate invoiceDate,
        LocalDate dueDate,
        BigDecimal subtotal,
        BigDecimal tax,
        BigDecimal total,
        String currency,
        List<InvoiceItemResponse> items,
        LocalDateTime createdAt) {

    public static PublicInvoiceAnalysisResponse from(InvoiceAnalysisResponse source) {
        return new PublicInvoiceAnalysisResponse(
                source.invoiceId(),
                source.status(),
                source.suspicionScore(),
                source.reasons(),
                source.supplierId(),
                source.supplierName(),
                source.invoiceNumber(),
                source.invoiceDate(),
                source.dueDate(),
                source.subtotal(),
                source.tax(),
                source.total(),
                source.currency(),
                source.items(),
                source.createdAt());
    }
}
