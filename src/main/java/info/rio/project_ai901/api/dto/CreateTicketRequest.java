package info.rio.project_ai901.api.dto;

import info.rio.project_ai901.models.enums.HandledBy;
import info.rio.project_ai901.models.enums.TicketChannel;
import info.rio.project_ai901.models.enums.TicketSentiment;
import info.rio.project_ai901.models.enums.TicketTopic;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateTicketRequest(
        @NotNull(message = "El cliente es obligatorio")
        UUID customerId,

        UUID orderId,

        @NotBlank(message = "El asunto es obligatorio")
        @Size(max = 200, message = "El asunto no puede superar 200 caracteres")
        String subject,

        @NotNull(message = "El canal es obligatorio")
        TicketChannel channel,

        TicketTopic topic,

        TicketSentiment sentiment,

        HandledBy handledBy,

        Boolean requiresHumanReview,

        @Valid
        CreateTicketMessageRequest initialMessage) {
}
