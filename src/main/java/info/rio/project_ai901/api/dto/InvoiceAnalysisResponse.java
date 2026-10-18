package info.rio.project_ai901.api.dto;

import info.rio.project_ai901.models.enums.InvoiceStatus;
import info.rio.project_ai901.services.invoice.dto.SuspicionReason;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record InvoiceAnalysisResponse(UUID invoiceId, InvoiceStatus status, int suspicionScore,
        List<SuspicionReason> reasons, UUID supplierId, String supplierName, String invoiceNumber, LocalDate invoiceDate,
        LocalDate dueDate, BigDecimal subtotal, BigDecimal tax, BigDecimal total, String currency,
        BigDecimal minConfidence, String originalFileName, String sha256, String container, String blobPath,
        String readUrl, List<InvoiceItemResponse> items, LocalDateTime createdAt) {
}
