package info.rio.project_ai901.api;

import info.rio.project_ai901.api.dto.PaginatedResponse;
import info.rio.project_ai901.api.dto.WarehouseDetailResponse;
import info.rio.project_ai901.api.dto.WarehouseSummaryResponse;
import info.rio.project_ai901.config.SecurityConfig;
import info.rio.project_ai901.exceptions.DuplicateResourceException;
import info.rio.project_ai901.exceptions.InvalidRequestException;
import info.rio.project_ai901.exceptions.ResourceNotFoundException;
import info.rio.project_ai901.services.warehouse.WarehouseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
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

@WebMvcTest(WarehouseController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
@WithMockUser(roles = "ADMIN")
class WarehouseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WarehouseService warehouseService;

    @Test
    void listsWarehousesWithFilters() throws Exception {
        UUID id = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        when(warehouseService.list(false, branchId, 0L, 20)).thenReturn(new PaginatedResponse<>(
                List.of(new WarehouseSummaryResponse(id, "ALM-01", "Almacen Norte", branchId, "Sucursal Centro")),
                1L, 0L, 20));

        mockMvc.perform(get("/warehouse?branchId=" + branchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].code").value("ALM-01"))
                .andExpect(jsonPath("$.data.total").value(1));

        verify(warehouseService).list(false, branchId, 0L, 20);
    }

    @Test
    void listsWithoutFilters() throws Exception {
        when(warehouseService.list(anyBoolean(), isNull(), anyLong(), anyInt())).thenReturn(
                new PaginatedResponse<>(List.of(), 0L, 0L, 20));

        mockMvc.perform(get("/warehouse?includeInactive=true"))
                .andExpect(status().isOk());

        verify(warehouseService).list(true, null, 0L, 20);
    }

    @Test
    void returnsWarehouseDetail() throws Exception {
        UUID id = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        when(warehouseService.detail(id)).thenReturn(new WarehouseDetailResponse(id, "ALM-01", "Almacen Norte",
                true, branchId, "Sucursal Centro", "CDMX"));

        mockMvc.perform(get("/warehouse/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.warehouseId").value(id.toString()))
                .andExpect(jsonPath("$.data.branchCity").value("CDMX"));
    }

    @Test
    void returns404WhenWarehouseDoesNotExist() throws Exception {
        UUID id = UUID.randomUUID();
        when(warehouseService.detail(id)).thenThrow(new ResourceNotFoundException("Almacen no encontrado: " + id));

        mockMvc.perform(get("/warehouse/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"));
    }

    @Test
    void createsWarehouseWith201() throws Exception {
        UUID id = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        when(warehouseService.create(any())).thenReturn(new WarehouseDetailResponse(id, "ALM-01",
                "Almacen Norte", true, branchId, "Sucursal Centro", "CDMX"));

        mockMvc.perform(post("/warehouse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"ALM-01\",\"name\":\"Almacen Norte\",\"branchId\":\"" + branchId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.code").value("ALM-01"));
    }

    @Test
    void rejectsMissingBranchIdWith400() throws Exception {
        mockMvc.perform(post("/warehouse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"ALM-01\",\"name\":\"Almacen Norte\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validacion fallida"))
                .andExpect(jsonPath("$.errors[0]").value("branchId: La sucursal es obligatoria"));

        verify(warehouseService, never()).create(any());
    }

    @Test
    void returns409WhenWarehouseCodeAlreadyExists() throws Exception {
        when(warehouseService.create(any())).thenThrow(new DuplicateResourceException("Ya existe"));

        mockMvc.perform(post("/warehouse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"ALM-01\",\"name\":\"Otro\",\"branchId\":\""
                                + UUID.randomUUID() + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Recurso duplicado"));
    }

    @Test
    void returns400WhenWarehouseBranchIsNotActive() throws Exception {
        when(warehouseService.create(any()))
                .thenThrow(new InvalidRequestException("El almacen no esta activo"));

        mockMvc.perform(post("/warehouse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"ALM-01\",\"name\":\"Otro\",\"branchId\":\""
                                + UUID.randomUUID() + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Solicitud invalida"));
    }

    @Test
    void updatesWarehouse() throws Exception {
        UUID id = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        when(warehouseService.update(any(), any())).thenReturn(new WarehouseDetailResponse(id, "ALM-02",
                "Almacen Sur", true, branchId, "Sucursal Centro", "CDMX"));

        mockMvc.perform(put("/warehouse/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"ALM-02\",\"name\":\"Almacen Sur\",\"branchId\":\"" + branchId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("ALM-02"));
    }

    @Test
    void deactivatesWarehouse() throws Exception {
        UUID id = UUID.randomUUID();
        when(warehouseService.deactivate(id)).thenReturn(new WarehouseDetailResponse(id, "ALM-01",
                "Almacen Norte", false, UUID.randomUUID(), "Sucursal Centro", "CDMX"));

        mockMvc.perform(delete("/warehouse/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));
    }
}
