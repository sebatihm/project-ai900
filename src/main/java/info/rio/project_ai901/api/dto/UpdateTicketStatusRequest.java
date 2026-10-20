package info.rio.project_ai901.api.dto;

import info.rio.project_ai901.models.enums.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTicketStatusRequest(
        @NotNull(message = "El estado es obligatorio")
        TicketStatus status) {
}
