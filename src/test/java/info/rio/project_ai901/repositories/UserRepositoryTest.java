package info.rio.project_ai901.repositories;

import info.rio.project_ai901.models.Branch;
import info.rio.project_ai901.models.Role;
import info.rio.project_ai901.models.User;
import info.rio.project_ai901.models.enums.RoleName;
import info.rio.project_ai901.support.AbstractRepositoryTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Test
    void buscaUsuarioPorEmailIgnorandoMayusculas() {
        Role role = role(RoleName.ADMIN);
        userRepository.saveAndFlush(user("Ana", "ana@ai901.local", role, true));

        Optional<User> result = userRepository.findByEmailIgnoreCase("ANA@AI901.LOCAL");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Ana");
    }

    @Test
    void verificaExistenciaPorEmail() {
        userRepository.saveAndFlush(user("Ana", "ana@ai901.local", role(RoleName.ADMIN), true));

        assertThat(userRepository.existsByEmailIgnoreCase("ana@ai901.local")).isTrue();
        assertThat(userRepository.existsByEmailIgnoreCase("otro@ai901.local")).isFalse();
    }

    @Test
    void verificaExistenciaPorEmailExcluyendoElPropioId() {
        User guardada = userRepository.saveAndFlush(user("Ana", "ana@ai901.local", role(RoleName.ADMIN), true));
        userRepository.saveAndFlush(user("Luis", "luis@ai901.local", role(RoleName.ADMIN), true));

        assertThat(userRepository.existsByEmailIgnoreCaseAndIdNot("ana@ai901.local", guardada.getId())).isFalse();
        assertThat(userRepository.existsByEmailIgnoreCaseAndIdNot("luis@ai901.local", guardada.getId())).isTrue();
    }

    @Test
    void filtraPorRolYSucursal() {
        Role admin = role(RoleName.ADMIN);
        Role soporte = role(RoleName.ADMIN_SUPPORT);
        Branch sucursal = branch("SUC-01", true);
        userRepository.saveAndFlush(user("Ana", "ana@ai901.local", admin, true, sucursal));
        userRepository.saveAndFlush(user("Luis", "luis@ai901.local", soporte, true, null));

        assertThat(userRepository.search(soporte.getId(), null, null, true, PageRequest.of(0, 10)))
                .hasSize(1)
                .allSatisfy(user -> assertThat(user.getName()).isEqualTo("Luis"));
        assertThat(userRepository.search(admin.getId(), sucursal.getId(), null, true, PageRequest.of(0, 10)))
                .hasSize(1)
                .allSatisfy(user -> assertThat(user.getName()).isEqualTo("Ana"));
    }

    @Test
    void excluyeInactivosPorDefecto() {
        Role role = role(RoleName.ADMIN);
        userRepository.saveAndFlush(user("Ana", "ana@ai901.local", role, true));
        userRepository.saveAndFlush(user("Luis", "luis@ai901.local", role, false));

        assertThat(userRepository.search(null, null, null, false, PageRequest.of(0, 10))).hasSize(1);
    }

    @Test
    void incluyeInactivosCuandoSeSolicita() {
        Role role = role(RoleName.ADMIN);
        userRepository.saveAndFlush(user("Ana", "ana@ai901.local", role, true));
        userRepository.saveAndFlush(user("Luis", "luis@ai901.local", role, false));

        assertThat(userRepository.search(null, null, null, true, PageRequest.of(0, 10))).hasSize(2);
    }

    @Test
    void cuentaUsuariosPorRol() {
        Role admin = role(RoleName.ADMIN);
        Role soporte = role(RoleName.ADMIN_SUPPORT);
        userRepository.saveAndFlush(user("Ana", "ana@ai901.local", admin, true));
        userRepository.saveAndFlush(user("Luis", "luis@ai901.local", soporte, true));

        assertThat(userRepository.countByRole_Id(admin.getId())).isEqualTo(1);
        assertThat(userRepository.countByRole_Id(soporte.getId())).isEqualTo(1);
    }

    @Test
    void rechazaEmailDuplicado() {
        Role role = role(RoleName.ADMIN);
        userRepository.saveAndFlush(user("Ana", "ana@ai901.local", role, true));

        assertThatThrownBy(() -> userRepository.saveAndFlush(user("Otra", "ana@ai901.local", role, true)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Role role(RoleName name) {
        return roleRepository.findByName(name).orElseGet(() -> {
            Role role = new Role();
            role.setName(name);
            return roleRepository.saveAndFlush(role);
        });
    }

    private Branch branch(String code, boolean active) {
        Branch branch = new Branch();
        branch.setCode(code);
        branch.setName("Sucursal " + code);
        branch.setCity("CDMX");
        branch.setActive(active);
        return branchRepository.saveAndFlush(branch);
    }

    private User user(String name, String email, Role role, boolean active) {
        return user(name, email, role, active, null);
    }

    private User user(String name, String email, Role role, boolean active, Branch branch) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword("$2a$10$hash");
        user.setRole(role);
        user.setBranch(branch);
        user.setActive(active);
        return user;
    }
}
