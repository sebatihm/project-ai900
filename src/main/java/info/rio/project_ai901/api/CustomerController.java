package info.rio.project_ai901.api;

import info.rio.project_ai901.api.dto.ApiResponse;
import info.rio.project_ai901.api.dto.CreateCustomerRequest;
import info.rio.project_ai901.api.dto.CustomerDetailResponse;
import info.rio.project_ai901.api.dto.CustomerSummaryResponse;
import info.rio.project_ai901.api.dto.PaginatedResponse;
import info.rio.project_ai901.api.dto.UpdateCustomerRequest;
import info.rio.project_ai901.models.enums.CustomerType;
import info.rio.project_ai901.services.customer.CustomerService;
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
@RequestMapping("/customer")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PaginatedResponse<CustomerSummaryResponse>>> list(
            @RequestParam(value = "type", required = false) CustomerType type,
            @RequestParam(value = "branchId", required = false) UUID branchId,
            @RequestParam(value = "offset", defaultValue = "0") long offset,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        PaginatedResponse<CustomerSummaryResponse> result = customerService.list(type, branchId, offset, limit);
        return ResponseEntity.ok(ApiResponse.ok(result, "Listado de clientes obtenido correctamente"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerDetailResponse>> detail(@PathVariable("id") UUID id) {
        CustomerDetailResponse result = customerService.detail(id);
        return ResponseEntity.ok(ApiResponse.ok(result, "Detalle de cliente obtenido correctamente"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerDetailResponse>> create(@Valid @RequestBody CreateCustomerRequest request) {
        CustomerDetailResponse result = customerService.create(request);
        return ResponseEntity.status(201).body(ApiResponse.created(result, "Cliente creado correctamente"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerDetailResponse>> update(@PathVariable("id") UUID id,
            @Valid @RequestBody UpdateCustomerRequest request) {
        CustomerDetailResponse result = customerService.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok(result, "Cliente actualizado correctamente"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") UUID id) {
        customerService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Cliente eliminado correctamente"));
    }
}
