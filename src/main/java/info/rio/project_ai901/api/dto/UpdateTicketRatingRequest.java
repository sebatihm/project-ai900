package info.rio.project_ai901.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateTicketRatingRequest(
        @NotNull(message = "La calificacion es obligatoria")
        @Min(value = 1, message = "La calificacion debe estar entre 1 y 5")
        @Max(value = 5, message = "La calificacion debe estar entre 1 y 5")
        Integer rating) {
}
