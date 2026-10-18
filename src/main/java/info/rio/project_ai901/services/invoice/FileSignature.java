package info.rio.project_ai901.services.invoice;

public enum FileSignature {
    PDF("application/pdf"),
    JPEG("image/jpeg"),
    PNG("image/png");

    private final String contentType;

    FileSignature(String contentType) {
        this.contentType = contentType;
    }

    public String contentType() {
        return contentType;
    }
}
