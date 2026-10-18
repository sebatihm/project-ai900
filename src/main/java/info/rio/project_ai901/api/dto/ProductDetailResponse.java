package info.rio.project_ai901.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductDetailResponse(
        UUID id,
        String sku,
        String name,
        String unit,
        BigDecimal price,
        UUID categoryId,
        String categoryName) {
}
