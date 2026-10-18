package info.rio.project_ai901.api;

import info.rio.project_ai901.api.dto.ApiResponse;
import info.rio.project_ai901.api.dto.CategoryDetailResponse;
import info.rio.project_ai901.api.dto.CategorySummaryResponse;
import info.rio.project_ai901.api.dto.CreateCategoryRequest;
import info.rio.project_ai901.api.dto.PaginatedResponse;
import info.rio.project_ai901.api.dto.UpdateCategoryRequest;
import info.rio.project_ai901.services.category.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Validated
@RestController
@RequestMapping("/category")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PaginatedResponse<CategorySummaryResponse>>> list(
            @RequestParam(value = "offset", defaultValue = "0") long offset,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        PaginatedResponse<CategorySummaryResponse> result = categoryService.list(offset, limit);
        return ResponseEntity.ok(ApiResponse.ok(result, "Listado de categorias obtenido correctamente"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryDetailResponse>> detail(@PathVariable("id") UUID id) {
        CategoryDetailResponse result = categoryService.detail(id);
        return ResponseEntity.ok(ApiResponse.ok(result, "Detalle de categoria obtenido correctamente"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryDetailResponse>> create(@Valid @RequestBody CreateCategoryRequest request) {
        CategoryDetailResponse result = categoryService.create(request);
        return ResponseEntity.status(201).body(ApiResponse.created(result, "Categoria creada correctamente"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryDetailResponse>> update(@PathVariable("id") UUID id,
            @Valid @RequestBody UpdateCategoryRequest request) {
        CategoryDetailResponse result = categoryService.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok(result, "Categoria actualizada correctamente"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") UUID id) {
        categoryService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Categoria eliminada correctamente"));
    }
}
