package info.rio.project_ai901.api;

import info.rio.project_ai901.api.dto.PaginatedResponse;
import info.rio.project_ai901.api.dto.SupplierDetailResponse;
import info.rio.project_ai901.api.dto.SupplierSummaryResponse;
import info.rio.project_ai901.config.SecurityConfig;
import info.rio.project_ai901.exceptions.DuplicateResourceException;
import info.rio.project_ai901.exceptions.ResourceInUseException;
import info.rio.project_ai901.exceptions.ResourceNotFoundException;
import info.rio.project_ai901.services.supplier.SupplierService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
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

@WebMvcTest(SupplierController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
@WithMockUser(roles = "ADMIN")
class SupplierControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SupplierService supplierService;

    @Test
    void listsSuppliers() throws Exception {
        UUID id = UUID.randomUUID();
        when(supplierService.list(0L, 20)).thenReturn(new PaginatedResponse<>(
                List.of(new SupplierSummaryResponse(id, "Proveedor Demo", "ABC123",
                        "ventas@demo.test", "http://frontend.test/invoice-analyzer/" + id)),
                1L, 0L, 20));

        mockMvc.perform(get("/supplier"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].name").value("Proveedor Demo"))
                .andExpect(jsonPath("$.data.items[0].rfc").value("ABC123"))
                .andExpect(jsonPath("$.data.items[0].invoiceAnalyzerUrl")
                        .value("http://frontend.test/invoice-analyzer/" + id));

        verify(supplierService).list(0L, 20);
    }

    @Test
    void returnsSupplierDetail() throws Exception {
        UUID id = UUID.randomUUID();
        when(supplierService.detail(id)).thenReturn(new SupplierDetailResponse(id, "Proveedor Demo",
                "ABC123", "ventas@demo.test", "http://frontend.test/invoice-analyzer/" + id));

        mockMvc.perform(get("/supplier/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Proveedor Demo"));
    }

    @Test
    void returns404WhenSupplierDoesNotExist() throws Exception {
        UUID id = UUID.randomUUID();
        when(supplierService.detail(id)).thenThrow(new ResourceNotFoundException("Proveedor no encontrado: " + id));

        mockMvc.perform(get("/supplier/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"));
    }

    @Test
    void createsSupplierWith201() throws Exception {
        UUID id = UUID.randomUUID();
        when(supplierService.create(any())).thenReturn(new SupplierDetailResponse(id, "Proveedor Demo",
                "ABC123", "ventas@demo.test", "http://frontend.test/invoice-analyzer/" + id));

        mockMvc.perform(post("/supplier")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Proveedor Demo\",\"rfc\":\"ABC123\","
                                + "\"email\":\"ventas@demo.test\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.rfc").value("ABC123"));
    }

    @Test
    void rejectsBlankNameWith400() throws Exception {
        mockMvc.perform(post("/supplier")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"rfc\":\"ABC123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validacion fallida"))
                .andExpect(jsonPath("$.errors[0]").value("name: El nombre es obligatorio"));

        verify(supplierService, never()).create(any());
    }

    @Test
    void rejectsRfcTooLongWith400() throws Exception {
        mockMvc.perform(post("/supplier")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Proveedor Demo\",\"rfc\":\"ABC12345678901\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]")
                        .value("rfc: El RFC no puede superar 13 caracteres"));
    }

    @Test
    void rejectsInvalidEmailWith400() throws Exception {
        mockMvc.perform(post("/supplier")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Proveedor Demo\",\"rfc\":\"ABC123\",\"email\":\"no-es-correo\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value("email: El correo no tiene un formato valido"));
    }

    @Test
    void returns409WhenRfcAlreadyExists() throws Exception {
        when(supplierService.create(any())).thenThrow(new DuplicateResourceException("Ya existe"));

        mockMvc.perform(post("/supplier")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Proveedor Demo\",\"rfc\":\"ABC123\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Recurso duplicado"));
    }

    @Test
    void updatesSupplier() throws Exception {
        UUID id = UUID.randomUUID();
        when(supplierService.update(eq(id), any())).thenReturn(new SupplierDetailResponse(id,
                "Proveedor Nuevo", "NEW123", "nuevo@demo.test", "http://frontend.test/invoice-analyzer/" + id));

        mockMvc.perform(put("/supplier/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Proveedor Nuevo\",\"rfc\":\"NEW123\","
                                + "\"email\":\"nuevo@demo.test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rfc").value("NEW123"));
    }

    @Test
    void deletesSupplier() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/supplier/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(supplierService).delete(id);
    }

    @Test
    void returns409WhenSupplierHasInvoices() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ResourceInUseException(
                "No se puede eliminar el proveedor porque tiene facturas asociadas"))
                .when(supplierService).delete(id);

        mockMvc.perform(delete("/supplier/{id}", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Recurso en uso"))
                .andExpect(jsonPath("$.detail")
                        .value("No se puede eliminar el proveedor porque tiene facturas asociadas"));
    }

    @Test
    @WithAnonymousUser
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/supplier"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "WAREHOUSE_HEAD")
    void allowsAnyAuthenticatedRole() throws Exception {
        when(supplierService.list(0L, 20))
                .thenReturn(new PaginatedResponse<>(List.of(), 0L, 0L, 20));

        mockMvc.perform(get("/supplier"))
                .andExpect(status().isOk());
    }
}
