package info.rio.project_ai901.services.user;

import info.rio.project_ai901.api.dto.ChangePasswordRequest;
import info.rio.project_ai901.api.dto.CreateUserRequest;
import info.rio.project_ai901.api.dto.UpdateUserRequest;
import info.rio.project_ai901.api.dto.UserDetailResponse;
import info.rio.project_ai901.exceptions.DuplicateResourceException;
import info.rio.project_ai901.exceptions.InvalidRequestException;
import info.rio.project_ai901.exceptions.ResourceInUseException;
import info.rio.project_ai901.exceptions.ResourceNotFoundException;
import info.rio.project_ai901.models.Branch;
import info.rio.project_ai901.models.Role;
import info.rio.project_ai901.models.User;
import info.rio.project_ai901.models.enums.RoleName;
import info.rio.project_ai901.repositories.BranchRepository;
import info.rio.project_ai901.repositories.InvoiceRepository;
import info.rio.project_ai901.repositories.RoleRepository;
import info.rio.project_ai901.repositories.UserRepository;
import info.rio.project_ai901.repositories.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private BranchRepository branchRepository;
    private WarehouseRepository warehouseRepository;
    private InvoiceRepository invoiceRepository;
    private PasswordEncoder passwordEncoder;
    private UserService service;

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        roleRepository = Mockito.mock(RoleRepository.class);
        branchRepository = Mockito.mock(BranchRepository.class);
        warehouseRepository = Mockito.mock(WarehouseRepository.class);
        invoiceRepository = Mockito.mock(InvoiceRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        service = new UserService(userRepository, roleRepository, branchRepository, warehouseRepository,
                invoiceRepository, passwordEncoder, new UserMapper());
    }

    @Test
    void creaUsuarioConPasswordHasheado() {
        UUID roleId = UUID.randomUUID();
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role(roleId, RoleName.ADMIN)));
        when(userRepository.existsByEmailIgnoreCase("ana@ai901.local")).thenReturn(false);

        UserDetailResponse result = service.create(new CreateUserRequest("Ana", " ana@ai901.local ", "secreto123",
                roleId, null, null));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        Mockito.verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getPassword()).isNotEqualTo("secreto123");
        assertThat(passwordEncoder.matches("secreto123", saved.getPassword())).isTrue();
        assertThat(result.email()).isEqualTo("ana@ai901.local");
        assertThat(result.role()).isEqualTo(RoleName.ADMIN);
        assertThat(result.active()).isTrue();
    }

    @Test
    void rechazaEmailDuplicado() {
        UUID roleId = UUID.randomUUID();
        when(userRepository.existsByEmailIgnoreCase("ana@ai901.local")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateUserRequest("Ana", "ana@ai901.local", "secreto123",
                roleId, null, null)))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void lanzaNotFoundParaUsuarioDesconocido() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.detail(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void exigeSucursalParaBranchManager() {
        UUID roleId = UUID.randomUUID();
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role(roleId, RoleName.BRANCH_MANAGER)));
        when(userRepository.existsByEmailIgnoreCase("ana@ai901.local")).thenReturn(false);

        assertThatThrownBy(() -> service.create(new CreateUserRequest("Ana", "ana@ai901.local", "secreto123",
                roleId, null, null)))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void exigeAlmacenParaWarehouseHead() {
        UUID roleId = UUID.randomUUID();
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role(roleId, RoleName.WAREHOUSE_HEAD)));
        when(userRepository.existsByEmailIgnoreCase("ana@ai901.local")).thenReturn(false);

        assertThatThrownBy(() -> service.create(new CreateUserRequest("Ana", "ana@ai901.local", "secreto123",
                roleId, null, null)))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void rechazaSucursalInactiva() {
        UUID roleId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        Branch branch = branch(branchId, false);
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role(roleId, RoleName.ADMIN)));
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(branch));
        when(userRepository.existsByEmailIgnoreCase("ana@ai901.local")).thenReturn(false);

        assertThatThrownBy(() -> service.create(new CreateUserRequest("Ana", "ana@ai901.local", "secreto123",
                roleId, branchId, null)))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void actualizaUsuarioSinTocarElPassword() {
        UUID id = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        User user = user(id, "ana@ai901.local", role(roleId, RoleName.ADMIN), true);
        String passwordOriginal = user.getPassword();
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("ana2@ai901.local", id)).thenReturn(false);
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(user.getRole()));

        UserDetailResponse result = service.update(id, new UpdateUserRequest("Ana Dos", "ana2@ai901.local",
                roleId, null, null));

        assertThat(result.name()).isEqualTo("Ana Dos");
        assertThat(result.email()).isEqualTo("ana2@ai901.local");
        assertThat(user.getPassword()).isEqualTo(passwordOriginal);
    }

    @Test
    void cambiaPasswordValidandoLaAnterior() {
        UUID id = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        User user = user(id, "ana@ai901.local", role(roleId, RoleName.ADMIN), true);
        user.setPassword(passwordEncoder.encode("vieja123"));
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        service.changePassword(id, new ChangePasswordRequest("vieja123", "nueva123"));

        assertThat(passwordEncoder.matches("nueva123", user.getPassword())).isTrue();
    }

    @Test
    void rechazaPasswordAnteriorIncorrecta() {
        UUID id = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        User user = user(id, "ana@ai901.local", role(roleId, RoleName.ADMIN), true);
        user.setPassword(passwordEncoder.encode("vieja123"));
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.changePassword(id, new ChangePasswordRequest("incorrecta", "nueva123")))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void desactivaUsuario() {
        UUID id = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        User user = user(id, "ana@ai901.local", role(roleId, RoleName.ADMIN), true);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(invoiceRepository.countByLoadedBy_IdAndStatusIn(any(), anyList())).thenReturn(0L);
        when(invoiceRepository.countByReviewedBy_IdAndStatusIn(any(), anyList())).thenReturn(0L);

        UserDetailResponse result = service.deactivate(id);

        assertThat(result.active()).isFalse();
        assertThat(user.isActive()).isFalse();
    }

    @Test
    void noDesactivaUsuarioConFacturasPendientes() {
        UUID id = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        User user = user(id, "ana@ai901.local", role(roleId, RoleName.ADMIN), true);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(invoiceRepository.countByLoadedBy_IdAndStatusIn(any(), anyList())).thenReturn(1L);

        assertThatThrownBy(() -> service.deactivate(id)).isInstanceOf(ResourceInUseException.class);
    }

    @Test
    void activaUsuarioInactivo() {
        UUID id = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        User user = user(id, "ana@ai901.local", role(roleId, RoleName.ADMIN), false);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        UserDetailResponse result = service.activate(id);

        assertThat(result.active()).isTrue();
    }

    private Role role(UUID id, RoleName name) {
        Role role = new Role();
        role.setId(id);
        role.setName(name);
        return role;
    }

    private Branch branch(UUID id, boolean active) {
        Branch branch = new Branch();
        branch.setId(id);
        branch.setCode("SUC-01");
        branch.setName("Sucursal Centro");
        branch.setCity("CDMX");
        branch.setActive(active);
        return branch;
    }

    private User user(UUID id, String email, Role role, boolean active) {
        User user = new User();
        user.setId(id);
        user.setName("Ana");
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("vieja123"));
        user.setRole(role);
        user.setActive(active);
        return user;
    }
}
