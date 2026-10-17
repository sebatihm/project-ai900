package info.rio.project_ai901.services.ai;

import com.azure.ai.documentintelligence.DocumentIntelligenceClient;
import com.azure.ai.documentintelligence.DocumentIntelligenceClientBuilder;
import com.azure.ai.documentintelligence.models.AnalyzeDocumentOptions;
import com.azure.ai.documentintelligence.models.AnalyzeOperationDetails;
import com.azure.ai.documentintelligence.models.AnalyzeResult;
import com.azure.ai.documentintelligence.models.AnalyzedDocument;
import com.azure.ai.documentintelligence.models.CurrencyValue;
import com.azure.ai.documentintelligence.models.DocumentField;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.BinaryData;
import com.azure.core.util.polling.SyncPoller;
import info.rio.project_ai901.services.ai.dto.InvoiceOcrResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "app.ai.di.provider", havingValue = "azure")
public class AzureDocumentIntelligenceService implements DocumentIntelligenceService {

    private static final String INVOICE_MODEL = "prebuilt-invoice";

    private final DocumentIntelligenceClient client;

    public AzureDocumentIntelligenceService(
            @Value("${app.ai.di.endpoint:}") String endpoint,
            @Value("${app.ai.di.key:}") String key) {
        if (endpoint == null || endpoint.isBlank() || key == null || key.isBlank()) {
            throw new IllegalStateException(
                    "app.ai.di.endpoint/key no estan configurados (AZURE_DOC_INTELLIGENCE_ENDPOINT / AZURE_DOC_INTELLIGENCE_KEY)");
        }
        this.client = new DocumentIntelligenceClientBuilder()
                .endpoint(endpoint)
                .credential(new AzureKeyCredential(key))
                .buildClient();
    }

    @Override
    public InvoiceOcrResult analyzeInvoice(InputStream content) {
        AnalyzeDocumentOptions options = new AnalyzeDocumentOptions(BinaryData.fromBytes(readAll(content)));
        SyncPoller<AnalyzeOperationDetails, AnalyzeResult> poller = client.beginAnalyzeDocument(INVOICE_MODEL, options);
        AnalyzeResult analyzeResult = poller.getFinalResult();

        InvoiceOcrResult result = new InvoiceOcrResult();
        List<String> missing = new ArrayList<>();

        AnalyzedDocument invoice = analyzeResult.getDocuments().isEmpty()
                ? null
                : analyzeResult.getDocuments().getFirst();
        Map<String, DocumentField> fields = invoice == null ? Map.of() : invoice.getFields();

        String vendorName = stringValue(fields.get("VendorName"));
        LocalDate invoiceDate = dateValue(fields.get("InvoiceDate"));
        BigDecimal invoiceTotal = currencyValue(fields.get("InvoiceTotal"));
        String invoiceNumber = stringValue(fields.get("InvoiceId"));
        if (invoiceNumber == null) {
            invoiceNumber = stringValue(fields.get("InvoiceNumber"));
        }

        if (vendorName == null) {
            missing.add("vendorName");
        }
        if (invoiceNumber == null) {
            missing.add("invoiceNumber");
        }
        if (invoiceDate == null) {
            missing.add("invoiceDate");
        }
        if (invoiceTotal == null) {
            missing.add("invoiceTotal");
        }

        result.setVendorName(vendorName);
        result.setInvoiceNumber(invoiceNumber);
        result.setInvoiceDate(invoiceDate);
        result.setInvoiceTotal(invoiceTotal);
        result.setConfidence(averageConfidence(fields,
                List.of("VendorName", "InvoiceId", "InvoiceNumber", "InvoiceDate", "InvoiceTotal")));
        result.setMissingFields(missing);
        return result;
    }

    private BigDecimal averageConfidence(Map<String, DocumentField> fields, List<String> keys) {
        List<Double> confidences = new ArrayList<>();
        for (String key : keys) {
            DocumentField field = fields.get(key);
            if (field != null && field.getConfidence() != null) {
                confidences.add(field.getConfidence());
            }
        }
        if (confidences.isEmpty()) {
            return BigDecimal.ZERO;
        }
        double avg = confidences.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        return BigDecimal.valueOf(avg).setScale(3, RoundingMode.HALF_UP);
    }

    private String stringValue(DocumentField field) {
        if (field == null || field.getValueString() == null || field.getValueString().isBlank()) {
            return null;
        }
        return field.getValueString();
    }

    private LocalDate dateValue(DocumentField field) {
        return field == null ? null : field.getValueDate();
    }

    private BigDecimal currencyValue(DocumentField field) {
        if (field == null) {
            return null;
        }
        CurrencyValue currency = field.getValueCurrency();
        return currency == null ? null : BigDecimal.valueOf(currency.getAmount());
    }

    private byte[] readAll(InputStream content) {
        try (InputStream in = content) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer el contenido del documento", e);
        }
    }
}
