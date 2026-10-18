package info.rio.project_ai901.services.invoice;

import info.rio.project_ai901.models.Invoice;
import info.rio.project_ai901.models.InvoiceEvent;
import info.rio.project_ai901.models.User;
import info.rio.project_ai901.models.enums.InvoiceEventType;
import info.rio.project_ai901.repositories.InvoiceEventRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class InvoiceEventRecorder {

    private static final int MAX_DETAIL_LENGTH = 500;

    private final InvoiceEventRepository invoiceEventRepository;

    public InvoiceEventRecorder(InvoiceEventRepository invoiceEventRepository) {
        this.invoiceEventRepository = invoiceEventRepository;
    }

    public InvoiceEvent record(Invoice invoice, User actor, InvoiceEventType type, String detail) {
        InvoiceEvent event = new InvoiceEvent();
        event.setInvoice(invoice);
        event.setActor(actor);
        event.setType(type);
        event.setDetail(truncate(detail));
        event.setCreatedAt(LocalDateTime.now());
        return invoiceEventRepository.save(event);
    }

    private String truncate(String detail) {
        if (detail == null) {
            return null;
        }
        return detail.length() <= MAX_DETAIL_LENGTH ? detail : detail.substring(0, MAX_DETAIL_LENGTH);
    }
}
