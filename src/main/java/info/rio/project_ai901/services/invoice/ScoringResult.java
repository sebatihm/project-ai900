package info.rio.project_ai901.services.invoice;

import info.rio.project_ai901.services.invoice.dto.SuspicionReason;

import java.util.List;

public record ScoringResult(int score, List<SuspicionReason> reasons) {
}
