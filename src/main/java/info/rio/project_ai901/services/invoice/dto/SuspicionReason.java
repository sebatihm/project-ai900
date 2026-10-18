package info.rio.project_ai901.services.invoice.dto;

import info.rio.project_ai901.models.enums.SuspicionRule;

public record SuspicionReason(SuspicionRule rule, int points, String detail) {
}
