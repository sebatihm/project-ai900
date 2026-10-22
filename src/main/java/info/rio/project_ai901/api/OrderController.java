package info.rio.project_ai901.api;

import info.rio.project_ai901.api.dto.ApiResponse;
import info.rio.project_ai901.api.dto.CreateOrderRequest;
import info.rio.project_ai901.api.dto.OrderDetailResponse;
import info.rio.project_ai901.api.dto.OrderSummaryResponse;
import info.rio.project_ai901.api.dto.PaginatedResponse;
import info.rio.project_ai901.api.dto.UpdateOrderRequest;
import info.rio.project_ai901.api.dto.UpdateOrderStatusRequest;
import info.rio.project_ai901.models.enums.OrderStatus;
import info.rio.project_ai901.services.order.OrderService;
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
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PaginatedResponse<OrderSummaryResponse>>> list(
            @RequestParam(value = "customerId", required = false) UUID customerId,
            @RequestParam(value = "branchId", required = false) UUID branchId,
            @RequestParam(value = "status", required = false) OrderStatus status,
            @RequestParam(value = "offset", defaultValue = "0") long offset,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        PaginatedResponse<OrderSummaryResponse> result = orderService.list(customerId, branchId, status, offset, limit);
        return ResponseEntity.ok(ApiResponse.ok(result, "Listado de ordenes obtenido correctamente"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> detail(@PathVariable("id") UUID id) {
        OrderDetailResponse result = orderService.detail(id);
        return ResponseEntity.ok(ApiResponse.ok(result, "Detalle de orden obtenido correctamente"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderDetailResponse>> create(@Valid @RequestBody CreateOrderRequest request) {
        OrderDetailResponse result = orderService.create(request);
        return ResponseEntity.status(201).body(ApiResponse.created(result, "Orden creada correctamente"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> update(@PathVariable("id") UUID id,
            @Valid @RequestBody UpdateOrderRequest request) {
        OrderDetailResponse result = orderService.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok(result, "Orden actualizada correctamente"));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> updateStatus(@PathVariable("id") UUID id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        OrderDetailResponse result = orderService.updateStatus(id, request.status());
        return ResponseEntity.ok(ApiResponse.ok(result, "Estado de la orden actualizado correctamente"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") UUID id) {
        orderService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Orden eliminada correctamente"));
    }
}
