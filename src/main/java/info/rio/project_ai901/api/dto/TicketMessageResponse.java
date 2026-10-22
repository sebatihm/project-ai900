package info.rio.project_ai901.api.dto;

import info.rio.project_ai901.models.enums.MessageAction;
import info.rio.project_ai901.models.enums.MessageAuthor;

import java.time.LocalDateTime;
import java.util.UUID;

public record TicketMessageResponse(
        UUID id,
        UUID ticketId,
        MessageAuthor author,
        String content,
        MessageAction action,
        String sources,
        LocalDateTime sentAt) {
}
