package info.rio.project_ai901.services.ai.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class InvoiceOcrResult {

    private String vendorName;
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private BigDecimal invoiceTotal;
    private BigDecimal confidence;
    private List<String> missingFields = new ArrayList<>();
}
