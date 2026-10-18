package info.rio.project_ai901.exceptions;

import lombok.Getter;

import java.util.UUID;

@Getter
public class DuplicateInvoiceException extends RuntimeException {

    private final UUID originalInvoiceId;
    private final String originalContainer;
    private final String originalBlobPath;

    public DuplicateInvoiceException(String message, UUID originalInvoiceId, String originalContainer,
            String originalBlobPath) {
        super(message);
        this.originalInvoiceId = originalInvoiceId;
        this.originalContainer = originalContainer;
        this.originalBlobPath = originalBlobPath;
    }
}
