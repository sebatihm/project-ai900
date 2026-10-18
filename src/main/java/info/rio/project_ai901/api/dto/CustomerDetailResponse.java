package info.rio.project_ai901.api.dto;

import info.rio.project_ai901.models.enums.CustomerType;

import java.util.UUID;

public record CustomerDetailResponse(
        UUID id,
        String name,
        CustomerType type,
        String email,
        String phone,
        UUID branchId,
        String branchName) {
}
