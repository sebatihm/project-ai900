package info.rio.project_ai901.api;

import info.rio.project_ai901.api.dto.ApiResponse;
import info.rio.project_ai901.api.dto.InvoiceAnalysisResponse;
import info.rio.project_ai901.api.dto.InvoiceAnalyzerRequest;
import info.rio.project_ai901.api.dto.PublicInvoiceAnalysisResponse;
import info.rio.project_ai901.exceptions.InvalidRequestException;
import info.rio.project_ai901.services.invoice.InvoiceIngestionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/public/invoice-analyzer")
public class PublicInvoiceAnalyzerController {

    private final InvoiceIngestionService invoiceIngestionService;

    public PublicInvoiceAnalyzerController(InvoiceIngestionService invoiceIngestionService) {
        this.invoiceIngestionService = invoiceIngestionService;
    }

    @PostMapping("/{supplierId}")
    public ResponseEntity<ApiResponse<PublicInvoiceAnalysisResponse>> analyze(
            @PathVariable("supplierId") UUID supplierId,
            @ModelAttribute @Valid InvoiceAnalyzerRequest request) {
        MultipartFile file = request.getFile();
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException("El archivo 'file' es obligatorio y no puede estar vacio");
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new InvalidRequestException("No se pudo leer el archivo subido");
        }
        InvoiceAnalysisResponse analysis = invoiceIngestionService.analyze(file.getOriginalFilename(), content,
                supplierId);
        PublicInvoiceAnalysisResponse response = PublicInvoiceAnalysisResponse.from(analysis);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Análisis de factura realizado correctamente"));
    }
}
