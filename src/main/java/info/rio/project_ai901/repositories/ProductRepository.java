package info.rio.project_ai901.repositories;

import info.rio.project_ai901.models.Product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, UUID id);

    Page<Product> findByNameContainingIgnoreCaseOrSkuContainingIgnoreCase(
        String name,
        String sku,
        Pageable pageable);

    long countByCategory_Id(UUID categoryId);
}
