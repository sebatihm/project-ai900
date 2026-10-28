package info.rio.project_ai901.services.report.components;

import info.rio.project_ai901.exceptions.InvalidRequestException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GeneratedReportStore {

    public static final Duration TTL = Duration.ofMinutes(15);
    private static final int MAX_ENTRIES = 50;

    public record Stored(UUID ownerId, String filename, byte[] content, Instant expiresAt) {}

    private final Map<String, Stored> items = new ConcurrentHashMap<>();

    public static String downloadPath(String token) {
        return "/report/download/" + token;
    }

    public String save(UUID ownerId, String filename, byte[] content) {
        purge();
        if (items.size() >= MAX_ENTRIES) {
            throw new InvalidRequestException("Hay demasiados informes en proceso, intenta en unos minutos.");
        }
        String token = UUID.randomUUID().toString();
        items.put(token, new Stored(ownerId, filename, content, Instant.now().plus(TTL)));
        return token;
    }

    public Optional<Stored> get(String token, UUID ownerId) {
        Stored s = items.get(token);
        if (s == null || ownerId == null || !ownerId.equals(s.ownerId())
                || s.expiresAt().isBefore(Instant.now())) {
            return Optional.empty();
        }
        return Optional.of(s);
    }

    private void purge() {
        Instant now = Instant.now();
        items.values().removeIf(s -> s.expiresAt().isBefore(now));
    }
}