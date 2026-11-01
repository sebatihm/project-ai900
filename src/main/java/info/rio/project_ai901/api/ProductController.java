package info.rio.project_ai901.api;

import info.rio.project_ai901.api.dto.ApiResponse;
import info.rio.project_ai901.api.dto.CreateProductRequest;
import info.rio.project_ai901.api.dto.PaginatedResponse;
import info.rio.project_ai901.api.dto.ProductDetailResponse;
import info.rio.project_ai901.api.dto.ProductSummaryResponse;
import info.rio.project_ai901.api.dto.UpdateProductRequest;
import info.rio.project_ai901.services.product.ProductService;
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
@RequestMapping("/product")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PaginatedResponse<ProductSummaryResponse>>> list(
            @RequestParam(value = "offset", defaultValue = "0") long offset,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            @RequestParam(value = "search", required = false) String search) {

        PaginatedResponse<ProductSummaryResponse> result =
                productService.list(offset, limit, search);

        return ResponseEntity.ok(
                ApiResponse.ok(result, "Listado de productos obtenido correctamente"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> detail(@PathVariable("id") UUID id) {
        ProductDetailResponse result = productService.detail(id);
        return ResponseEntity.ok(ApiResponse.ok(result, "Detalle de producto obtenido correctamente"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductDetailResponse>> create(@Valid @RequestBody CreateProductRequest request) {
        ProductDetailResponse result = productService.create(request);
        return ResponseEntity.status(201).body(ApiResponse.created(result, "Producto creado correctamente"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> update(@PathVariable("id") UUID id,
            @Valid @RequestBody UpdateProductRequest request) {
        ProductDetailResponse result = productService.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok(result, "Producto actualizado correctamente"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") UUID id) {
        productService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Producto eliminado correctamente"));
    }
}
