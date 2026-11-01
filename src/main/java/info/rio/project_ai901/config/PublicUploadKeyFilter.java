package info.rio.project_ai901.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

public class PublicUploadKeyFilter extends OncePerRequestFilter {

    private static final String PATH_PREFIX = "/public/invoice-analyzer/";
    private static final String HEADER = "X-Public-Upload-Key";

    private final byte[] configuredKey;

    public PublicUploadKeyFilter(String configuredKey) {
        this.configuredKey = configuredKey == null || configuredKey.isBlank()
                ? new byte[0]
                : configuredKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!isPublicUploadRequest(request)) {
            chain.doFilter(request, response);
            return;
        }

        String providedKey = request.getHeader(HEADER);
        if (providedKey == null) {
            chain.doFilter(request, response);
            return;
        }

        if (configuredKey.length == 0
                || !MessageDigest.isEqual(configuredKey, providedKey.getBytes(StandardCharsets.UTF_8))) {
            SecurityContextHolder.clearContext();
            unauthorized(response);
            return;
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "public-upload", null, List.of(new SimpleGrantedAuthority("ROLE_PUBLIC_UPLOAD")));
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        chain.doFilter(request, response);
    }

    private boolean isPublicUploadRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        return "POST".equalsIgnoreCase(request.getMethod()) && path.startsWith(PATH_PREFIX);
    }

    private void unauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/problem+json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
                "{\"title\":\"No autorizado\",\"status\":401,\"detail\":\"Clave publica invalida o no configurada\"}");
    }
}
