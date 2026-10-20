package info.rio.project_ai901.services.ticket;

import info.rio.project_ai901.api.dto.TicketDetailResponse;
import info.rio.project_ai901.api.dto.TicketMessageResponse;
import info.rio.project_ai901.api.dto.TicketSummaryResponse;
import info.rio.project_ai901.models.Customer;
import info.rio.project_ai901.models.Ticket;
import info.rio.project_ai901.models.TicketMessage;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TicketMapper {

    public TicketSummaryResponse toSummary(Ticket ticket) {
        return new TicketSummaryResponse(
                ticket.getId(),
                ticket.getSubject(),
                ticket.getStatus(),
                ticket.getChannel(),
                ticket.getTopic(),
                ticket.getSentiment(),
                ticket.getHandledBy(),
                ticket.isRequiresHumanReview(),
                customerId(ticket),
                customerName(ticket),
                orderId(ticket),
                ticket.getCreatedAt());
    }

    public TicketDetailResponse toDetail(Ticket ticket, long messageCount) {
        return new TicketDetailResponse(
                ticket.getId(),
                ticket.getSubject(),
                ticket.getStatus(),
                ticket.getChannel(),
                ticket.getTopic(),
                ticket.getSentiment(),
                ticket.getHandledBy(),
                ticket.isRequiresHumanReview(),
                ticket.getRating(),
                customerId(ticket),
                customerName(ticket),
                orderId(ticket),
                ticket.getCreatedAt(),
                (int) messageCount);
    }

    public TicketMessageResponse toMessage(TicketMessage message) {
        return new TicketMessageResponse(
                message.getId(),
                message.getTicket().getId(),
                message.getAuthor(),
                message.getContent(),
                message.getAction(),
                message.getSources(),
                message.getSentAt());
    }

    private UUID customerId(Ticket ticket) {
        Customer customer = ticket.getCustomer();
        return customer != null ? customer.getId() : null;
    }

    private String customerName(Ticket ticket) {
        Customer customer = ticket.getCustomer();
        return customer != null ? customer.getName() : null;
    }

    private UUID orderId(Ticket ticket) {
        return ticket.getOrder() != null ? ticket.getOrder().getId() : null;
    }
}
