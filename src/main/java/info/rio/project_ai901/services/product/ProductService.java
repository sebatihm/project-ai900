package info.rio.project_ai901.services.product;

import info.rio.project_ai901.api.dto.CreateProductRequest;
import info.rio.project_ai901.api.dto.PaginatedResponse;
import info.rio.project_ai901.api.dto.ProductDetailResponse;
import info.rio.project_ai901.api.dto.ProductSummaryResponse;
import info.rio.project_ai901.api.dto.UpdateProductRequest;
import info.rio.project_ai901.exceptions.DuplicateResourceException;
import info.rio.project_ai901.exceptions.ResourceInUseException;
import info.rio.project_ai901.exceptions.ResourceNotFoundException;
import info.rio.project_ai901.models.Category;
import info.rio.project_ai901.models.Product;
import info.rio.project_ai901.repositories.CategoryRepository;
import info.rio.project_ai901.repositories.InventoryRepository;
import info.rio.project_ai901.repositories.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductMapper mapper;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository,
            InventoryRepository inventoryRepository, ProductMapper mapper) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.inventoryRepository = inventoryRepository;
        this.mapper = mapper;
    }

    private int clampLimit(int limit) {
        if (limit <= 0) {
            return 20;
        }
        return Math.min(limit, 100);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private Product find(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + id));
    }

    private Category findCategory(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada: " + categoryId));
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<ProductSummaryResponse> list(long offset, int limit) {
        return list(offset, limit, null);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<ProductSummaryResponse> list(
            long offset,
            int limit,
            String search) {

        int size = clampLimit(limit);
        long safeOffset = Math.max(offset, 0);

        PageRequest pageable = PageRequest.of(
                (int) (safeOffset / size),
                size,
                Sort.by("sku").ascending()
        );

        Page<Product> pageResult;

        if (search == null || search.isBlank()) {
            pageResult = productRepository.findAll(pageable);
        } else {
            String term = search.trim();

            pageResult = productRepository
                    .findByNameContainingIgnoreCaseOrSkuContainingIgnoreCase(
                            term,
                            term,
                            pageable
                    );
        }

        List<ProductSummaryResponse> items = pageResult.getContent()
                .stream()
                .map(mapper::toSummary)
                .toList();

        return new PaginatedResponse<>(
                items,
                pageResult.getTotalElements(),
                safeOffset,
                size
        );
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse detail(UUID id) {
        return mapper.toDetail(find(id));
    }

    @Transactional
    public ProductDetailResponse create(CreateProductRequest request) {
        String sku = normalize(request.sku());
        String name = normalize(request.name());
        String unit = normalize(request.unit());

        if (productRepository.existsBySkuIgnoreCase(sku)) {
            throw new DuplicateResourceException("Ya existe un producto con el SKU: " + sku);
        }

        Category category = findCategory(request.categoryId());

        Product product = new Product();
        product.setSku(sku);
        product.setName(name);
        product.setCategory(category);
        product.setUnit(unit);
        product.setPrice(request.price());
        productRepository.save(product);
        return mapper.toDetail(product);
    }

    @Transactional
    public ProductDetailResponse update(UUID id, UpdateProductRequest request) {
        Product product = find(id);
        String sku = normalize(request.sku());
        String name = normalize(request.name());
        String unit = normalize(request.unit());

        if (productRepository.existsBySkuIgnoreCaseAndIdNot(sku, id)) {
            throw new DuplicateResourceException("Ya existe un producto con el SKU: " + sku);
        }

        Category category = findCategory(request.categoryId());

        product.setSku(sku);
        product.setName(name);
        product.setCategory(category);
        product.setUnit(unit);
        product.setPrice(request.price());
        productRepository.save(product);
        return mapper.toDetail(product);
    }

    @Transactional
    public void delete(UUID id) {
        Product product = find(id);
        if (inventoryRepository.countByProduct_Id(id) > 0) {
            throw new ResourceInUseException("No se puede eliminar el producto porque tiene inventario asociado");
        }
        productRepository.delete(product);
    }
}
