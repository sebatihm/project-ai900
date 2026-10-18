package info.rio.project_ai901.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateProductRequest(
        @NotBlank(message = "El SKU es obligatorio")
        @Size(max = 40, message = "El SKU no puede superar 40 caracteres")
        String sku,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String name,

        @NotNull(message = "La categoria es obligatoria")
        UUID categoryId,

        @NotBlank(message = "La unidad es obligatoria")
        @Size(max = 20, message = "La unidad no puede superar 20 caracteres")
        String unit,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.01", inclusive = true, message = "El precio debe ser mayor a 0")
        @Digits(integer = 10, fraction = 2, message = "El precio debe tener maximo 10 enteros y 2 decimales")
        BigDecimal price) {
}
