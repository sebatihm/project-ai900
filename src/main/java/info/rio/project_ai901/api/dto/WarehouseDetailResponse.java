package info.rio.project_ai901.api.dto;

import java.util.UUID;

public record WarehouseDetailResponse(UUID warehouseId, String code, String name, boolean active, UUID branchId,
        String branchName, String branchCity) {
}
