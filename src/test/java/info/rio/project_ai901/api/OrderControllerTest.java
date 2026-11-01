package info.rio.project_ai901.api;

import info.rio.project_ai901.api.dto.OrderDetailResponse;
import info.rio.project_ai901.api.dto.OrderSummaryResponse;
import info.rio.project_ai901.api.dto.PaginatedResponse;
import info.rio.project_ai901.config.SecurityConfig;
import info.rio.project_ai901.exceptions.InvalidRequestException;
import info.rio.project_ai901.exceptions.ResourceInUseException;
import info.rio.project_ai901.exceptions.ResourceNotFoundException;
import info.rio.project_ai901.models.enums.OrderStatus;
import info.rio.project_ai901.services.order.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
@WithMockUser(roles = "ADMIN")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Test
    void listsOrdersWithStatusFilter() throws Exception {
        UUID id = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        when(orderService.list(eq(customerId), eq(null), eq(OrderStatus.RECEIVED), eq(0L), eq(20)))
                .thenReturn(new PaginatedResponse<>(List.of(new OrderSummaryResponse(id, "ORD-20260101-0001",
                        OrderStatus.RECEIVED, customerId, "Hotel Miramar", UUID.randomUUID(), "SUC-01",
                        LocalDateTime.of(2026, 1, 1, 10, 0), LocalDate.of(2026, 1, 10),
                        new BigDecimal("60.00"))), 1L, 0L, 20));

        mockMvc.perform(get("/order?customerId=" + customerId + "&status=RECEIVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].code").value("ORD-20260101-0001"))
                .andExpect(jsonPath("$.data.items[0].status").value("RECEIVED"));

        verify(orderService).list(customerId, null, OrderStatus.RECEIVED, 0L, 20);
    }

    @Test
    void returnsOrderDetailWithItems() throws Exception {
        UUID id = UUID.randomUUID();
        when(orderService.detail(id)).thenReturn(new OrderDetailResponse(id, "ORD-20260101-0001",
                OrderStatus.PREPARING, UUID.randomUUID(), "Hotel Miramar", UUID.randomUUID(), "SUC-01",
                "Sucursal Centro", LocalDateTime.of(2026, 1, 1, 10, 0), LocalDate.of(2026, 1, 10),
                new BigDecimal("60.00"), List.of()));

        mockMvc.perform(get("/order/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PREPARING"))
                .andExpect(jsonPath("$.data.total").value(60.00));
    }

    @Test
    void returns404WhenOrderDoesNotExist() throws Exception {
        UUID id = UUID.randomUUID();
        when(orderService.detail(id)).thenThrow(new ResourceNotFoundException("Orden no encontrada: " + id));

        mockMvc.perform(get("/order/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"));
    }

    @Test
    void createsOrderWith201() throws Exception {
        UUID id = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        when(orderService.create(any())).thenReturn(new OrderDetailResponse(id, "ORD-20260101-0001",
                OrderStatus.RECEIVED, customerId, "Hotel Miramar", UUID.randomUUID(), "SUC-01",
                "Sucursal Centro", LocalDateTime.of(2026, 1, 1, 10, 0), LocalDate.of(2026, 1, 10),
                new BigDecimal("60.00"), List.of()));

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(customerId, UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.status").value("RECEIVED"))
                .andExpect(jsonPath("$.data.total").value(60.00));
    }

    @Test
    void rejectsOrderWithoutItemsWith400() throws Exception {
        UUID branchId = UUID.randomUUID();
        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"" + UUID.randomUUID() + "\",\"branchId\":\"" + branchId
                                + "\",\"items\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validacion fallida"))
                .andExpect(jsonPath("$.errors[0]").value("items: La orden debe tener al menos un item"));

        verify(orderService, never()).create(any());
    }

    @Test
    void rejectsMissingCustomerWith400() throws Exception {
        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"branchId\":\"" + UUID.randomUUID() + "\",\"items\":[{\"productId\":\""
                                + UUID.randomUUID() + "\",\"quantity\":1}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value("customerId: El cliente es obligatorio"));
    }

    @Test
    void updatesOrder() throws Exception {
        UUID id = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        when(orderService.update(eq(id), any())).thenReturn(new OrderDetailResponse(id, "ORD-20260101-0001",
                OrderStatus.RECEIVED, customerId, "Hotel Miramar", UUID.randomUUID(), "SUC-01",
                "Sucursal Centro", LocalDateTime.of(2026, 1, 1, 10, 0), LocalDate.of(2026, 1, 10),
                new BigDecimal("75.00"), List.of()));

        mockMvc.perform(put("/order/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(customerId, UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(75.00));
    }

    @Test
    void returns409WhenUpdatingOrderNotInReceivedState() throws Exception {
        UUID id = UUID.randomUUID();
        when(orderService.update(eq(id), any()))
                .thenThrow(new ResourceInUseException("Solo se puede editar una orden en estado RECEIVED"));

        mockMvc.perform(put("/order/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Recurso en uso"));
    }

    @Test
    void updatesOrderStatus() throws Exception {
        UUID id = UUID.randomUUID();
        when(orderService.updateStatus(id, OrderStatus.PREPARING)).thenReturn(new OrderDetailResponse(id,
                "ORD-20260101-0001", OrderStatus.PREPARING, UUID.randomUUID(), "Hotel Miramar",
                UUID.randomUUID(), "SUC-01", "Sucursal Centro", LocalDateTime.of(2026, 1, 1, 10, 0),
                LocalDate.of(2026, 1, 10), new BigDecimal("60.00"), List.of()));

        mockMvc.perform(put("/order/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PREPARING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PREPARING"));
    }

    @Test
    void returns400OnInvalidStatusTransition() throws Exception {
        UUID id = UUID.randomUUID();
        when(orderService.updateStatus(id, OrderStatus.DELIVERED))
                .thenThrow(new InvalidRequestException("Transicion de estado no permitida: RECEIVED -> DELIVERED"));

        mockMvc.perform(put("/order/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DELIVERED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Solicitud invalida"))
                .andExpect(jsonPath("$.detail").value("Transicion de estado no permitida: RECEIVED -> DELIVERED"));
    }

    @Test
    void rejectsMissingStatusWith400() throws Exception {
        mockMvc.perform(put("/order/{id}/status", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value("status: El estado es obligatorio"));
    }

    @Test
    void deletesOrder() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/order/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(orderService).delete(id);
    }

    @Test
    void returns409WhenDeletingOrderNotCanceled() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ResourceInUseException("Solo se puede eliminar una orden en estado CANCELED"))
                .when(orderService).delete(id);

        mockMvc.perform(delete("/order/{id}", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Recurso en uso"));
    }

    private String orderBody(UUID customerId, UUID branchId) {
        return "{\"customerId\":\"" + customerId + "\",\"branchId\":\"" + branchId + "\","
                + "\"orderDate\":\"2026-01-15T10:00:00\",\"estimatedDeliveryDate\":\"2026-01-20\","
                + "\"items\":[{\"productId\":\"" + UUID.randomUUID() + "\",\"quantity\":2}]}";
    }
}
