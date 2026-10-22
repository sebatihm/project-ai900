package info.rio.project_ai901.repositories;

import info.rio.project_ai901.models.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    long countByCustomer_Id(UUID customerId);

    long countByOrder_Id(UUID orderId);
}
