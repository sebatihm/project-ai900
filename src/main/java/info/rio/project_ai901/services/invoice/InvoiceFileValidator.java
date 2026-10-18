package info.rio.project_ai901.services.invoice;

import info.rio.project_ai901.exceptions.InvoiceValidationException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

@Component
public class InvoiceFileValidator {

    private final FileSignatureValidator signatureValidator;
    private final long maxSizeBytes;
    private final Set<String> allowedExtensions;
    private final int maxPages;
    private final int minImageWidth;
    private final int minImageHeight;

    public InvoiceFileValidator(FileSignatureValidator signatureValidator,
            @Value("${app.invoice.max-size-bytes:10485760}") long maxSizeBytes,
            @Value("${app.invoice.allowed-extensions:pdf,jpg,jpeg,png}") String allowedExtensions,
            @Value("${app.invoice.max-pages:5}") int maxPages,
            @Value("${app.invoice.min-image-width:800}") int minImageWidth,
            @Value("${app.invoice.min-image-height:600}") int minImageHeight) {
        this.signatureValidator = signatureValidator;
        this.maxSizeBytes = maxSizeBytes;
        this.allowedExtensions = new LinkedHashSet<>(
                Arrays.stream(allowedExtensions.split(",")).map(String::trim).map(String::toLowerCase).toList());
        this.maxPages = maxPages;
        this.minImageWidth = minImageWidth;
        this.minImageHeight = minImageHeight;
    }

    public ValidatedFile validate(String originalFilename, byte[] content) {
        if (content == null || content.length == 0) {
            throw new InvoiceValidationException("El archivo esta vacio");
        }
        if (content.length > maxSizeBytes) {
            throw new InvoiceValidationException(
                    "El archivo supera el tamano maximo permitido de " + (maxSizeBytes / (1024 * 1024)) + " MB");
        }

        String extension = extension(originalFilename);
        if (!allowedExtensions.contains(extension)) {
            throw new InvoiceValidationException(
                    "Extension no permitida: " + extension + ". Permitidas: " + allowedExtensions);
        }

        FileSignature signature = signatureValidator.detect(content)
                .orElseThrow(() -> new InvoiceValidationException(
                        "El contenido del archivo no corresponde a un PDF o imagen valida (firma desconocida)"));

        if (!matches(extension, signature)) {
            throw new InvoiceValidationException(
                    "La extension '" + extension + "' no coincide con el contenido real (" + signature + ")");
        }

        if (signature == FileSignature.PDF) {
            return validatePdf(content, originalFilename);
        }
        return validateImage(content, signature, originalFilename);
    }

    private ValidatedFile validatePdf(byte[] content, String originalFilename) {
        try (PDDocument document = Loader.loadPDF(content)) {
            int pages = document.getNumberOfPages();
            if (pages > maxPages) {
                throw new InvoiceValidationException("El PDF tiene " + pages + " paginas; el maximo permitido es " + maxPages);
            }
            if (pages == 0) {
                throw new InvoiceValidationException("El PDF no tiene paginas");
            }
            return new ValidatedFile(content, FileSignature.PDF, originalFilename,
                    FileSignature.PDF.contentType(), pages, null, null);
        } catch (InvalidPasswordException e) {
            throw new InvoiceValidationException("El PDF esta protegido con contrasena");
        } catch (InvoiceValidationException e) {
            throw e;
        } catch (IOException e) {
            throw new InvoiceValidationException("El PDF esta danado o no se puede abrir");
        }
    }

    private ValidatedFile validateImage(byte[] content, FileSignature signature, String originalFilename) {
        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(content));
        } catch (IOException e) {
            throw new InvoiceValidationException("La imagen esta danada o no se puede leer");
        }
        if (image == null) {
            throw new InvoiceValidationException("La imagen esta danada o no se puede leer");
        }
        if (image.getWidth() < minImageWidth || image.getHeight() < minImageHeight) {
            throw new InvoiceValidationException("La imagen es muy pequena: " + image.getWidth() + "x" + image.getHeight()
                    + ", minimo " + minImageWidth + "x" + minImageHeight);
        }
        return new ValidatedFile(content, signature, originalFilename, signature.contentType(), 1, image.getWidth(),
                image.getHeight());
    }

    private String extension(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new InvoiceValidationException("El archivo no tiene nombre");
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            throw new InvoiceValidationException("El archivo no tiene extension");
        }
        return filename.substring(dot + 1).toLowerCase();
    }

    private boolean matches(String extension, FileSignature signature) {
        return switch (signature) {
            case PDF -> "pdf".equals(extension);
            case JPEG -> "jpg".equals(extension) || "jpeg".equals(extension);
            case PNG -> "png".equals(extension);
        };
    }
}
