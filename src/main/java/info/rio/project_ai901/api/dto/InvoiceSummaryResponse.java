package info.rio.project_ai901.api.dto;

import info.rio.project_ai901.models.enums.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record InvoiceSummaryResponse(UUID id, InvoiceStatus status, int suspicionScore, String supplierName,
        String invoiceNumber, LocalDate invoiceDate, BigDecimal total, String currency, String originalFileName,
        LocalDateTime createdAt) {
}
