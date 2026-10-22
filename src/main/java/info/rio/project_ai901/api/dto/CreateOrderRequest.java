package info.rio.project_ai901.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(
        @NotNull(message = "El cliente es obligatorio")
        UUID customerId,

        @NotNull(message = "La sucursal es obligatoria")
        UUID branchId,

        LocalDateTime orderDate,

        LocalDate estimatedDeliveryDate,

        @NotEmpty(message = "La orden debe tener al menos un item")
        @Valid
        List<CreateOrderItemRequest> items) {
}
