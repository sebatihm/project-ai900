package info.rio.project_ai901.services.invoice;

import info.rio.project_ai901.models.Invoice;
import info.rio.project_ai901.models.Supplier;
import info.rio.project_ai901.models.enums.SuspicionRule;
import info.rio.project_ai901.repositories.InvoiceRepository;
import info.rio.project_ai901.services.invoice.dto.SuspicionReason;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class InvoiceScoringService {

    private static final double SUPPLIER_MATCH_THRESHOLD = 0.6;
    private static final int MIN_HISTORY_FOR_ATYPICAL = 5;

    private final InvoiceRepository invoiceRepository;
    private final double totalTolerance;
    private final int oldDays;
    private final BigDecimal lowConfidence;
    private final int pointsLogicalDuplicate;
    private final int pointsMissingFields;
    private final int pointsLowConfidence;
    private final int pointsSupplierMismatch;
    private final int pointsTotalInconsistent;
    private final int pointsOddDate;
    private final int pointsAtypicalAmount;

    public InvoiceScoringService(InvoiceRepository invoiceRepository,
            @Value("${app.invoice.total-tolerance:0.01}") double totalTolerance,
            @Value("${app.invoice.old-days:90}") int oldDays,
            @Value("${app.invoice.low-confidence:0.80}") double lowConfidence,
            @Value("${app.invoice.score.logical-duplicate:3}") int pointsLogicalDuplicate,
            @Value("${app.invoice.score.missing-fields:2}") int pointsMissingFields,
            @Value("${app.invoice.score.low-confidence:1}") int pointsLowConfidence,
            @Value("${app.invoice.score.supplier-mismatch:2}") int pointsSupplierMismatch,
            @Value("${app.invoice.score.total-inconsistent:2}") int pointsTotalInconsistent,
            @Value("${app.invoice.score.odd-date:1}") int pointsOddDate,
            @Value("${app.invoice.score.atypical-amount:1}") int pointsAtypicalAmount) {
        this.invoiceRepository = invoiceRepository;
        this.totalTolerance = totalTolerance;
        this.oldDays = oldDays;
        this.lowConfidence = BigDecimal.valueOf(lowConfidence);
        this.pointsLogicalDuplicate = pointsLogicalDuplicate;
        this.pointsMissingFields = pointsMissingFields;
        this.pointsLowConfidence = pointsLowConfidence;
        this.pointsSupplierMismatch = pointsSupplierMismatch;
        this.pointsTotalInconsistent = pointsTotalInconsistent;
        this.pointsOddDate = pointsOddDate;
        this.pointsAtypicalAmount = pointsAtypicalAmount;
    }

    public ScoringResult evaluate(ScoringInput input) {
        List<SuspicionReason> reasons = new ArrayList<>();
        evaluateMissingFields(input, reasons);
        evaluateLowConfidence(input, reasons);
        evaluateLogicalDuplicate(input, reasons);
        evaluateSupplierMismatch(input, reasons);
        evaluateTotalConsistency(input, reasons);
        evaluateOddDate(input, reasons);
        evaluateAtypicalAmount(input, reasons);
        int score = reasons.stream().mapToInt(SuspicionReason::points).sum();
        return new ScoringResult(score, reasons);
    }

    private void evaluateMissingFields(ScoringInput input, List<SuspicionReason> reasons) {
        List<String> missing = new ArrayList<>();
        if (isBlank(input.invoiceNumber())) {
            missing.add("numero de factura");
        }
        if (input.invoiceDate() == null) {
            missing.add("fecha");
        }
        if (input.total() == null) {
            missing.add("total");
        }
        if (!missing.isEmpty()) {
            reasons.add(new SuspicionReason(SuspicionRule.MISSING_KEY_FIELDS, pointsMissingFields,
                    "Campos clave faltantes: " + String.join(", ", missing)));
        }
    }

    private void evaluateLowConfidence(ScoringInput input, List<SuspicionReason> reasons) {
        if (input.confidenceByField() == null) {
            return;
        }
        List<String> low = new ArrayList<>();
        for (String key : List.of("invoiceNumber", "invoiceDate", "total")) {
            BigDecimal confidence = input.confidenceByField().get(key);
            if (confidence != null && confidence.compareTo(lowConfidence) < 0) {
                low.add(key);
            }
        }
        if (!low.isEmpty()) {
            reasons.add(new SuspicionReason(SuspicionRule.LOW_CONFIDENCE, pointsLowConfidence * low.size(),
                    "Confianza baja (< " + lowConfidence + ") en: " + String.join(", ", low)));
        }
    }

    private void evaluateLogicalDuplicate(ScoringInput input, List<SuspicionReason> reasons) {
        Supplier supplier = input.supplier();
        if (supplier == null || supplier.getId() == null) {
            return;
        }
        if (!isBlank(input.invoiceNumber())
                && !invoiceRepository.findBySupplier_IdAndInvoiceNumber(supplier.getId(), input.invoiceNumber())
                        .isEmpty()) {
            reasons.add(new SuspicionReason(SuspicionRule.LOGICAL_DUPLICATE, pointsLogicalDuplicate,
                    "Ya existe una factura del mismo proveedor con el numero " + input.invoiceNumber()));
            return;
        }
        if (input.invoiceDate() != null && input.total() != null
                && !invoiceRepository
                        .findBySupplier_IdAndInvoiceDateAndTotal(supplier.getId(), input.invoiceDate(), input.total())
                        .isEmpty()) {
            reasons.add(new SuspicionReason(SuspicionRule.LOGICAL_DUPLICATE, pointsLogicalDuplicate,
                    "Ya existe una factura del mismo proveedor con la misma fecha y el mismo total"));
        }
    }

    private void evaluateSupplierMismatch(ScoringInput input, List<SuspicionReason> reasons) {
        Supplier supplier = input.supplier();
        if (supplier == null || isBlank(input.vendorName()) || isBlank(supplier.getName())) {
            return;
        }
        double similarity = StringSimilarity.similarity(supplier.getName(), input.vendorName());
        if (similarity < SUPPLIER_MATCH_THRESHOLD) {
            reasons.add(new SuspicionReason(SuspicionRule.SUPPLIER_MISMATCH, pointsSupplierMismatch,
                    "El proveedor detectado ('" + input.vendorName() + "') no coincide con el seleccionado ('"
                            + supplier.getName() + "'), similitud "
                            + BigDecimal.valueOf(similarity).setScale(2, RoundingMode.HALF_UP)));
        }
    }

    private void evaluateTotalConsistency(ScoringInput input, List<SuspicionReason> reasons) {
        if (input.subtotal() == null || input.tax() == null || input.total() == null) {
            return;
        }
        BigDecimal expected = input.subtotal().add(input.tax());
        BigDecimal difference = expected.subtract(input.total()).abs();
        BigDecimal tolerance = input.total().abs().multiply(BigDecimal.valueOf(totalTolerance))
                .max(BigDecimal.valueOf(0.01));
        if (difference.compareTo(tolerance) > 0) {
            reasons.add(new SuspicionReason(SuspicionRule.TOTAL_INCONSISTENT, pointsTotalInconsistent,
                    "Subtotal + impuestos (" + expected + ") difiere del total (" + input.total() + ") por "
                            + difference));
        }
    }

    private void evaluateOddDate(ScoringInput input, List<SuspicionReason> reasons) {
        LocalDate invoiceDate = input.invoiceDate();
        if (invoiceDate == null) {
            return;
        }
        LocalDate today = LocalDate.now();
        if (invoiceDate.isAfter(today)) {
            reasons.add(new SuspicionReason(SuspicionRule.ODD_DATE, pointsOddDate,
                    "La fecha de la factura es futura: " + invoiceDate));
        } else if (invoiceDate.isBefore(today.minusDays(oldDays))) {
            reasons.add(new SuspicionReason(SuspicionRule.ODD_DATE, pointsOddDate,
                    "La fecha de la factura tiene mas de " + oldDays + " dias: " + invoiceDate));
        }
    }

    private void evaluateAtypicalAmount(ScoringInput input, List<SuspicionReason> reasons) {
        Supplier supplier = input.supplier();
        if (supplier == null || supplier.getId() == null || input.total() == null) {
            return;
        }
        List<Invoice> history = invoiceRepository.findTop20BySupplier_IdOrderByCreatedAtDesc(supplier.getId());
        List<BigDecimal> totals = history.stream().map(Invoice::getTotal).filter(java.util.Objects::nonNull).toList();
        if (totals.size() < MIN_HISTORY_FOR_ATYPICAL) {
            return;
        }
        double mean = totals.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0.0);
        double variance = totals.stream()
                .mapToDouble(total -> Math.pow(total.doubleValue() - mean, 2))
                .average()
                .orElse(0.0);
        double standardDeviation = Math.sqrt(variance);
        double value = input.total().doubleValue();
        if (standardDeviation > 0 && Math.abs(value - mean) > 3 * standardDeviation) {
            reasons.add(new SuspicionReason(SuspicionRule.ATYPICAL_AMOUNT, pointsAtypicalAmount,
                    "El total " + input.total() + " esta fuera del rango historico del proveedor (promedio "
                            + BigDecimal.valueOf(mean).setScale(2, RoundingMode.HALF_UP) + " +/- "
                            + BigDecimal.valueOf(3 * standardDeviation).setScale(2, RoundingMode.HALF_UP) + ")"));
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
