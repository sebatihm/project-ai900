package info.rio.project_ai901.e2e;

import info.rio.project_ai901.models.enums.CustomerType;
import info.rio.project_ai901.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CustomerE2ETest extends AbstractIntegrationTest {

    @Test
    void creaClienteYLoObtienePorId() throws Exception {
        UUID branchId = createBranch("SUC-CLI");
        UUID id = createCustomer("Hotel Miramar", CustomerType.HOTEL, branchId);

        mockMvc.perform(get("/customer/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(id.toString()))
                .andExpect(jsonPath("$.data.name").value("Hotel Miramar"))
                .andExpect(jsonPath("$.data.type").value("HOTEL"))
                .andExpect(jsonPath("$.data.email").value("hotel.miramar@test.com"))
                .andExpect(jsonPath("$.data.branchId").value(branchId.toString()))
                .andExpect(jsonPath("$.data.branchName").value("Sucursal SUC-CLI"));
    }

    @Test
    void rechazaEmailDuplicadoCon409() throws Exception {
        UUID branchId = createBranch("SUC-CLI2");
        createCustomerWithEmail("Hotel Uno", "duplicado@test.com", CustomerType.HOTEL, branchId);

        mockMvc.perform(post("/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hotel Dos\",\"type\":\"HOTEL\",\"email\":\"DUPLICADO@test.com\","
                                + "\"phone\":\"555\",\"branchId\":\"" + branchId + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Recurso duplicado"));
    }

    @Test
    void devuelve404CuandoLaSucursalNoExiste() throws Exception {
        mockMvc.perform(post("/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cliente Sin Sucursal\",\"type\":\"STORE\",\"email\":\"sinsuc@test.com\","
                                + "\"branchId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"));
    }

    @Test
    void rechazaValidacionCon400() throws Exception {
        mockMvc.perform(post("/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"type\":\"HOTEL\",\"email\":\"ok@test.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validacion fallida"))
                .andExpect(jsonPath("$.errors[0]").value("name: El nombre es obligatorio"));

        mockMvc.perform(post("/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cliente Tipo Invalido\",\"type\":\"NO_EXISTE\",\"email\":\"x@test.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Solicitud mal formada"));
    }

    @Test
    void actualizaCliente() throws Exception {
        UUID branchId = createBranch("SUC-CLI3");
        UUID id = createCustomer("Escuela Original", CustomerType.SCHOOL, branchId);

        mockMvc.perform(put("/customer/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Escuela Renombrada\",\"type\":\"SCHOOL\","
                                + "\"email\":\"escuela@test.com\",\"phone\":\"5599999999\",\"branchId\":\"" + branchId
                                + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Escuela Renombrada"))
                .andExpect(jsonPath("$.data.phone").value("5599999999"));

        mockMvc.perform(get("/customer/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Escuela Renombrada"));
    }

    @Test
    void listaClientesFiltrandoPorTipoYSucursal() throws Exception {
        UUID branchId = createBranch("SUC-CLI4");
        UUID otraBranchId = createBranch("SUC-CLI5");
        UUID hotelId = createCustomer("Hotel Filtro", CustomerType.HOTEL, branchId);
        UUID storeId = createCustomer("Tienda Filtro", CustomerType.STORE, otraBranchId);

        mockMvc.perform(get("/customer?type=HOTEL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].id", hasItem(hotelId.toString())))
                .andExpect(jsonPath("$.data.items[*].id", not(hasItem(storeId.toString()))));

        mockMvc.perform(get("/customer?branchId=" + branchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].id", hasItem(hotelId.toString())))
                .andExpect(jsonPath("$.data.items[*].id", not(hasItem(storeId.toString()))));

        mockMvc.perform(get("/customer?type=HOTEL&branchId=" + otraBranchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void devuelve404CuandoElClienteNoExiste() throws Exception {
        mockMvc.perform(get("/customer/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"));
    }

    @Test
    void eliminaClienteSinOrdenes() throws Exception {
        UUID branchId = createBranch("SUC-CLI6");
        UUID id = createCustomer("Cliente Borrable", CustomerType.STORE, branchId);

        mockMvc.perform(delete("/customer/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());

        mockMvc.perform(get("/customer/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void noEliminaClienteConOrdenes() throws Exception {
        UUID branchId = createBranch("SUC-CLI7");
        UUID categoryId = createCategory("Categoria Cliente");
        UUID productId = createProduct("SKU-CLIENTE", categoryId, "10.00");
        UUID customerId = createCustomer("Cliente Con Orden", CustomerType.HOTEL, branchId);
        createOrder(customerId, branchId, productId, 1);

        mockMvc.perform(delete("/customer/{id}", customerId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Recurso en uso"))
                .andExpect(jsonPath("$.detail")
                        .value("No se puede eliminar el cliente porque tiene ordenes asociadas"));

        mockMvc.perform(get("/customer/{id}", customerId))
                .andExpect(status().isOk());
    }

    @Test
    void acotaElLimiteA100() throws Exception {
        mockMvc.perform(get("/customer?limit=500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.limit").value(100));
    }
}
