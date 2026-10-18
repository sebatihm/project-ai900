package info.rio.project_ai901.repositories;

import info.rio.project_ai901.models.Warehouse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WarehouseRepository extends JpaRepository<Warehouse, UUID> {

    Optional<Warehouse> findFirstByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);

    boolean existsByActiveTrueAndBranch_Id(UUID branchId);

    long countByActiveTrueAndBranch_Id(UUID branchId);

    Page<Warehouse> findByActive(boolean active, Pageable pageable);

    Page<Warehouse> findByActiveAndBranch_Id(boolean active, UUID branchId, Pageable pageable);

    Page<Warehouse> findByBranch_Id(UUID branchId, Pageable pageable);
}
