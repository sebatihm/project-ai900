package info.rio.project_ai901.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "La contrasena actual es obligatoria")
        String oldPassword,

        @NotBlank(message = "La nueva contrasena es obligatoria")
        @Size(min = 8, message = "La contrasena debe tener al menos 8 caracteres")
        @Size(max = 100, message = "La contrasena no puede superar 100 caracteres")
        String newPassword) {
}
