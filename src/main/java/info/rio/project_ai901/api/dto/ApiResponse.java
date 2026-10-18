package info.rio.project_ai901.api.dto;

import java.time.Instant;

public record ApiResponse<T>(
        Instant timestamp,
        int status,
        String message,
        T data
) {
    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(Instant.now(), 200, message, data);
    }

    public static <T> ApiResponse<T> created(T data, String message) {
        return new ApiResponse<>(Instant.now(), 201, message, data);
    }
}