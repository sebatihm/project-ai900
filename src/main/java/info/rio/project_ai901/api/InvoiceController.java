package info.rio.project_ai901.api;

import info.rio.project_ai901.api.dto.InvoiceDetailResponse;
import info.rio.project_ai901.api.dto.InvoiceSummaryResponse;
import info.rio.project_ai901.api.dto.ReviewRequest;
import info.rio.project_ai901.models.enums.InvoiceStatus;
import info.rio.project_ai901.services.invoice.InvoiceQueryService;
import info.rio.project_ai901.services.invoice.InvoiceReviewService;
import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import java.util.Map;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/invoices")
public class InvoiceController {

    private final InvoiceQueryService invoiceQueryService;
    private final InvoiceReviewService invoiceReviewService;

    public InvoiceController(InvoiceQueryService invoiceQueryService, InvoiceReviewService invoiceReviewService) {
        this.invoiceQueryService = invoiceQueryService;
        this.invoiceReviewService = invoiceReviewService;
    }

    @GetMapping
    public info.rio.project_ai901.api.dto.PaginatedResponse<InvoiceSummaryResponse> list(
            @RequestParam(value = "status", required = false) InvoiceStatus status,
            @RequestParam(value = "offset", defaultValue = "0") long offset,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return invoiceQueryService.list(status, offset, limit);
    }

    @GetMapping("/{id}")
    public InvoiceDetailResponse detail(@PathVariable("id") UUID id) {
        return invoiceQueryService.detail(id);
    }

    @PostMapping("/{id}/review")
    public InvoiceDetailResponse review(@PathVariable("id") UUID id, @Valid @RequestBody ReviewRequest request) {
        return invoiceReviewService.review(id, request);
    }

    @GetMapping("/{id}/file-url")
    public Map<String, String> fileUrl(@PathVariable("id") UUID id) {
        return Map.of("readUrl", invoiceQueryService.readUrl(id));
    }
}
