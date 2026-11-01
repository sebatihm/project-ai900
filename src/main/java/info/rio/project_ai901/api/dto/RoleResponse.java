package info.rio.project_ai901.api.dto;

import info.rio.project_ai901.models.enums.RoleName;

import java.util.UUID;

public record RoleResponse(UUID id, RoleName name) {
}
