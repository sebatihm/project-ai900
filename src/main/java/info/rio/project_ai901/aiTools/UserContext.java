package info.rio.project_ai901.aiTools;

import java.util.Set;
import java.util.UUID;

public record UserContext(UUID userId, String name, String role, UUID warehouseId) {

    public static UserContext anonymous() {
        return new UserContext(null, null, "ANONYMOUS", null);
    }

    public boolean isAuthenticated() { return userId != null; }

    public boolean hasAnyRole(Set<String> roles) {
        return roles.isEmpty() || roles.contains(role);
    }
}