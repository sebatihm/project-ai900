package info.rio.project_ai901.api.dto;

import java.util.UUID;

/**
 * Asignacion de un ticket a un usuario del modulo de soporte. Un
 * {@code assignedToId} nulo desasigna el ticket.
 */
public record AssignTicketRequest(UUID assignedToId) {
}
