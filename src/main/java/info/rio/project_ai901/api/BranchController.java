package info.rio.project_ai901.api;

import info.rio.project_ai901.api.dto.ApiResponse;
import info.rio.project_ai901.api.dto.BranchDetailResponse;
import info.rio.project_ai901.api.dto.BranchSummaryResponse;
import info.rio.project_ai901.api.dto.CreateBranchRequest;
import info.rio.project_ai901.api.dto.PaginatedResponse;
import info.rio.project_ai901.api.dto.UpdateBranchRequest;
import info.rio.project_ai901.services.branch.BranchService;
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
@RequestMapping("/branch")
public class BranchController {

    private final BranchService branchService;

    public BranchController(BranchService branchService) {
        this.branchService = branchService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PaginatedResponse<BranchSummaryResponse>>> list(
            @RequestParam(value = "includeInactive", defaultValue = "false") boolean includeInactive,
            @RequestParam(value = "offset", defaultValue = "0") long offset,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        PaginatedResponse<BranchSummaryResponse> result = branchService.list(includeInactive, offset, limit);
        return ResponseEntity.ok(ApiResponse.ok(result, "Listado de sucursales obtenido correctamente"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BranchDetailResponse>> detail(@PathVariable("id") UUID id) {
        BranchDetailResponse result = branchService.detail(id);
        return ResponseEntity.ok(ApiResponse.ok(result, "Detalle de sucursal obtenido correctamente"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BranchDetailResponse>> create(@Valid @RequestBody CreateBranchRequest request) {
        BranchDetailResponse result = branchService.create(request);
        return ResponseEntity.status(201).body(ApiResponse.created(result, "Sucursal creada correctamente"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BranchDetailResponse>> update(@PathVariable("id") UUID id,
            @Valid @RequestBody UpdateBranchRequest request) {
        BranchDetailResponse result = branchService.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok(result, "Sucursal actualizada correctamente"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<BranchDetailResponse>> deactivate(@PathVariable("id") UUID id) {
        BranchDetailResponse result = branchService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.ok(result, "Sucursal desactivada correctamente"));
    }
}
