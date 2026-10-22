package info.rio.project_ai901.api.dto;

import info.rio.project_ai901.models.enums.MessageAction;
import info.rio.project_ai901.models.enums.MessageAuthor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTicketMessageRequest(
        @NotNull(message = "El autor es obligatorio")
        MessageAuthor author,

        @NotBlank(message = "El contenido es obligatorio")
        @Size(max = 2000, message = "El contenido no puede superar 2000 caracteres")
        String content,

        MessageAction action,

        @Size(max = 500, message = "Las fuentes no pueden superar 500 caracteres")
        String sources) {
}
