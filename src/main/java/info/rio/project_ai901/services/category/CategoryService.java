package info.rio.project_ai901.services.category;

import info.rio.project_ai901.api.dto.CategoryDetailResponse;
import info.rio.project_ai901.api.dto.CategorySummaryResponse;
import info.rio.project_ai901.api.dto.CreateCategoryRequest;
import info.rio.project_ai901.api.dto.PaginatedResponse;
import info.rio.project_ai901.api.dto.UpdateCategoryRequest;
import info.rio.project_ai901.exceptions.DuplicateResourceException;
import info.rio.project_ai901.exceptions.ResourceInUseException;
import info.rio.project_ai901.exceptions.ResourceNotFoundException;
import info.rio.project_ai901.models.Category;
import info.rio.project_ai901.repositories.CategoryRepository;
import info.rio.project_ai901.repositories.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CategoryMapper mapper;

    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository,
            CategoryMapper mapper) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
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

    private Category find(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada: " + id));
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<CategorySummaryResponse> list(long offset, int limit) {
        int size = clampLimit(limit);
        long safeOffset = Math.max(offset, 0);
        PageRequest pageable = PageRequest.of((int) (safeOffset / size), size, Sort.by("name").ascending());
        Page<Category> pageResult = categoryRepository.findAll(pageable);
        List<CategorySummaryResponse> items = pageResult.getContent().stream()
                .map(mapper::toSummary)
                .toList();
        return new PaginatedResponse<>(items, pageResult.getTotalElements(), safeOffset, size);
    }

    @Transactional(readOnly = true)
    public CategoryDetailResponse detail(UUID id) {
        return mapper.toDetail(find(id));
    }

    @Transactional
    public CategoryDetailResponse create(CreateCategoryRequest request) {
        String name = normalize(request.name());
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Ya existe una categoria con el nombre: " + name);
        }
        Category category = new Category();
        category.setName(name);
        categoryRepository.save(category);
        return mapper.toDetail(category);
    }

    @Transactional
    public CategoryDetailResponse update(UUID id, UpdateCategoryRequest request) {
        Category category = find(id);
        String name = normalize(request.name());
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateResourceException("Ya existe una categoria con el nombre: " + name);
        }
        category.setName(name);
        categoryRepository.save(category);
        return mapper.toDetail(category);
    }

    @Transactional
    public void delete(UUID id) {
        Category category = find(id);
        long productCount = productRepository.countByCategory_Id(id);
        if (productCount > 0) {
            throw new ResourceInUseException("No se puede eliminar la categoria porque tiene productos asociados");
        }
        categoryRepository.delete(category);
    }
}
