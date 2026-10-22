package info.rio.project_ai901.repositories;

import info.rio.project_ai901.models.TicketMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TicketMessageRepository extends JpaRepository<TicketMessage, UUID> {

    List<TicketMessage> findByTicket_IdOrderBySentAtAsc(UUID ticketId);

    long countByTicket_Id(UUID ticketId);

    void deleteByTicket_Id(UUID ticketId);
}
