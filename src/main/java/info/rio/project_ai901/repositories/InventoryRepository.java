package info.rio.project_ai901.repositories;

import info.rio.project_ai901.models.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    Optional<Inventory> findByWarehouse_IdAndProduct_Id(UUID warehouseId, UUID productId);

    boolean existsByWarehouse_IdAndProduct_Id(UUID warehouseId, UUID productId);

    Page<Inventory> findByWarehouse_Id(UUID warehouseId, Pageable pageable);

    Page<Inventory> findByProduct_Id(UUID productId, Pageable pageable);

    Page<Inventory> findByWarehouse_IdAndProduct_Id(UUID warehouseId, UUID productId, Pageable pageable);

    long countByProduct_Id(UUID productId);

    long countByWarehouse_Id(UUID warehouseId);
}
