package info.rio.project_ai901.api.dto;

import java.math.BigDecimal;

public record InvoiceItemResponse(String description, BigDecimal quantity, BigDecimal amount) {
}
