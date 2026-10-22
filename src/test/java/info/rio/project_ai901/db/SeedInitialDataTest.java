package info.rio.project_ai901.db;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica el script de seed manual: que siembre los roles esperados y que el
 * hash BCrypt del administrador corresponda a la contrasena documentada.
 */
class SeedInitialDataTest {

    private static final String ADMIN_HASH =
            "$2a$10$N0W5aSWtGlanaiDq53Q5y.4Gx0QB0akm2TS7EezwfkOPxZetBITHO";

    @Test
    void siembraLosCincoRolesDelCatalogo() throws IOException {
        String script = seedScript();

        assertThat(script).contains("'BOSS'");
        assertThat(script).contains("'ADMIN'");
        assertThat(script).contains("'BRANCH_MANAGER'");
        assertThat(script).contains("'WAREHOUSE_HEAD'");
        assertThat(script).contains("'ADMIN_SUPPORT'");
    }

    @Test
    void siembraElAdministradorConSuHashDeContrasena() throws IOException {
        String script = seedScript();

        assertThat(script).contains("admin@ai901.local");
        assertThat(script).contains(ADMIN_HASH);
    }

    @Test
    void elHashDelAdministradorCorrespondeALaContrasenaDocumentada() {
        PasswordEncoder encoder = new BCryptPasswordEncoder();

        assertThat(encoder.matches("Admin@123", ADMIN_HASH)).isTrue();
        assertThat(encoder.matches("otra-clave", ADMIN_HASH)).isFalse();
    }

    @Test
    void elHashDelScriptEsElMismoQueElVerificado() throws IOException {
        Matcher matcher = Pattern.compile("\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}").matcher(seedScript());

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group()).isEqualTo(ADMIN_HASH);
    }

    private String seedScript() throws IOException {
        try (InputStream stream = getClass().getClassLoader()
                .getResourceAsStream("db/seed/seed-initial-data.sql")) {
            assertThat(stream).as("seed-initial-data.sql debe estar en los recursos").isNotNull();
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
