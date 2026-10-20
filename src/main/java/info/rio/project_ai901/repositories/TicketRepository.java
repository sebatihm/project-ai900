package info.rio.project_ai901.repositories;

import info.rio.project_ai901.models.Ticket;
import info.rio.project_ai901.models.enums.HandledBy;
import info.rio.project_ai901.models.enums.TicketStatus;
import info.rio.project_ai901.models.enums.TicketTopic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    long countByCustomer_Id(UUID customerId);

    long countByOrder_Id(UUID orderId);

    @Query("select t from Ticket t where (:customerId is null or t.customer.id = :customerId) "
            + "and (:status is null or t.status = :status) "
            + "and (:topic is null or t.topic = :topic) "
            + "and (:handledBy is null or t.handledBy = :handledBy)")
    Page<Ticket> search(@Param("customerId") UUID customerId, @Param("status") TicketStatus status,
            @Param("topic") TicketTopic topic, @Param("handledBy") HandledBy handledBy, Pageable pageable);
}
