package info.rio.project_ai901.e2e;

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

class WarehouseE2ETest extends AbstractIntegrationTest {

    @Test
    void creaAlmacenYLoObtienePorId() throws Exception {
        UUID branchId = createBranch("SUC-W1");
        UUID warehouseId = createWarehouse("ALM-01", branchId);

        mockMvc.perform(get("/warehouse/{id}", warehouseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.warehouseId").value(warehouseId.toString()))
                .andExpect(jsonPath("$.data.code").value("ALM-01"))
                .andExpect(jsonPath("$.data.active").value(true))
                .andExpect(jsonPath("$.data.branchId").value(branchId.toString()))
                .andExpect(jsonPath("$.data.branchName").value("Sucursal SUC-W1"));
    }

    @Test
    void listaAlmacenesFiltrandoPorSucursal() throws Exception {
        UUID branchId = createBranch("SUC-W2");
        UUID otraBranchId = createBranch("SUC-W3");
        createWarehouse("ALM-PROPIO", branchId);
        createWarehouse("ALM-AJENO", otraBranchId);

        mockMvc.perform(get("/warehouse?branchId=" + branchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].code", hasItem("ALM-PROPIO")))
                .andExpect(jsonPath("$.data.items[*].code", not(hasItem("ALM-AJENO"))));

        mockMvc.perform(get("/warehouse?branchId=" + otraBranchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].code", hasItem("ALM-AJENO")));
    }

    @Test
    void rechazaCodigoDuplicadoCon409() throws Exception {
        UUID branchId = createBranch("SUC-W4");
        createWarehouse("ALM-DUP", branchId);

        mockMvc.perform(post("/warehouse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"alm-dup\",\"name\":\"Otro\",\"branchId\":\"" + branchId + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Recurso duplicado"));
    }

    @Test
    void rechazaValidacionCon400() throws Exception {
        mockMvc.perform(post("/warehouse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"\",\"name\":\"Almacen\",\"branchId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validacion fallida"))
                .andExpect(jsonPath("$.errors[0]").value("code: El codigo es obligatorio"));

        mockMvc.perform(post("/warehouse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"ALM-X\",\"name\":\"Almacen X\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value("branchId: La sucursal es obligatoria"));
    }

    @Test
    void devuelve400CuandoLaSucursalNoExiste() throws Exception {
        mockMvc.perform(post("/warehouse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"ALM-NF\",\"name\":\"Almacen\",\"branchId\":\"" + UUID.randomUUID()
                                + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Solicitud invalida"));
    }

    @Test
    void devuelve400CuandoLaSucursalNoEstaActiva() throws Exception {
        UUID branchId = createBranch("SUC-INACTIVA-W");
        mockMvc.perform(delete("/branch/{id}", branchId))
                .andExpect(status().isOk());

        mockMvc.perform(post("/warehouse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"ALM-INY\",\"name\":\"Almacen\",\"branchId\":\"" + branchId + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("La sucursal no esta activa: " + branchId));
    }

    @Test
    void actualizaAlmacen() throws Exception {
        UUID branchId = createBranch("SUC-W5");
        UUID warehouseId = createWarehouse("ALM-UPD", branchId);

        mockMvc.perform(put("/warehouse/{id}", warehouseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"ALM-UPD\",\"name\":\"Almacen Renombrado\",\"branchId\":\"" + branchId
                                + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Almacen Renombrado"));

        mockMvc.perform(get("/warehouse/{id}", warehouseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Almacen Renombrado"));
    }

    @Test
    void devuelve404CuandoElAlmacenNoExiste() throws Exception {
        mockMvc.perform(get("/warehouse/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"));
    }

    @Test
    void desactivaAlmacenSinInventario() throws Exception {
        UUID branchId = createBranch("SUC-W6");
        UUID warehouseId = createWarehouse("ALM-DEL", branchId);

        mockMvc.perform(delete("/warehouse/{id}", warehouseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));
    }

    @Test
    void noDesactivaAlmacenConInventario() throws Exception {
        UUID branchId = createBranch("SUC-W7");
        UUID warehouseId = createWarehouse("ALM-BLOQ", branchId);
        UUID categoryId = createCategory("Categoria Almacen");
        UUID productId = createProduct("SKU-ALM-BLOQ", categoryId, "15.00");
        createInventory(warehouseId, productId, 10);

        mockMvc.perform(delete("/warehouse/{id}", warehouseId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Recurso en uso"));

        mockMvc.perform(get("/warehouse/{id}", warehouseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    void acotaElLimiteA100() throws Exception {
        mockMvc.perform(get("/warehouse?limit=500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.limit").value(100));
    }
}
