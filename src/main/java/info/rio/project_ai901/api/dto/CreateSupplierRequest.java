package info.rio.project_ai901.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSupplierRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String name,

        @NotBlank(message = "El RFC es obligatorio")
        @Size(max = 13, message = "El RFC no puede superar 13 caracteres")
        String rfc,

        @Email(message = "El correo no tiene un formato valido")
        @Size(max = 255, message = "El correo no puede superar 255 caracteres")
        String email) {
}
