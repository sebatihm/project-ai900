package info.rio.project_ai901.repositories;

import info.rio.project_ai901.models.InvoiceEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InvoiceEventRepository extends JpaRepository<InvoiceEvent, UUID> {

    List<InvoiceEvent> findByInvoice_IdOrderByCreatedAtAsc(UUID invoiceId);
}
