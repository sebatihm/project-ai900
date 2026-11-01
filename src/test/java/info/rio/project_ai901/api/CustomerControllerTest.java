package info.rio.project_ai901.api;

import info.rio.project_ai901.api.dto.CustomerDetailResponse;
import info.rio.project_ai901.api.dto.CustomerSummaryResponse;
import info.rio.project_ai901.api.dto.PaginatedResponse;
import info.rio.project_ai901.config.SecurityConfig;
import info.rio.project_ai901.exceptions.DuplicateResourceException;
import info.rio.project_ai901.exceptions.ResourceInUseException;
import info.rio.project_ai901.exceptions.ResourceNotFoundException;
import info.rio.project_ai901.models.enums.CustomerType;
import info.rio.project_ai901.services.customer.CustomerService;
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
import static org.mockito.ArgumentMatchers.eq;
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

@WebMvcTest(CustomerController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
@WithMockUser(roles = "ADMIN")
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerService customerService;

    @Test
    void listsCustomersWithTypeAndBranchFilters() throws Exception {
        UUID id = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        when(customerService.list(eq(CustomerType.HOTEL), eq(branchId), eq(0L), eq(20))).thenReturn(
                new PaginatedResponse<>(List.of(new CustomerSummaryResponse(id, "Hotel Miramar",
                        CustomerType.HOTEL, "contacto@miramar.test", "555")), 1L, 0L, 20));

        mockMvc.perform(get("/customer?type=HOTEL&branchId=" + branchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].type").value("HOTEL"))
                .andExpect(jsonPath("$.data.items[0].email").value("contacto@miramar.test"));

        verify(customerService).list(CustomerType.HOTEL, branchId, 0L, 20);
    }

    @Test
    void listsCustomersWithoutFilters() throws Exception {
        when(customerService.list(isNull(), isNull(), eq(0L), eq(20))).thenReturn(
                new PaginatedResponse<>(List.of(), 0L, 0L, 20));

        mockMvc.perform(get("/customer"))
                .andExpect(status().isOk());

        verify(customerService).list(null, null, 0L, 20);
    }

    @Test
    void returnsCustomerDetail() throws Exception {
        UUID id = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        when(customerService.detail(id)).thenReturn(new CustomerDetailResponse(id, "Hotel Miramar",
                CustomerType.HOTEL, "contacto@miramar.test", "555", branchId, "Sucursal Centro"));

        mockMvc.perform(get("/customer/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Hotel Miramar"))
                .andExpect(jsonPath("$.data.branchName").value("Sucursal Centro"));
    }

    @Test
    void returns404WhenCustomerDoesNotExist() throws Exception {
        UUID id = UUID.randomUUID();
        when(customerService.detail(id)).thenThrow(new ResourceNotFoundException("Cliente no encontrado: " + id));

        mockMvc.perform(get("/customer/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"));
    }

    @Test
    void createsCustomerWith201() throws Exception {
        UUID id = UUID.randomUUID();
        when(customerService.create(any())).thenReturn(new CustomerDetailResponse(id, "Hotel Miramar",
                CustomerType.HOTEL, "contacto@miramar.test", "555", null, null));

        mockMvc.perform(post("/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hotel Miramar\",\"type\":\"HOTEL\","
                                + "\"email\":\"contacto@miramar.test\",\"phone\":\"555\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.type").value("HOTEL"));
    }

    @Test
    void rejectsBlankNameWith400() throws Exception {
        mockMvc.perform(post("/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"type\":\"HOTEL\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validacion fallida"))
                .andExpect(jsonPath("$.errors[0]").value("name: El nombre es obligatorio"));

        verify(customerService, never()).create(any());
    }

    @Test
    void rejectsInvalidEmailWith400() throws Exception {
        mockMvc.perform(post("/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hotel Miramar\",\"type\":\"HOTEL\",\"email\":\"no-es-correo\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value("email: El correo no tiene un formato valido"));
    }

    @Test
    void returns409WhenEmailAlreadyExists() throws Exception {
        when(customerService.create(any())).thenThrow(new DuplicateResourceException("Ya existe"));

        mockMvc.perform(post("/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Otro Hotel\",\"type\":\"HOTEL\","
                                + "\"email\":\"contacto@miramar.test\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Recurso duplicado"));
    }

    @Test
    void updatesCustomer() throws Exception {
        UUID id = UUID.randomUUID();
        when(customerService.update(eq(id), any())).thenReturn(new CustomerDetailResponse(id,
                "Hotel Miramar Norte", CustomerType.HOTEL, "nuevo@miramar.test", "666", null, null));

        mockMvc.perform(put("/customer/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hotel Miramar Norte\",\"type\":\"HOTEL\","
                                + "\"email\":\"nuevo@miramar.test\",\"phone\":\"666\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("nuevo@miramar.test"));
    }

    @Test
    void deletesCustomer() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/customer/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(customerService).delete(id);
    }

    @Test
    void returns409WhenCustomerHasOrders() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ResourceInUseException("No se puede eliminar el cliente porque tiene ordenes asociadas"))
                .when(customerService).delete(id);

        mockMvc.perform(delete("/customer/{id}", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Recurso en uso"));
    }
}
