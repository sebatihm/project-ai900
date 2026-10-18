package info.rio.project_ai901.services.invoice;

import info.rio.project_ai901.api.dto.InvoiceDetailResponse;
import info.rio.project_ai901.api.dto.InvoiceSummaryResponse;
import info.rio.project_ai901.exceptions.ResourceNotFoundException;
import info.rio.project_ai901.models.Invoice;
import info.rio.project_ai901.models.enums.InvoiceStatus;
import info.rio.project_ai901.repositories.InvoiceEventRepository;
import info.rio.project_ai901.repositories.InvoiceItemRepository;
import info.rio.project_ai901.repositories.InvoiceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class InvoiceQueryService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final InvoiceEventRepository invoiceEventRepository;
    private final InvoiceMapper mapper;

    public InvoiceQueryService(InvoiceRepository invoiceRepository, InvoiceItemRepository invoiceItemRepository,
            InvoiceEventRepository invoiceEventRepository, InvoiceMapper mapper) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.invoiceEventRepository = invoiceEventRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public info.rio.project_ai901.api.dto.PaginatedResponse<InvoiceSummaryResponse> list(InvoiceStatus status,
            long offset, int limit) {
        if (limit < 1) {
            limit = 1;
        }
        if (limit > 100) {
            limit = 100;
        }
        if (offset < 0) {
            offset = 0;
        }
        int page = (int) (offset / limit);
        int size = limit;
        org.springframework.data.domain.PageRequest pageable = org.springframework.data.domain.PageRequest.of(page, size,
                org.springframework.data.domain.Sort.by("createdAt").descending());
        org.springframework.data.domain.Page<Invoice> pageResult = status == null ? invoiceRepository.findAll(pageable)
                : invoiceRepository.findByStatus(status, pageable);
        java.util.List<InvoiceSummaryResponse> items = pageResult.getContent().stream()
                .map(mapper::toSummary)
                .toList();
        long total = pageResult.getTotalElements();
        return new info.rio.project_ai901.api.dto.PaginatedResponse<>(items, total, offset, limit);
    }

    @Transactional(readOnly = true)
    public InvoiceDetailResponse detail(UUID id) {
        Invoice invoice = find(id);
        return mapper.toDetail(invoice, mapper.parseReasons(invoice.getReasons()),
                mapper.toItems(invoiceItemRepository.findByInvoice_Id(id)),
                mapper.toEvents(invoiceEventRepository.findByInvoice_IdOrderByCreatedAtAsc(id)));
    }

    @Transactional(readOnly = true)
    public String readUrl(UUID id) {
        return mapper.readUrl(find(id));
    }

    private Invoice find(UUID id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Factura no encontrada: " + id));
    }
}
