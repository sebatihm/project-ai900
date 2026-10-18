package info.rio.project_ai901.api.dto;

import info.rio.project_ai901.models.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(
        @NotNull(message = "El estado es obligatorio")
        OrderStatus status) {
}
