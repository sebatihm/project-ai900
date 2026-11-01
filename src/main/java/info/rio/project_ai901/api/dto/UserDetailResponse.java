package info.rio.project_ai901.api.dto;

import info.rio.project_ai901.models.enums.RoleName;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserDetailResponse(UUID id, String name, String email, RoleName role, UUID branchId, String branchName,
        UUID warehouseId, String warehouseName, boolean active, LocalDateTime createdAt, LocalDateTime updatedAt) {
}
