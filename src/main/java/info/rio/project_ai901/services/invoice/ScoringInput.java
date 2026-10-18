package info.rio.project_ai901.services.invoice;

import info.rio.project_ai901.models.Supplier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record ScoringInput(Supplier supplier, String vendorName, String invoiceNumber, LocalDate invoiceDate,
        BigDecimal subtotal, BigDecimal tax, BigDecimal total, Map<String, BigDecimal> confidenceByField,
        BigDecimal minConfidence) {
}
