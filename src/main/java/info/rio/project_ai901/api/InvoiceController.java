package info.rio.project_ai901.api;

import info.rio.project_ai901.api.dto.ApiResponse;
import info.rio.project_ai901.api.dto.InvoiceDetailResponse;
import info.rio.project_ai901.api.dto.InvoiceSummaryResponse;
import info.rio.project_ai901.api.dto.PaginatedResponse;
import info.rio.project_ai901.api.dto.ReviewRequest;
import info.rio.project_ai901.models.enums.InvoiceStatus;
import info.rio.project_ai901.services.invoice.InvoiceQueryService;
import info.rio.project_ai901.services.invoice.InvoiceReviewService;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
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
@RequestMapping("/invoice")
public class InvoiceController {

    private final InvoiceQueryService invoiceQueryService;
    private final InvoiceReviewService invoiceReviewService;

    public InvoiceController(InvoiceQueryService invoiceQueryService, InvoiceReviewService invoiceReviewService) {
        this.invoiceQueryService = invoiceQueryService;
        this.invoiceReviewService = invoiceReviewService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PaginatedResponse<InvoiceSummaryResponse>>> list(
            @RequestParam(value = "status", required = false) InvoiceStatus status,
            @RequestParam(value = "offset", defaultValue = "0") long offset,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        PaginatedResponse<InvoiceSummaryResponse> result = invoiceQueryService.list(status, offset, limit);
        return ResponseEntity.ok(ApiResponse.ok(result, "Listado de facturas obtenido correctamente"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceDetailResponse>> detail(@PathVariable("id") UUID id) {
        InvoiceDetailResponse result = invoiceQueryService.detail(id);
        return ResponseEntity.ok(ApiResponse.ok(result, "Detalle de factura obtenido correctamente"));
    }

    @PostMapping("/{id}/review")
    public ResponseEntity<ApiResponse<InvoiceDetailResponse>> review(@PathVariable("id") UUID id,
            @Valid @RequestBody ReviewRequest request) {
        InvoiceDetailResponse result = invoiceReviewService.review(id, request);
        return ResponseEntity.ok(ApiResponse.ok(result, "Revisión de factura registrada correctamente"));
    }

    @GetMapping("/{id}/file-url")
    public ResponseEntity<ApiResponse<Map<String, String>>> fileUrl(@PathVariable("id") UUID id) {
        String readUrl = invoiceQueryService.readUrl(id);
        Map<String, String> result = Map.of("readUrl", readUrl);
        return ResponseEntity.ok(ApiResponse.ok(result, "URL de lectura generada correctamente"));
    }
}
