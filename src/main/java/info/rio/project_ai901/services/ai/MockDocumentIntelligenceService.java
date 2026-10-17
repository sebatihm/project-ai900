package info.rio.project_ai901.services.ai;

import info.rio.project_ai901.services.ai.dto.InvoiceOcrResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@ConditionalOnProperty(name = "app.ai.di.provider", havingValue = "mock", matchIfMissing = true)
public class MockDocumentIntelligenceService implements DocumentIntelligenceService {

    @Override
    public InvoiceOcrResult analyzeInvoice(InputStream content) {
        InvoiceOcrResult result = new InvoiceOcrResult();
        result.setVendorName(null);
        result.setInvoiceNumber(null);
        result.setInvoiceDate(null);
        result.setInvoiceTotal(null);
        result.setConfidence(BigDecimal.ZERO);
        result.setMissingFields(new ArrayList<>(List.of("vendorName", "invoiceNumber", "invoiceDate", "invoiceTotal")));
        return result;
    }
}
