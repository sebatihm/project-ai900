package info.rio.project_ai901.api;

import info.rio.project_ai901.api.dto.ApiResponse;
import info.rio.project_ai901.api.dto.CreateWarehouseRequest;
import info.rio.project_ai901.api.dto.PaginatedResponse;
import info.rio.project_ai901.api.dto.UpdateWarehouseRequest;
import info.rio.project_ai901.api.dto.WarehouseDetailResponse;
import info.rio.project_ai901.api.dto.WarehouseSummaryResponse;
import info.rio.project_ai901.services.warehouse.WarehouseService;
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
@RequestMapping("/warehouse")
public class WarehouseController {

    private final WarehouseService warehouseService;

    public WarehouseController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PaginatedResponse<WarehouseSummaryResponse>>> list(
            @RequestParam(value = "branchId", required = false) UUID branchId,
            @RequestParam(value = "includeInactive", defaultValue = "false") boolean includeInactive,
            @RequestParam(value = "offset", defaultValue = "0") long offset,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        PaginatedResponse<WarehouseSummaryResponse> result = warehouseService.list(includeInactive, branchId, offset,
                limit);
        return ResponseEntity.ok(ApiResponse.ok(result, "Listado de almacenes obtenido correctamente"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WarehouseDetailResponse>> detail(@PathVariable("id") UUID id) {
        WarehouseDetailResponse result = warehouseService.detail(id);
        return ResponseEntity.ok(ApiResponse.ok(result, "Detalle de almacen obtenido correctamente"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WarehouseDetailResponse>> create(
            @Valid @RequestBody CreateWarehouseRequest request) {
        WarehouseDetailResponse result = warehouseService.create(request);
        return ResponseEntity.status(201).body(ApiResponse.created(result, "Almacen creado correctamente"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<WarehouseDetailResponse>> update(@PathVariable("id") UUID id,
            @Valid @RequestBody UpdateWarehouseRequest request) {
        WarehouseDetailResponse result = warehouseService.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok(result, "Almacen actualizado correctamente"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<WarehouseDetailResponse>> deactivate(@PathVariable("id") UUID id) {
        WarehouseDetailResponse result = warehouseService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.ok(result, "Almacen desactivado correctamente"));
    }
}
