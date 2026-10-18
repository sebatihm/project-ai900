package info.rio.project_ai901.services.ai.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class InvoiceOcrResult {

    private String vendorName;
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal total;
    private String currency;
    private List<InvoiceItemData> items = new ArrayList<>();
    private Map<String, BigDecimal> confidenceByField = new LinkedHashMap<>();
    private BigDecimal minConfidence = BigDecimal.ZERO;
    private int documentCount;
    private boolean analysisFailed;
    private String analysisError;
    private String rawJson;

    public boolean isIdentifiedAsInvoice() {
        return documentCount > 0;
    }
}
