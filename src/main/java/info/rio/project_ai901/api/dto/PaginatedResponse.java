package info.rio.project_ai901.api.dto;

import java.util.List;

public record PaginatedResponse<T>(List<T> items, long total, long offset, int limit) {
}