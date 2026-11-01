package info.rio.project_ai901.api.dto;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public class InvoiceAnalyzerRequest {

    @NotNull(message = "El archivo 'file' es obligatorio")
    private MultipartFile file;

    public InvoiceAnalyzerRequest() {
    }

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }
}