package info.rio.project_ai901.api;

import info.rio.project_ai901.api.dto.InvoiceAnalysisResponse;
import info.rio.project_ai901.exceptions.InvalidRequestException;
import info.rio.project_ai901.services.invoice.InvoiceIngestionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/invoice-analyzer")
public class InvoiceAnalyzerController {

    private final InvoiceIngestionService invoiceIngestionService;

    public InvoiceAnalyzerController(InvoiceIngestionService invoiceIngestionService) {
        this.invoiceIngestionService = invoiceIngestionService;
    }

    @PostMapping
    public ResponseEntity<InvoiceAnalysisResponse> analyze(@RequestParam("file") MultipartFile file,
            @RequestParam("uploadedBy") String uploadedBy,
            @RequestParam(value = "supplierId", required = false) UUID supplierId) {
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException("El archivo 'file' es obligatorio y no puede estar vacio");
        }
        if (uploadedBy == null || uploadedBy.isBlank()) {
            throw new InvalidRequestException("El parametro 'uploadedBy' es obligatorio");
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new InvalidRequestException("No se pudo leer el archivo subido");
        }
        InvoiceAnalysisResponse response = invoiceIngestionService.analyze(file.getOriginalFilename(), content,
                uploadedBy.trim(), supplierId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
