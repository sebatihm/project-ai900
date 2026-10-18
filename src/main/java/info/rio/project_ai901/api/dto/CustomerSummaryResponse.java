package info.rio.project_ai901.api.dto;

import info.rio.project_ai901.models.enums.CustomerType;

import java.util.UUID;

public record CustomerSummaryResponse(
        UUID id,
        String name,
        CustomerType type,
        String email,
        String phone) {
}
