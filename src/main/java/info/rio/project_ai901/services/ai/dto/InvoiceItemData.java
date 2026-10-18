package info.rio.project_ai901.services.ai.dto;

import java.math.BigDecimal;

public record InvoiceItemData(String description, BigDecimal quantity, BigDecimal amount) {
}
