package info.rio.project_ai901.services.report.dto;

import java.util.UUID;

public record ReportRequester(UUID userId, String name, String role, UUID warehouseId) {}