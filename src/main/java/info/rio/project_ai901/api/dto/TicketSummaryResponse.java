package info.rio.project_ai901.api.dto;

import info.rio.project_ai901.models.enums.HandledBy;
import info.rio.project_ai901.models.enums.TicketChannel;
import info.rio.project_ai901.models.enums.TicketSentiment;
import info.rio.project_ai901.models.enums.TicketStatus;
import info.rio.project_ai901.models.enums.TicketTopic;

import java.time.LocalDateTime;
import java.util.UUID;

public record TicketSummaryResponse(
        UUID id,
        String subject,
        TicketStatus status,
        TicketChannel channel,
        TicketTopic topic,
        TicketSentiment sentiment,
        HandledBy handledBy,
        boolean requiresHumanReview,
        UUID customerId,
        String customerName,
        UUID orderId,
        LocalDateTime createdAt) {
}
