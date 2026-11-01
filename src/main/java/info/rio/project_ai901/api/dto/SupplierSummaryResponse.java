package info.rio.project_ai901.api.dto;

import java.util.UUID;

public record SupplierSummaryResponse(
        UUID id,
        String name,
        String rfc,
        String email,
        String invoiceAnalyzerUrl) {
}
