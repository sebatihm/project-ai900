package info.rio.project_ai901.api;

import info.rio.project_ai901.aiTools.UserContext;
import info.rio.project_ai901.services.report.SalesReportService;
import info.rio.project_ai901.services.report.components.GeneratedReportStore;
import info.rio.project_ai901.services.report.dto.GeneratedReport;
import info.rio.project_ai901.services.report.dto.ReportRequester;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/report")
public class ReportController {

    private static final MediaType DOCX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    public record SalesReportRequest(UUID branchId, Integer month, Integer year, LocalDate from, LocalDate to) {}

    public record SalesReportResponse(String downloadPath, String filename, String period, String scope,
                                      long orders, BigDecimal total, long expiresInMinutes) {}

    private final SalesReportService reportService;
    private final GeneratedReportStore store;

    public ReportController(SalesReportService reportService, GeneratedReportStore store) {
        this.reportService = reportService;
        this.store = store;
    }

    @PostMapping("/sales")
    public SalesReportResponse sales(@RequestBody(required = false) SalesReportRequest req,
                                     @RequestAttribute("userContext") UserContext ctx) {
        SalesReportRequest r = (req != null) ? req : new SalesReportRequest(null, null, null, null, null);
        ReportRequester who = new ReportRequester(ctx.userId(), ctx.name(), ctx.role(), ctx.warehouseId());

        GeneratedReport g = reportService.generate(who, r.branchId(), r.month(), r.year(), r.from(), r.to());

        return new SalesReportResponse(GeneratedReportStore.downloadPath(g.token()), g.filename(),
                g.periodLabel(), g.scope(), g.orders(), g.total(), GeneratedReportStore.TTL.toMinutes());
    }

    @GetMapping("/download/{token}")
    public ResponseEntity<byte[]> download(@PathVariable String token,
                                           @RequestAttribute("userContext") UserContext ctx) {
        GeneratedReportStore.Stored file = store.get(token, ctx.userId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "El informe no existe o ya caduco"));

        return ResponseEntity.ok()
                .contentType(DOCX)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(file.filename(), StandardCharsets.UTF_8).build().toString())
                .body(file.content());
    }
}