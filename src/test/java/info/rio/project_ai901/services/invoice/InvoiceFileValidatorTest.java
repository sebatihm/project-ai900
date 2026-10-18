package info.rio.project_ai901.services.invoice;

import info.rio.project_ai901.exceptions.InvoiceValidationException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvoiceFileValidatorTest {

    private static final int MAX_SIZE = 10485760;

    private final InvoiceFileValidator validator = new InvoiceFileValidator(new FileSignatureValidator(), MAX_SIZE,
            "pdf,jpg,jpeg,png", 5, 800, 600);

    @Test
    void rejectsEmptyContent() {
        assertThatThrownBy(() -> validator.validate("factura.pdf", new byte[0]))
                .isInstanceOf(InvoiceValidationException.class)
                .hasMessageContaining("vacio");
    }

    @Test
    void rejectsDisallowedExtension() {
        assertThatThrownBy(() -> validator.validate("factura.txt", pdf()))
                .isInstanceOf(InvoiceValidationException.class)
                .hasMessageContaining("Extension no permitida");
    }

    @Test
    void rejectsExtensionThatDoesNotMatchContent() {
        assertThatThrownBy(() -> validator.validate("factura.pdf", image("png", 1000, 800)))
                .isInstanceOf(InvoiceValidationException.class)
                .hasMessageContaining("no coincide con el contenido real");
    }

    @Test
    void rejectsUnknownSignature() {
        byte[] garbage = "esto no es un archivo valido".getBytes();
        assertThatThrownBy(() -> validator.validate("factura.pdf", garbage))
                .isInstanceOf(InvoiceValidationException.class);
    }

    @Test
    void rejectsFileOverMaxSize() {
        InvoiceFileValidator smallValidator = new InvoiceFileValidator(new FileSignatureValidator(), 10,
                "pdf,jpg,jpeg,png", 5, 800, 600);
        assertThatThrownBy(() -> smallValidator.validate("factura.pdf", new byte[11]))
                .isInstanceOf(InvoiceValidationException.class)
                .hasMessageContaining("maximo permitido");
    }

    @Test
    void acceptsValidPdf() {
        ValidatedFile file = validator.validate("factura.pdf", pdf());

        assertThat(file.signature()).isEqualTo(FileSignature.PDF);
        assertThat(file.pageCount()).isEqualTo(1);
        assertThat(file.contentType()).isEqualTo("application/pdf");
        assertThat(file.originalFileName()).isEqualTo("factura.pdf");
    }

    @Test
    void rejectsImageTooSmall() {
        assertThatThrownBy(() -> validator.validate("factura.png", image("png", 100, 50)))
                .isInstanceOf(InvoiceValidationException.class)
                .hasMessageContaining("muy pequena");
    }

    @Test
    void acceptsValidImage() {
        ValidatedFile file = validator.validate("factura.png", image("png", 1200, 900));

        assertThat(file.signature()).isEqualTo(FileSignature.PNG);
        assertThat(file.pageCount()).isEqualTo(1);
        assertThat(file.width()).isEqualTo(1200);
        assertThat(file.height()).isEqualTo(900);
        assertThat(file.contentType()).isEqualTo("image/png");
    }

    private byte[] pdf() {
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private byte[] image(String format, int width, int height) {
        try {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, format, output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
