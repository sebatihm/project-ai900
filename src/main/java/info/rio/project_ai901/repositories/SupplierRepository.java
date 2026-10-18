package info.rio.project_ai901.repositories;

import info.rio.project_ai901.models.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SupplierRepository extends JpaRepository<Supplier, UUID> {

    Optional<Supplier> findFirstByNameIgnoreCase(String name);
}
