package info.rio.project_ai901.repositories;

import info.rio.project_ai901.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);

    long countByRole_Id(UUID roleId);

    @Query("select u from User u where (:roleId is null or u.role.id = :roleId) "
            + "and (:branchId is null or u.branch.id = :branchId) "
            + "and (:warehouseId is null or u.warehouse.id = :warehouseId) "
            + "and (:includeInactive = true or u.active = true)")
    Page<User> search(@Param("roleId") UUID roleId, @Param("branchId") UUID branchId,
            @Param("warehouseId") UUID warehouseId, @Param("includeInactive") boolean includeInactive,
            Pageable pageable);
}
