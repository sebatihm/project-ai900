package info.rio.project_ai901.services.category;

import info.rio.project_ai901.api.dto.CategoryDetailResponse;
import info.rio.project_ai901.api.dto.CreateCategoryRequest;
import info.rio.project_ai901.api.dto.UpdateCategoryRequest;
import info.rio.project_ai901.exceptions.DuplicateResourceException;
import info.rio.project_ai901.exceptions.ResourceInUseException;
import info.rio.project_ai901.exceptions.ResourceNotFoundException;
import info.rio.project_ai901.models.Category;
import info.rio.project_ai901.repositories.CategoryRepository;
import info.rio.project_ai901.repositories.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CategoryServiceTest {

    private CategoryRepository categoryRepository;
    private ProductRepository productRepository;
    private CategoryService service;

    @BeforeEach
    void setUp() {
        categoryRepository = Mockito.mock(CategoryRepository.class);
        productRepository = Mockito.mock(ProductRepository.class);
        service = new CategoryService(categoryRepository, productRepository, new CategoryMapper());
    }

    @Test
    void createsCategoryWithNormalizedName() {
        when(categoryRepository.existsByNameIgnoreCase("Bebidas")).thenReturn(false);

        CategoryDetailResponse result = service.create(new CreateCategoryRequest("  Bebidas  "));

        assertThat(result.name()).isEqualTo("Bebidas");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void rejectsDuplicatedName() {
        when(categoryRepository.existsByNameIgnoreCase("Bebidas")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateCategoryRequest("Bebidas")))
                .isInstanceOf(DuplicateResourceException.class);
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void rejectsUpdateWithDuplicatedName() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.of(category(id, "Limpieza")));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("Bebidas", id)).thenReturn(true);

        assertThatThrownBy(() -> service.update(id, new UpdateCategoryRequest("Bebidas")))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void updatesCategorySuccessfully() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.of(category(id, "Limpieza")));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("Abarrotes", id)).thenReturn(false);

        CategoryDetailResponse result = service.update(id, new UpdateCategoryRequest("Abarrotes"));

        assertThat(result.id()).isEqualTo(id);
        assertThat(result.name()).isEqualTo("Abarrotes");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void throwsNotFoundForUnknownCategory() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.detail(id)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void refusesToDeleteCategoryWithProducts() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.of(category(id, "Bebidas")));
        when(productRepository.countByCategory_Id(id)).thenReturn(2L);

        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(ResourceInUseException.class);
        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    void deletesCategoryWithoutProducts() {
        UUID id = UUID.randomUUID();
        Category category = category(id, "Bebidas");
        when(categoryRepository.findById(id)).thenReturn(Optional.of(category));
        when(productRepository.countByCategory_Id(id)).thenReturn(0L);

        service.delete(id);

        verify(categoryRepository).delete(category);
    }

    private Category category(UUID id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        return category;
    }
}
