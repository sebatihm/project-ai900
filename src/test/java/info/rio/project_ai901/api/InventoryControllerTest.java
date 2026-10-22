package info.rio.project_ai901.api;

import info.rio.project_ai901.api.dto.InventoryDetailResponse;
import info.rio.project_ai901.api.dto.InventorySummaryResponse;
import info.rio.project_ai901.api.dto.PaginatedResponse;
import info.rio.project_ai901.config.SecurityConfig;
import info.rio.project_ai901.exceptions.DuplicateResourceException;
import info.rio.project_ai901.exceptions.InvalidRequestException;
import info.rio.project_ai901.exceptions.ResourceNotFoundException;
import info.rio.project_ai901.services.inventory.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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

@WebMvcTest(InventoryController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryService inventoryService;

    @Test
    void listsInventoryWithWarehouseFilter() throws Exception {
        UUID id = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(inventoryService.list(eq(warehouseId), eq(null), eq(0L), eq(20))).thenReturn(
                new PaginatedResponse<>(List.of(new InventorySummaryResponse(id, warehouseId, "ALM-01",
                        "Almacen Norte", productId, "SKU-001", "Coca Cola", 25)), 1L, 0L, 20));

        mockMvc.perform(get("/inventory?warehouseId=" + warehouseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].warehouseCode").value("ALM-01"))
                .andExpect(jsonPath("$.data.items[0].quantity").value(25));

        verify(inventoryService).list(warehouseId, null, 0L, 20);
    }

    @Test
    void returnsInventoryDetail() throws Exception {
        UUID id = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(inventoryService.detail(id)).thenReturn(new InventoryDetailResponse(id, warehouseId, "ALM-01",
                "Almacen Norte", branchId, "Sucursal Centro", productId, "SKU-001", "Coca Cola", 25));

        mockMvc.perform(get("/inventory/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.branchName").value("Sucursal Centro"))
                .andExpect(jsonPath("$.data.quantity").value(25));
    }

    @Test
    void returns404WhenInventoryDoesNotExist() throws Exception {
        UUID id = UUID.randomUUID();
        when(inventoryService.detail(id))
                .thenThrow(new ResourceNotFoundException("Inventario no encontrado: " + id));

        mockMvc.perform(get("/inventory/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"));
    }

    @Test
    void createsInventoryWith201() throws Exception {
        UUID id = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(inventoryService.create(any())).thenReturn(new InventoryDetailResponse(id, warehouseId, "ALM-01",
                "Almacen Norte", UUID.randomUUID(), "Sucursal Centro", productId, "SKU-001", "Coca Cola", 25));

        mockMvc.perform(post("/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"warehouseId\":\"" + warehouseId + "\",\"productId\":\"" + productId
                                + "\",\"quantity\":25}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.quantity").value(25));
    }

    @Test
    void rejectsNegativeQuantityWith400() throws Exception {
        mockMvc.perform(post("/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"warehouseId\":\"" + UUID.randomUUID() + "\",\"productId\":\""
                                + UUID.randomUUID() + "\",\"quantity\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validacion fallida"))
                .andExpect(jsonPath("$.errors[0]").value("quantity: La cantidad no puede ser negativa"));

        verify(inventoryService, never()).create(any());
    }

    @Test
    void rejectsMissingQuantityWith400() throws Exception {
        mockMvc.perform(post("/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"warehouseId\":\"" + UUID.randomUUID() + "\",\"productId\":\""
                                + UUID.randomUUID() + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value("quantity: La cantidad es obligatoria"));
    }

    @Test
    void returns409WhenWarehouseProductPairAlreadyExists() throws Exception {
        when(inventoryService.create(any())).thenThrow(new DuplicateResourceException("Ya existe inventario"));

        mockMvc.perform(post("/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"warehouseId\":\"" + UUID.randomUUID() + "\",\"productId\":\""
                                + UUID.randomUUID() + "\",\"quantity\":10}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Recurso duplicado"));
    }

    @Test
    void returns400WhenWarehouseIsNotActive() throws Exception {
        when(inventoryService.create(any())).thenThrow(new InvalidRequestException("El almacen no esta activo"));

        mockMvc.perform(post("/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"warehouseId\":\"" + UUID.randomUUID() + "\",\"productId\":\""
                                + UUID.randomUUID() + "\",\"quantity\":10}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Solicitud invalida"));
    }

    @Test
    void updatesInventoryQuantity() throws Exception {
        UUID id = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(inventoryService.update(eq(id), any())).thenReturn(new InventoryDetailResponse(id, warehouseId,
                "ALM-01", "Almacen Norte", UUID.randomUUID(), "Sucursal Centro", productId, "SKU-001",
                "Coca Cola", 42));

        mockMvc.perform(put("/inventory/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":42}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quantity").value(42));
    }

    @Test
    void deletesInventory() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/inventory/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(inventoryService).delete(id);
    }

    @Test
    void returns404WhenDeletingUnknownInventory() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("Inventario no encontrado: " + id)).when(inventoryService).delete(id);

        mockMvc.perform(delete("/inventory/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"));
    }
}
