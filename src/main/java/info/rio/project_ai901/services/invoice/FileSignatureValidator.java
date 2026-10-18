package info.rio.project_ai901.services.invoice;

import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class FileSignatureValidator {

    public Optional<FileSignature> detect(byte[] content) {
        if (content == null || content.length < 8) {
            return Optional.empty();
        }
        if (startsWith(content, 0x25, 0x50, 0x44, 0x46, 0x2D)) {
            return Optional.of(FileSignature.PDF);
        }
        if (startsWith(content, 0xFF, 0xD8, 0xFF)) {
            return Optional.of(FileSignature.JPEG);
        }
        if (startsWith(content, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
            return Optional.of(FileSignature.PNG);
        }
        return Optional.empty();
    }

    private boolean startsWith(byte[] content, int... signature) {
        if (content.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((content[i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        return true;
    }
}
