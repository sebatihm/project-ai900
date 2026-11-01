package info.rio.project_ai901.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewRequest(
        @NotNull(message = "La accion es obligatoria")
        ReviewAction action,

        @Size(max = 500, message = "El comentario no puede superar 500 caracteres")
        String comment) {
}
