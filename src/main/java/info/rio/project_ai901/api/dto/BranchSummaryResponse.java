package info.rio.project_ai901.api.dto;

import java.util.UUID;

public record BranchSummaryResponse(UUID id, String code, String name, String city) {
}
