package info.rio.project_ai901.repositories;

import info.rio.project_ai901.models.Role;
import info.rio.project_ai901.models.enums.RoleName;
import info.rio.project_ai901.support.AbstractRepositoryTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RoleRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void guardaYBuscaRolPorNombre() {
        roleRepository.saveAndFlush(role(RoleName.ADMIN));

        assertThat(roleRepository.findByName(RoleName.ADMIN)).isPresent()
                .get()
                .extracting(Role::getName)
                .isEqualTo(RoleName.ADMIN);
    }

    @Test
    void verificaExistenciaPorNombre() {
        roleRepository.saveAndFlush(role(RoleName.ADMIN));

        assertThat(roleRepository.existsByName(RoleName.ADMIN)).isTrue();
        assertThat(roleRepository.existsByName(RoleName.BOSS)).isFalse();
    }

    @Test
    void listaRolesOrdenadosPorNombre() {
        roleRepository.saveAndFlush(role(RoleName.WAREHOUSE_HEAD));
        roleRepository.saveAndFlush(role(RoleName.ADMIN));
        roleRepository.saveAndFlush(role(RoleName.BOSS));

        List<Role> roles = roleRepository.findAllByOrderByNameAsc();

        assertThat(roles).extracting(Role::getName)
                .containsExactly(RoleName.ADMIN, RoleName.BOSS, RoleName.WAREHOUSE_HEAD);
    }

    private Role role(RoleName name) {
        Role role = new Role();
        role.setName(name);
        return role;
    }
}
