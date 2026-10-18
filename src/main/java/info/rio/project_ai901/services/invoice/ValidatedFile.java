package info.rio.project_ai901.services.invoice;

public record ValidatedFile(byte[] content, FileSignature signature, String originalFileName, String contentType,
        int pageCount, Integer width, Integer height) {
}
