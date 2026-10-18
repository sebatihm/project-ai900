package info.rio.project_ai901.api.dto;

import java.util.UUID;

public record InventorySummaryResponse(
        UUID id,
        UUID warehouseId,
        String warehouseCode,
        String warehouseName,
        UUID productId,
        String productSku,
        String productName,
        Integer quantity) {
}
