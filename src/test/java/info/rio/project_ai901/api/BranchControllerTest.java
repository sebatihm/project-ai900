package info.rio.project_ai901.api;

import info.rio.project_ai901.api.dto.BranchDetailResponse;
import info.rio.project_ai901.api.dto.BranchSummaryResponse;
import info.rio.project_ai901.api.dto.PaginatedResponse;
import info.rio.project_ai901.config.SecurityConfig;
import info.rio.project_ai901.exceptions.DuplicateResourceException;
import info.rio.project_ai901.exceptions.ResourceInUseException;
import info.rio.project_ai901.exceptions.ResourceNotFoundException;
import info.rio.project_ai901.services.branch.BranchService;
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

@WebMvcTest(BranchController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
@WithMockUser(roles = "ADMIN")
class BranchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BranchService branchService;

    @Test
    void listsActiveBranchesByDefault() throws Exception {
        UUID id = UUID.randomUUID();
        when(branchService.list(false, 0L, 20)).thenReturn(new PaginatedResponse<>(
                List.of(new BranchSummaryResponse(id, "SUC-01", "Sucursal Centro", "CDMX")), 1L, 0L, 20));

        mockMvc.perform(get("/branch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.items[0].code").value("SUC-01"))
                .andExpect(jsonPath("$.data.total").value(1));

        verify(branchService).list(false, 0L, 20);
    }

    @Test
    void listsIncludingInactiveWhenRequested() throws Exception {
        when(branchService.list(true, 0L, 20)).thenReturn(new PaginatedResponse<>(List.of(), 0L, 0L, 20));

        mockMvc.perform(get("/branch?includeInactive=true"))
                .andExpect(status().isOk());

        verify(branchService).list(true, 0L, 20);
    }

    @Test
    void returnsBranchDetail() throws Exception {
        UUID id = UUID.randomUUID();
        when(branchService.detail(id)).thenReturn(new BranchDetailResponse(id, "SUC-01", "Sucursal Centro",
                "CDMX", true, 2L));

        mockMvc.perform(get("/branch/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.branchId").value(id.toString()))
                .andExpect(jsonPath("$.data.active").value(true))
                .andExpect(jsonPath("$.data.warehouseCount").value(2));
    }

    @Test
    void returns404WhenBranchDoesNotExist() throws Exception {
        UUID id = UUID.randomUUID();
        when(branchService.detail(id)).thenThrow(new ResourceNotFoundException("Sucursal no encontrada: " + id));

        mockMvc.perform(get("/branch/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"));
    }

    @Test
    void createsBranchWith201() throws Exception {
        UUID id = UUID.randomUUID();
        when(branchService.create(any())).thenReturn(new BranchDetailResponse(id, "SUC-01", "Sucursal Centro",
                "CDMX", true, 0L));

        mockMvc.perform(post("/branch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"SUC-01\",\"name\":\"Sucursal Centro\",\"city\":\"CDMX\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.code").value("SUC-01"));
    }

    @Test
    void rejectsBlankCodeWith400() throws Exception {
        mockMvc.perform(post("/branch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"\",\"name\":\"Sucursal Centro\",\"city\":\"CDMX\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validacion fallida"))
                .andExpect(jsonPath("$.errors[0]").value("code: El codigo es obligatorio"));

        verify(branchService, never()).create(any());
    }

    @Test
    void returns409WhenBranchCodeAlreadyExists() throws Exception {
        when(branchService.create(any())).thenThrow(new DuplicateResourceException("Ya existe"));

        mockMvc.perform(post("/branch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"SUC-01\",\"name\":\"Otra\",\"city\":\"CDMX\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Recurso duplicado"));
    }

    @Test
    void updatesBranch() throws Exception {
        UUID id = UUID.randomUUID();
        when(branchService.update(any(), any())).thenReturn(new BranchDetailResponse(id, "SUC-02",
                "Sucursal Norte", "Puebla", true, 0L));

        mockMvc.perform(put("/branch/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"SUC-02\",\"name\":\"Sucursal Norte\",\"city\":\"Puebla\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.city").value("Puebla"));
    }

    @Test
    void deactivatesBranch() throws Exception {
        UUID id = UUID.randomUUID();
        when(branchService.deactivate(id)).thenReturn(new BranchDetailResponse(id, "SUC-01",
                "Sucursal Centro", "CDMX", false, 0L));

        mockMvc.perform(delete("/branch/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));
    }

    @Test
    void returns409WhenBranchHasActiveWarehouses() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ResourceInUseException("La sucursal tiene almacenes activos")).when(branchService).deactivate(id);

        mockMvc.perform(delete("/branch/{id}", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Recurso en uso"));
    }
}
