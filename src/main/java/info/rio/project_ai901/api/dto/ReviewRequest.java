package info.rio.project_ai901.api.dto;

public record ReviewRequest(ReviewAction action, String reviewedBy, String comment) {
}
