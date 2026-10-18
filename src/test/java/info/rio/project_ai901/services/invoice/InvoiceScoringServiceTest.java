package info.rio.project_ai901.services.invoice;

import info.rio.project_ai901.models.Supplier;
import info.rio.project_ai901.models.enums.SuspicionRule;
import info.rio.project_ai901.repositories.InvoiceRepository;
import info.rio.project_ai901.services.invoice.dto.SuspicionReason;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class InvoiceScoringServiceTest {

    private InvoiceScoringService service;

    @BeforeEach
    void setUp() {
        service = new InvoiceScoringService(Mockito.mock(InvoiceRepository.class), 0.01, 90, 0.80, 3, 2, 1, 2, 2, 1, 1);
    }

    @Test
    void returnsNoReasonsForACleanInvoice() {
        ScoringResult result = service.evaluate(input(null, "Proveedor Uno", "F-001", LocalDate.now().minusDays(5),
                new BigDecimal("100.00"), new BigDecimal("21.00"), new BigDecimal("121.00"), confidentFields()));

        assertThat(result.score()).isZero();
        assertThat(result.reasons()).isEmpty();
    }

    @Test
    void flagsMissingFields() {
        ScoringResult result = service.evaluate(input(null, "Proveedor Uno", null, null, null, null, null,
                new HashMap<>()));

        assertThat(rules(result)).contains(SuspicionRule.MISSING_KEY_FIELDS);
        assertThat(result.reasons()).filteredOn(reason -> reason.rule() == SuspicionRule.MISSING_KEY_FIELDS)
                .allSatisfy(reason -> assertThat(reason.points()).isEqualTo(2));
    }

    @Test
    void flagsLowConfidence() {
        Map<String, BigDecimal> confidences = new HashMap<>();
        confidences.put("invoiceNumber", new BigDecimal("0.50"));
        confidences.put("invoiceDate", new BigDecimal("0.95"));
        confidences.put("total", new BigDecimal("0.95"));

        ScoringResult result = service.evaluate(input(null, "Proveedor Uno", "F-001", LocalDate.now().minusDays(5),
                new BigDecimal("100.00"), new BigDecimal("21.00"), new BigDecimal("121.00"), confidences));

        assertThat(rules(result)).containsExactly(SuspicionRule.LOW_CONFIDENCE);
        assertThat(result.score()).isEqualTo(1);
    }

    @Test
    void flagsTotalInconsistency() {
        ScoringResult result = service.evaluate(input(null, "Proveedor Uno", "F-001", LocalDate.now().minusDays(5),
                new BigDecimal("100.00"), new BigDecimal("10.00"), new BigDecimal("500.00"), confidentFields()));

        assertThat(rules(result)).contains(SuspicionRule.TOTAL_INCONSISTENT);
    }

    @Test
    void flagsFutureDate() {
        ScoringResult result = service.evaluate(input(null, "Proveedor Uno", "F-001", LocalDate.now().plusDays(3),
                new BigDecimal("100.00"), new BigDecimal("21.00"), new BigDecimal("121.00"), confidentFields()));

        assertThat(rules(result)).contains(SuspicionRule.ODD_DATE);
    }

    @Test
    void flagsSupplierMismatch() {
        ScoringResult result = service.evaluate(input(supplier("Acme Corporation"), "Zzzzzzzz S.A.", "F-001",
                LocalDate.now().minusDays(5), new BigDecimal("100.00"), new BigDecimal("21.00"),
                new BigDecimal("121.00"), confidentFields()));

        assertThat(rules(result)).contains(SuspicionRule.SUPPLIER_MISMATCH);
    }

    @Test
    void acceptsSimilarSupplierName() {
        ScoringResult result = service.evaluate(input(supplier("Acme Corporation"), "Acme Corp.", "F-001",
                LocalDate.now().minusDays(5), new BigDecimal("100.00"), new BigDecimal("21.00"),
                new BigDecimal("121.00"), confidentFields()));

        assertThat(rules(result)).doesNotContain(SuspicionRule.SUPPLIER_MISMATCH);
    }

    private Set<SuspicionRule> rules(ScoringResult result) {
        return result.reasons().stream().map(SuspicionReason::rule).collect(Collectors.toSet());
    }

    private ScoringInput input(Supplier supplier, String vendorName, String invoiceNumber, LocalDate invoiceDate,
            BigDecimal subtotal, BigDecimal tax, BigDecimal total, Map<String, BigDecimal> confidence) {
        return new ScoringInput(supplier, vendorName, invoiceNumber, invoiceDate, subtotal, tax, total, confidence,
                new BigDecimal("0.95"));
    }

    private Supplier supplier(String name) {
        Supplier supplier = new Supplier();
        supplier.setId(UUID.randomUUID());
        supplier.setName(name);
        return supplier;
    }

    private Map<String, BigDecimal> confidentFields() {
        Map<String, BigDecimal> confidences = new HashMap<>();
        confidences.put("invoiceNumber", new BigDecimal("0.95"));
        confidences.put("invoiceDate", new BigDecimal("0.95"));
        confidences.put("total", new BigDecimal("0.95"));
        return confidences;
    }
}
