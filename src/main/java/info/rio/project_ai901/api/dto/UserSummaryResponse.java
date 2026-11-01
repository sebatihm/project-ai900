package info.rio.project_ai901.api.dto;

import info.rio.project_ai901.models.enums.RoleName;

import java.util.UUID;

public record UserSummaryResponse(UUID id, String name, String email, RoleName role, UUID branchId, String branchName,
        UUID warehouseId, String warehouseName) {
}
