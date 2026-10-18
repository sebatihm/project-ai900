package info.rio.project_ai901.services.invoice;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class InvoiceNormalizerTest {

    private final InvoiceNormalizer normalizer = new InvoiceNormalizer();

    @Test
    void keepsPdfContentUnchanged() {
        byte[] pdf = pdf();

        assertThat(normalizer.toPdf(pdf, FileSignature.PDF)).isSameAs(pdf);
    }

    @Test
    void convertsImageToSinglePagePdf() throws IOException {
        byte[] result = normalizer.toPdf(image(900, 700), FileSignature.PNG);

        assertThat(new String(java.util.Arrays.copyOf(result, 4))).isEqualTo("%PDF");
        try (PDDocument document = Loader.loadPDF(result)) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);
        }
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

    private byte[] image(int width, int height) {
        try {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
