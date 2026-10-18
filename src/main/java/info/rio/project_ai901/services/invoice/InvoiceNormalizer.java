package info.rio.project_ai901.services.invoice;

import info.rio.project_ai901.exceptions.InvoiceValidationException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Component
public class InvoiceNormalizer {

    public byte[] toPdf(byte[] content, FileSignature signature) {
        if (signature == FileSignature.PDF) {
            return content;
        }
        try (PDDocument document = new PDDocument()) {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(content));
            if (image == null) {
                throw new InvoiceValidationException("La imagen esta danada o no se puede leer");
            }
            PDImageXObject pdImage = LosslessFactory.createFromImage(document, image);
            PDPage page = new PDPage(new PDRectangle(pdImage.getWidth(), pdImage.getHeight()));
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.drawImage(pdImage, 0, 0, pdImage.getWidth(), pdImage.getHeight());
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new InvoiceValidationException("No se pudo convertir la imagen a PDF");
        }
    }
}
