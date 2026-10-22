package info.rio.project_ai901.services.auth;

import info.rio.project_ai901.api.dto.LoginRequest;
import info.rio.project_ai901.api.dto.LoginResponse;
import info.rio.project_ai901.api.dto.UserSummaryResponse;
import info.rio.project_ai901.exceptions.InvalidCredentialsException;
import info.rio.project_ai901.exceptions.ResourceNotFoundException;
import info.rio.project_ai901.models.Role;
import info.rio.project_ai901.models.User;
import info.rio.project_ai901.models.enums.RoleName;
import info.rio.project_ai901.repositories.UserRepository;
import info.rio.project_ai901.services.user.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuthService service;

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        jwtService = Mockito.mock(JwtService.class);
        service = new AuthService(userRepository, passwordEncoder, jwtService, new UserMapper());
    }

    @Test
    void loginExitosoDevuelveToken() {
        User user = user("ana@ai901.local", "secreto123", true);
        when(userRepository.findByEmailIgnoreCase("ana@ai901.local")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("token-jwt");
        when(jwtService.expirationInstant()).thenReturn(Instant.parse("2030-01-01T00:00:00Z"));

        LoginResponse result = service.login(new LoginRequest("ana@ai901.local", "secreto123"));

        assertThat(result.token()).isEqualTo("token-jwt");
        assertThat(result.tokenType()).isEqualTo("Bearer");
        assertThat(result.user().email()).isEqualTo("ana@ai901.local");
        assertThat(result.user().role()).isEqualTo(RoleName.ADMIN);
    }

    @Test
    void loginConEmailDesconocidoLanzaExcepcion() {
        when(userRepository.findByEmailIgnoreCase("ana@ai901.local")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest("ana@ai901.local", "secreto123")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginConPasswordIncorrectoLanzaExcepcion() {
        User user = user("ana@ai901.local", "secreto123", true);
        when(userRepository.findByEmailIgnoreCase("ana@ai901.local")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.login(new LoginRequest("ana@ai901.local", "otra-clave")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginConUsuarioInactivoLanzaExcepcion() {
        User user = user("ana@ai901.local", "secreto123", false);
        when(userRepository.findByEmailIgnoreCase("ana@ai901.local")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.login(new LoginRequest("ana@ai901.local", "secreto123")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void currentUserDevuelveElResumen() {
        User user = user("ana@ai901.local", "secreto123", true);
        when(userRepository.findByEmailIgnoreCase("ana@ai901.local")).thenReturn(Optional.of(user));

        UserSummaryResponse result = service.currentUser("ana@ai901.local");

        assertThat(result.id()).isEqualTo(user.getId());
        assertThat(result.role()).isEqualTo(RoleName.ADMIN);
    }

    @Test
    void currentUserLanzaNotFoundSiNoExiste() {
        when(userRepository.findByEmailIgnoreCase("ana@ai901.local")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.currentUser("ana@ai901.local"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private User user(String email, String rawPassword, boolean active) {
        Role role = new Role();
        role.setId(UUID.randomUUID());
        role.setName(RoleName.ADMIN);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setName("Ana");
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setActive(active);
        return user;
    }
}
