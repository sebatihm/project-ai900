package info.rio.project_ai901.api.dto;

import java.util.UUID;

public record WarehouseSummaryResponse(UUID id, String code, String name, UUID branchId, String branchName) {
}
