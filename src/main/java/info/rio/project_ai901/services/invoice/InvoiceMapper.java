package info.rio.project_ai901.services.invoice;

import info.rio.project_ai901.api.dto.InvoiceAnalysisResponse;
import info.rio.project_ai901.api.dto.InvoiceDetailResponse;
import info.rio.project_ai901.api.dto.InvoiceEventResponse;
import info.rio.project_ai901.api.dto.InvoiceItemResponse;
import info.rio.project_ai901.api.dto.InvoiceSummaryResponse;
import info.rio.project_ai901.models.Invoice;
import info.rio.project_ai901.models.InvoiceEvent;
import info.rio.project_ai901.models.InvoiceItem;
import info.rio.project_ai901.services.invoice.dto.SuspicionReason;
import info.rio.project_ai901.services.storage.StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;

@Component
public class InvoiceMapper {

    private final StorageService storageService;
    private final ObjectMapper objectMapper;
    private final Duration sasTtl;

    public InvoiceMapper(StorageService storageService, ObjectMapper objectMapper,
            @Value("${app.storage.sas-ttl:PT10M}") Duration sasTtl) {
        this.storageService = storageService;
        this.objectMapper = objectMapper;
        this.sasTtl = sasTtl;
    }

    public List<SuspicionReason> parseReasons(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        return objectMapper.readValue(json, new TypeReference<List<SuspicionReason>>() {
        });
    }

    public InvoiceAnalysisResponse toAnalysis(Invoice invoice, List<SuspicionReason> reasons,
            List<InvoiceItemResponse> items) {
        return new InvoiceAnalysisResponse(invoice.getId(), invoice.getStatus(), invoice.getSuspicionScore(), reasons,
                supplierId(invoice), supplierName(invoice), invoice.getInvoiceNumber(), invoice.getInvoiceDate(),
                invoice.getDueDate(), invoice.getSubtotal(), invoice.getTax(), invoice.getTotal(),
                invoice.getCurrency(), invoice.getMinConfidence(), invoice.getOriginalFileName(),
                invoice.getFileHash(), invoice.getContainer(), invoice.getBlobPath(), readUrl(invoice), items,
                invoice.getCreatedAt());
    }

    public InvoiceSummaryResponse toSummary(Invoice invoice) {
        return new InvoiceSummaryResponse(invoice.getId(), invoice.getStatus(), invoice.getSuspicionScore(),
                supplierName(invoice), invoice.getInvoiceNumber(), invoice.getInvoiceDate(), invoice.getTotal(),
                invoice.getCurrency(), invoice.getOriginalFileName(), invoice.getCreatedAt());
    }

    public InvoiceDetailResponse toDetail(Invoice invoice, List<SuspicionReason> reasons,
            List<InvoiceItemResponse> items, List<InvoiceEventResponse> events) {
        return new InvoiceDetailResponse(invoice.getId(), invoice.getStatus(), invoice.getSuspicionScore(), reasons,
                supplierId(invoice), supplierName(invoice), invoice.getInvoiceNumber(), invoice.getInvoiceDate(),
                invoice.getDueDate(), invoice.getSubtotal(), invoice.getTax(), invoice.getTotal(),
                invoice.getCurrency(), invoice.getMinConfidence(), invoice.getOriginalFileName(),
                invoice.getFileHash(), invoice.getContainer(), invoice.getBlobPath(), readUrl(invoice), items, events,
                userEmail(invoice.getLoadedBy()), userEmail(invoice.getReviewedBy()), invoice.getCreatedAt(),
                invoice.getReviewedAt());
    }

    public List<InvoiceItemResponse> toItems(List<InvoiceItem> items) {
        return items.stream()
                .map(item -> new InvoiceItemResponse(item.getDescription(), item.getQuantity(), item.getAmount()))
                .toList();
    }

    public List<InvoiceEventResponse> toEvents(List<InvoiceEvent> events) {
        return events.stream()
                .map(event -> new InvoiceEventResponse(event.getType(), userEmail(event.getActor()), event.getDetail(),
                        event.getCreatedAt()))
                .toList();
    }

    public String readUrl(Invoice invoice) {
        if (invoice.getContainer() == null || invoice.getBlobPath() == null) {
            return null;
        }
        return storageService.readUrl(invoice.getContainer(), invoice.getBlobPath(), sasTtl);
    }

    private java.util.UUID supplierId(Invoice invoice) {
        return invoice.getSupplier() == null ? null : invoice.getSupplier().getId();
    }

    private String supplierName(Invoice invoice) {
        return invoice.getSupplier() == null ? null : invoice.getSupplier().getName();
    }

    private String userEmail(info.rio.project_ai901.models.User user) {
        return user == null ? null : user.getEmail();
    }
}
