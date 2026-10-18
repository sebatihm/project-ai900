package info.rio.project_ai901.services.ai;

import info.rio.project_ai901.services.ai.dto.InvoiceItemData;
import info.rio.project_ai901.services.ai.dto.InvoiceOcrResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "app.ai.di.provider", havingValue = "mock", matchIfMissing = true)
public class MockDocumentIntelligenceService implements DocumentIntelligenceService {

    private final boolean sample;

    public MockDocumentIntelligenceService(@Value("${app.ai.di.mock.mode:sample}") String mode) {
        this.sample = !"empty".equalsIgnoreCase(mode);
    }

    @Override
    public InvoiceOcrResult analyzeInvoice(InputStream content) {
        InvoiceOcrResult result = new InvoiceOcrResult();
        result.setDocumentCount(sample ? 1 : 0);
        if (!sample) {
            return result;
        }

        result.setVendorName("Proveedor Demo SA de CV");
        result.setInvoiceNumber("F-0001");
        result.setInvoiceDate(LocalDate.now().minusDays(3));
        result.setDueDate(LocalDate.now().plusDays(27));
        result.setSubtotal(new BigDecimal("1000.00"));
        result.setTax(new BigDecimal("160.00"));
        result.setTotal(new BigDecimal("1160.00"));
        result.setCurrency("MXN");
        result.setItems(List.of(new InvoiceItemData("Caja de guantes de nitrilo", BigDecimal.valueOf(10),
                new BigDecimal("1000.00"))));
        result.setConfidenceByField(Map.of(
                "vendorName", BigDecimal.valueOf(0.97),
                "invoiceNumber", BigDecimal.valueOf(0.96),
                "invoiceDate", BigDecimal.valueOf(0.95),
                "dueDate", BigDecimal.valueOf(0.93),
                "subtotal", BigDecimal.valueOf(0.94),
                "tax", BigDecimal.valueOf(0.93),
                "total", BigDecimal.valueOf(0.96)));
        result.setMinConfidence(new BigDecimal("0.950"));
        result.setRawJson("{\"mock\":true,\"model\":\"prebuilt-invoice\"}");
        return result;
    }
}
