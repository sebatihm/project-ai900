package info.rio.project_ai901.services.auth;

import info.rio.project_ai901.api.dto.LoginRequest;
import info.rio.project_ai901.api.dto.LoginResponse;
import info.rio.project_ai901.api.dto.UserSummaryResponse;
import info.rio.project_ai901.exceptions.InvalidCredentialsException;
import info.rio.project_ai901.exceptions.ResourceNotFoundException;
import info.rio.project_ai901.models.User;
import info.rio.project_ai901.repositories.UserRepository;
import info.rio.project_ai901.services.user.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Credenciales invalidas";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper mapper;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
            UserMapper mapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String email = request.email().trim();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new InvalidCredentialsException(INVALID_CREDENTIALS));
        if (!user.isActive() || user.getPassword() == null
                || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException(INVALID_CREDENTIALS);
        }
        String token = jwtService.generateToken(user);
        return new LoginResponse(token, "Bearer", jwtService.expirationInstant(), mapper.toSummary(user));
    }

    @Transactional(readOnly = true)
    public UserSummaryResponse currentUser(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + email));
        return mapper.toSummary(user);
    }
}
