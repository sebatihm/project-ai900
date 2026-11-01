package info.rio.project_ai901.api;

import info.rio.project_ai901.api.dto.ApiResponse;
import info.rio.project_ai901.api.dto.CreateSupplierRequest;
import info.rio.project_ai901.api.dto.PaginatedResponse;
import info.rio.project_ai901.api.dto.SupplierDetailResponse;
import info.rio.project_ai901.api.dto.SupplierSummaryResponse;
import info.rio.project_ai901.api.dto.UpdateSupplierRequest;
import info.rio.project_ai901.services.supplier.SupplierService;
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
@RequestMapping("/supplier")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PaginatedResponse<SupplierSummaryResponse>>> list(
            @RequestParam(value = "offset", defaultValue = "0") long offset,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        PaginatedResponse<SupplierSummaryResponse> result = supplierService.list(offset, limit);
        return ResponseEntity.ok(ApiResponse.ok(result, "Listado de proveedores obtenido correctamente"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SupplierDetailResponse>> detail(@PathVariable("id") UUID id) {
        SupplierDetailResponse result = supplierService.detail(id);
        return ResponseEntity.ok(ApiResponse.ok(result, "Detalle de proveedor obtenido correctamente"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SupplierDetailResponse>> create(@Valid @RequestBody CreateSupplierRequest request) {
        SupplierDetailResponse result = supplierService.create(request);
        return ResponseEntity.status(201).body(ApiResponse.created(result, "Proveedor creado correctamente"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SupplierDetailResponse>> update(@PathVariable("id") UUID id,
            @Valid @RequestBody UpdateSupplierRequest request) {
        SupplierDetailResponse result = supplierService.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok(result, "Proveedor actualizado correctamente"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") UUID id) {
        supplierService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Proveedor eliminado correctamente"));
    }
}
