package info.rio.project_ai901.api;

import info.rio.project_ai901.api.dto.ApiResponse;
import info.rio.project_ai901.api.dto.InvoiceAnalysisResponse;
import info.rio.project_ai901.api.dto.InvoiceAnalyzerRequest;
import info.rio.project_ai901.exceptions.InvalidRequestException;
import info.rio.project_ai901.services.invoice.InvoiceIngestionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import org.springframework.validation.annotation.Validated;

@Validated
@RestController
@RequestMapping("/invoice-analyzer")
public class InvoiceAnalyzerController {

    private final InvoiceIngestionService invoiceIngestionService;

    public InvoiceAnalyzerController(InvoiceIngestionService invoiceIngestionService) {
        this.invoiceIngestionService = invoiceIngestionService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<InvoiceAnalysisResponse>> analyze(
            @ModelAttribute @Valid InvoiceAnalyzerRequest request, Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new info.rio.project_ai901.exceptions.InvalidCredentialsException("Autenticacion requerida");
        }
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
        InvoiceAnalysisResponse response = invoiceIngestionService.analyze(file.getOriginalFilename(), content,
                authentication.getName(), request.getSupplierId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(info.rio.project_ai901.api.dto.ApiResponse.created(response,
                        "Análisis de factura realizado correctamente"));
    }
}
