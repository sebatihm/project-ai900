package info.rio.project_ai901.services.ai;

import com.azure.ai.documentintelligence.DocumentIntelligenceClient;
import com.azure.ai.documentintelligence.models.AnalyzeDocumentOptions;
import com.azure.ai.documentintelligence.models.AnalyzeOperationDetails;
import com.azure.ai.documentintelligence.models.AnalyzeResult;
import com.azure.ai.documentintelligence.models.AnalyzedDocument;
import com.azure.ai.documentintelligence.models.CurrencyValue;
import com.azure.ai.documentintelligence.models.DocumentField;
import com.azure.core.util.BinaryData;
import com.azure.core.util.polling.SyncPoller;
import com.azure.json.JsonProviders;

import info.rio.project_ai901.services.ai.dto.InvoiceItemData;
import info.rio.project_ai901.services.ai.dto.InvoiceOcrResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.azure.json.JsonWriter;
@Service
@ConditionalOnProperty(name = "app.ai.di.provider", havingValue = "azure")
public class AzureDocumentIntelligenceService implements DocumentIntelligenceService {

    private static final String INVOICE_MODEL = "prebuilt-invoice";

    private final DocumentIntelligenceClient client;

    public AzureDocumentIntelligenceService(DocumentIntelligenceClient client) {
        this.client = client;
    }

    @Override
    public InvoiceOcrResult analyzeInvoice(InputStream content) {
        AnalyzeDocumentOptions options = new AnalyzeDocumentOptions(BinaryData.fromBytes(readAll(content)));
        SyncPoller<AnalyzeOperationDetails, AnalyzeResult> poller = client.beginAnalyzeDocument(INVOICE_MODEL, options);
        AnalyzeResult analyzeResult = poller.getFinalResult();

        InvoiceOcrResult result = new InvoiceOcrResult();
        result.setDocumentCount(analyzeResult.getDocuments().size());
        result.setRawJson(toJson(analyzeResult));

        if (analyzeResult.getDocuments().isEmpty()) {
            return result;
        }

        AnalyzedDocument invoice = analyzeResult.getDocuments().getFirst();
        Map<String, DocumentField> fields = invoice.getFields() == null ? Map.of() : invoice.getFields();

        result.setVendorName(stringValue(fields.get("VendorName")));
        result.setInvoiceNumber(
                firstNonNull(stringValue(fields.get("InvoiceId")), stringValue(fields.get("InvoiceNumber"))));
        result.setInvoiceDate(dateValue(fields.get("InvoiceDate")));
        result.setDueDate(dateValue(fields.get("DueDate")));
        result.setSubtotal(amountValue(fields.get("SubTotal")));
        result.setTax(amountValue(fields.get("TotalTax")));
        result.setTotal(amountValue(fields.get("InvoiceTotal")));
        result.setCurrency(currencyCode(fields.get("InvoiceTotal")));
        result.setItems(items(fields.get("Items")));

        Map<String, BigDecimal> confidences = new LinkedHashMap<>();
        putConfidence(confidences, "vendorName", fields.get("VendorName"));
        putConfidence(confidences, "invoiceNumber", fields.get("InvoiceId"));
        putConfidence(confidences, "invoiceDate", fields.get("InvoiceDate"));
        putConfidence(confidences, "dueDate", fields.get("DueDate"));
        putConfidence(confidences, "subtotal", fields.get("SubTotal"));
        putConfidence(confidences, "tax", fields.get("TotalTax"));
        putConfidence(confidences, "total", fields.get("InvoiceTotal"));
        result.setConfidenceByField(confidences);
        result.setMinConfidence(minOf(confidences, "invoiceNumber", "invoiceDate", "total"));
        return result;
    }

private String toJson(AnalyzeResult analyzeResult) {
    try (StringWriter out = new StringWriter();
         JsonWriter writer = JsonProviders.createWriter(out)) {
        analyzeResult.toJson(writer);
        writer.flush();
        return out.toString();
    } catch (Exception e) {
        return null;
    }
}

    private List<InvoiceItemData> items(DocumentField field) {
        List<InvoiceItemData> result = new ArrayList<>();
        if (field == null || field.getValueList() == null) {
            return result;
        }
        for (DocumentField item : field.getValueList()) {
            Map<String, DocumentField> map = item.getValueMap();
            if (map == null) {
                continue;
            }
            result.add(new InvoiceItemData(stringValue(map.get("Description")), numberValue(map.get("Quantity")),
                    amountValue(map.get("Amount"))));
        }
        return result;
    }

    private void putConfidence(Map<String, BigDecimal> target, String key, DocumentField field) {
        if (field != null && field.getConfidence() != null) {
            target.put(key, BigDecimal.valueOf(field.getConfidence()));
        }
    }

    private BigDecimal minOf(Map<String, BigDecimal> confidences, String... keys) {
        BigDecimal min = null;
        for (String key : keys) {
            BigDecimal value = confidences.get(key);
            if (value != null && (min == null || value.compareTo(min) < 0)) {
                min = value;
            }
        }
        return min == null ? BigDecimal.ZERO : min;
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

    private BigDecimal numberValue(DocumentField field) {
        if (field == null || field.getValueNumber() == null) {
            return null;
        }
        return BigDecimal.valueOf(field.getValueNumber());
    }

    private BigDecimal amountValue(DocumentField field) {
        if (field == null) {
            return null;
        }
        CurrencyValue currency = field.getValueCurrency();
        if (currency != null) {
            return BigDecimal.valueOf(currency.getAmount());
        }
        return numberValue(field);
    }

    private String currencyCode(DocumentField field) {
        if (field == null || field.getValueCurrency() == null) {
            return null;
        }
        return field.getValueCurrency().getCurrencyCode();
    }

    private String firstNonNull(String first, String second) {
        return first != null ? first : second;
    }

    private byte[] readAll(InputStream content) {
        try (InputStream in = content) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer el contenido del documento", e);
        }
    }
}
