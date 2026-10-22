package info.rio.project_ai901.api.dto;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public class InvoiceAnalyzerRequest {

    @NotNull(message = "El archivo 'file' es obligatorio")
    private MultipartFile file;

    private UUID supplierId;

    public InvoiceAnalyzerRequest() {
    }

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }

    public UUID getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(UUID supplierId) {
        this.supplierId = supplierId;
    }
}