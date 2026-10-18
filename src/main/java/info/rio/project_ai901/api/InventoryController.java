package info.rio.project_ai901.api;

import info.rio.project_ai901.api.dto.ApiResponse;
import info.rio.project_ai901.api.dto.CreateInventoryRequest;
import info.rio.project_ai901.api.dto.InventoryDetailResponse;
import info.rio.project_ai901.api.dto.InventorySummaryResponse;
import info.rio.project_ai901.api.dto.PaginatedResponse;
import info.rio.project_ai901.api.dto.UpdateInventoryRequest;
import info.rio.project_ai901.services.inventory.InventoryService;
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
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PaginatedResponse<InventorySummaryResponse>>> list(
            @RequestParam(value = "warehouseId", required = false) UUID warehouseId,
            @RequestParam(value = "productId", required = false) UUID productId,
            @RequestParam(value = "offset", defaultValue = "0") long offset,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        PaginatedResponse<InventorySummaryResponse> result = inventoryService.list(warehouseId, productId, offset, limit);
        return ResponseEntity.ok(ApiResponse.ok(result, "Listado de inventario obtenido correctamente"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InventoryDetailResponse>> detail(@PathVariable("id") UUID id) {
        InventoryDetailResponse result = inventoryService.detail(id);
        return ResponseEntity.ok(ApiResponse.ok(result, "Detalle de inventario obtenido correctamente"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<InventoryDetailResponse>> create(
            @Valid @RequestBody CreateInventoryRequest request) {
        InventoryDetailResponse result = inventoryService.create(request);
        return ResponseEntity.status(201).body(ApiResponse.created(result, "Inventario creado correctamente"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<InventoryDetailResponse>> update(@PathVariable("id") UUID id,
            @Valid @RequestBody UpdateInventoryRequest request) {
        InventoryDetailResponse result = inventoryService.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok(result, "Inventario actualizado correctamente"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") UUID id) {
        inventoryService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Inventario eliminado correctamente"));
    }
}
