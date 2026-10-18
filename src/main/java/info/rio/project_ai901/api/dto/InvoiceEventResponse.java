package info.rio.project_ai901.api.dto;

import info.rio.project_ai901.models.enums.InvoiceEventType;

import java.time.LocalDateTime;

public record InvoiceEventResponse(InvoiceEventType type, String actorEmail, String detail,
        LocalDateTime createdAt) {
}
