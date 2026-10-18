package info.rio.project_ai901.api.dto;

import java.util.UUID;

public record BranchDetailResponse(UUID branchId, String code, String name, String city, boolean active,
        long warehouseCount) {
}
