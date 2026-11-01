package info.rio.project_ai901.services.auth;

import info.rio.project_ai901.models.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Firma y verifica tokens JWT (HS256). La instancia se crea desde
 * {@code SecurityConfig} para que el bean exista tambien en los slices
 * {@code @WebMvcTest}.
 */
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey key;
    private final Duration expiration;

    public JwtService(String secret, Duration expiration) {
        this.expiration = expiration == null || expiration.isZero() || expiration.isNegative()
                ? Duration.ofHours(8)
                : expiration;
        this.key = buildKey(secret);
    }

    public String generateToken(User user) {
        Instant issuedAt = Instant.now();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId() == null ? null : user.getId().toString())
                .claim("name", user.getName())
                .claim("role", user.getRole().getName().name())
                .claim("warehouseId", user.getWarehouse().getId())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plus(this.expiration)))
                .signWith(this.key, Jwts.SIG.HS256)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(this.key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Instant expirationInstant() {
        return Instant.now().plus(this.expiration);
    }

    public Duration expiration() {
        return this.expiration;
    }

    private SecretKey buildKey(String secret) {
        if (secret == null || secret.isBlank()) {
            byte[] random = new byte[MIN_SECRET_BYTES];
            new SecureRandom().nextBytes(random);
            log.warn("No se configuro app.security.jwt.secret: se genera un secreto efimero. "
                    + "Los tokens invalidados al reiniciar la aplicacion. Defina JWT_SECRET en produccion.");
            return Keys.hmacShaKeyFor(random);
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "app.security.jwt.secret debe tener al menos " + MIN_SECRET_BYTES + " caracteres");
        }
        return Keys.hmacShaKeyFor(bytes);
    }
}
