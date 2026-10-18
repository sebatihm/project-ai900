package info.rio.project_ai901.api.dto;

import info.rio.project_ai901.models.enums.CustomerType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateCustomerRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String name,

        @NotNull(message = "El tipo de cliente es obligatorio")
        CustomerType type,

        @Email(message = "El correo no tiene un formato valido")
        @Size(max = 255, message = "El correo no puede superar 255 caracteres")
        String email,

        @Size(max = 20, message = "El telefono no puede superar 20 caracteres")
        String phone,

        UUID branchId) {
}
