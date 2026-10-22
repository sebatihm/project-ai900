package info.rio.project_ai901.services.product;

import info.rio.project_ai901.api.dto.CreateProductRequest;
import info.rio.project_ai901.api.dto.ProductDetailResponse;
import info.rio.project_ai901.api.dto.UpdateProductRequest;
import info.rio.project_ai901.exceptions.DuplicateResourceException;
import info.rio.project_ai901.exceptions.ResourceInUseException;
import info.rio.project_ai901.exceptions.ResourceNotFoundException;
import info.rio.project_ai901.models.Category;
import info.rio.project_ai901.models.Product;
import info.rio.project_ai901.repositories.CategoryRepository;
import info.rio.project_ai901.repositories.InventoryRepository;
import info.rio.project_ai901.repositories.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductServiceTest {

    private ProductRepository productRepository;
    private CategoryRepository categoryRepository;
    private InventoryRepository inventoryRepository;
    private ProductService service;

    @BeforeEach
    void setUp() {
        productRepository = Mockito.mock(ProductRepository.class);
        categoryRepository = Mockito.mock(CategoryRepository.class);
        inventoryRepository = Mockito.mock(InventoryRepository.class);
        service = new ProductService(productRepository, categoryRepository, inventoryRepository,
                new ProductMapper());
    }

    @Test
    void createsProductWithResolvedCategory() {
        UUID categoryId = UUID.randomUUID();
        when(productRepository.existsBySkuIgnoreCase("SKU-001")).thenReturn(false);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category(categoryId, "Bebidas")));

        ProductDetailResponse result = service.create(
                new CreateProductRequest(" SKU-001 ", "Coca Cola", categoryId, "ml", new BigDecimal("12.50")));

        assertThat(result.sku()).isEqualTo("SKU-001");
        assertThat(result.categoryName()).isEqualTo("Bebidas");
        assertThat(result.price()).isEqualByComparingTo("12.50");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void rejectsUnknownCategory() {
        UUID categoryId = UUID.randomUUID();
        when(productRepository.existsBySkuIgnoreCase("SKU-001")).thenReturn(false);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(
                new CreateProductRequest("SKU-001", "Coca Cola", categoryId, "ml", new BigDecimal("12.50"))))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void rejectsDuplicatedSku() {
        when(productRepository.existsBySkuIgnoreCase("SKU-001")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateProductRequest("SKU-001", "Coca Cola",
                UUID.randomUUID(), "ml", new BigDecimal("12.50"))))
                .isInstanceOf(DuplicateResourceException.class);
        verify(categoryRepository, never()).findById(any(UUID.class));
    }

    @Test
    void throwsNotFoundForUnknownProduct() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.detail(id)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void refusesToDeleteProductWithInventory() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.of(product(id, "SKU-001")));
        when(inventoryRepository.countByProduct_Id(id)).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(ResourceInUseException.class);
        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    void deletesProductWithoutInventory() {
        UUID id = UUID.randomUUID();
        Product product = product(id, "SKU-001");
        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(inventoryRepository.countByProduct_Id(id)).thenReturn(0L);

        service.delete(id);

        verify(productRepository).delete(product);
    }

    @Test
    void updatesProductPriceAndCategory() {
        UUID id = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.of(product(id, "SKU-001")));
        when(productRepository.existsBySkuIgnoreCaseAndIdNot("SKU-002", id)).thenReturn(false);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category(categoryId, "Bebidas")));

        ProductDetailResponse result = service.update(id, new UpdateProductRequest("SKU-002", "Refresco",
                categoryId, "lt", new BigDecimal("9.99")));

        assertThat(result.sku()).isEqualTo("SKU-002");
        assertThat(result.price()).isEqualByComparingTo("9.99");
        verify(productRepository).save(any(Product.class));
    }

    private Category category(UUID id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        return category;
    }

    private Product product(UUID id, String sku) {
        Product product = new Product();
        product.setId(id);
        product.setSku(sku);
        product.setName("Producto " + sku);
        product.setUnit("ml");
        product.setPrice(new BigDecimal("12.50"));
        product.setCategory(category(UUID.randomUUID(), "Bebidas"));
        return product;
    }
}
