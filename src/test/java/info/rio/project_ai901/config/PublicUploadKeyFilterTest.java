package info.rio.project_ai901.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PublicUploadKeyFilterTest {

    private static final String HEADER = "X-Public-Upload-Key";

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void autenticaConClaveValida() throws Exception {
        MockHttpServletRequest request = request();
        request.addHeader(HEADER, "clave-secreta");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        new PublicUploadKeyFilter("clave-secreta").doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_PUBLIC_UPLOAD");
    }

    @Test
    void rechazaClaveIncorrecta() throws Exception {
        MockHttpServletRequest request = request();
        request.addHeader(HEADER, "otra-clave");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        new PublicUploadKeyFilter("clave-secreta").doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void rechazaHeaderCuandoLaClaveConfiguradaEstaVacia() throws Exception {
        MockHttpServletRequest request = request();
        request.addHeader(HEADER, "cualquiera");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        new PublicUploadKeyFilter("").doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void dejaPasarSinHeaderParaValidarJwt() throws Exception {
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        new PublicUploadKeyFilter("clave-secreta").doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void ignoraRutasQueNoSonDelAnalizadorPublico() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/invoice-analyzer/" + UUID.randomUUID());
        request.addHeader(HEADER, "clave-secreta");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        new PublicUploadKeyFilter("otra-clave").doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }

    private MockHttpServletRequest request() {
        return new MockHttpServletRequest("POST", "/public/invoice-analyzer/" + UUID.randomUUID());
    }
}
