package info.rio.project_ai901.services.invoice;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class InvoiceHashing {

    private InvoiceHashing() {
    }

    public static String sha256Hex(byte[] content) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(content);
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(Character.forDigit((value >> 4) & 0xF, 16));
                builder.append(Character.forDigit(value & 0xF, 16));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
