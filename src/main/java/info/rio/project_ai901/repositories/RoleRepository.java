package info.rio.project_ai901.repositories;

import info.rio.project_ai901.models.Role;
import info.rio.project_ai901.models.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByName(RoleName name);

    boolean existsByName(RoleName name);

    List<Role> findAllByOrderByNameAsc();
}
