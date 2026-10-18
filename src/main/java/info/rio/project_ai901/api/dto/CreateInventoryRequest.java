package info.rio.project_ai901.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

public record CreateInventoryRequest(
        @NotNull(message = "El almacen es obligatorio")
        UUID warehouseId,

        @NotNull(message = "El producto es obligatorio")
        UUID productId,

        @NotNull(message = "La cantidad es obligatoria")
        @PositiveOrZero(message = "La cantidad no puede ser negativa")
        Integer quantity) {
}
