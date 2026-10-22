package info.rio.project_ai901.api.dto;

import java.time.Instant;

public record LoginResponse(String token, String tokenType, Instant expiresAt, UserSummaryResponse user) {
}
