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

class BranchE2ETest extends AbstractIntegrationTest {

    @Test
    void creaSucursalYLaObtienePorId() throws Exception {
        UUID id = createBranch("SUC-01");

        mockMvc.perform(get("/branch/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.branchId").value(id.toString()))
                .andExpect(jsonPath("$.data.code").value("SUC-01"))
                .andExpect(jsonPath("$.data.city").value("CDMX"))
                .andExpect(jsonPath("$.data.active").value(true))
                .andExpect(jsonPath("$.data.warehouseCount").value(0));
    }

    @Test
    void listaSoloSucursalesActivasPorDefecto() throws Exception {
        createBranch("SUC-ACTIVA");
        UUID inactiva = createBranch("SUC-INACTIVA");
        mockMvc.perform(delete("/branch/{id}", inactiva))
                .andExpect(status().isOk());

        mockMvc.perform(get("/branch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].code", hasItem("SUC-ACTIVA")))
                .andExpect(jsonPath("$.data.items[*].code", not(hasItem("SUC-INACTIVA"))));
    }

    @Test
    void listaIncluyendoInactivosCuandoSePide() throws Exception {
        UUID inactiva = createBranch("SUC-OCULTA");
        mockMvc.perform(delete("/branch/{id}", inactiva))
                .andExpect(status().isOk());

        mockMvc.perform(get("/branch?includeInactive=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].code", hasItem("SUC-OCULTA")));
    }

    @Test
    void rechazaCodigoDuplicadoCon409() throws Exception {
        createBranch("SUC-DUP");

        mockMvc.perform(post("/branch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"suc-dup\",\"name\":\"Otra\",\"city\":\"Puebla\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Recurso duplicado"))
                .andExpect(jsonPath("$.detail").value("Ya existe una sucursal con el codigo: suc-dup"));
    }

    @Test
    void rechazaValidacionEnBlancoCon400() throws Exception {
        mockMvc.perform(post("/branch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"\",\"name\":\"Sucursal\",\"city\":\"CDMX\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validacion fallida"))
                .andExpect(jsonPath("$.errors[0]").value("code: El codigo es obligatorio"));
    }

    @Test
    void actualizaSucursal() throws Exception {
        UUID id = createBranch("SUC-UPD");

        mockMvc.perform(put("/branch/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"SUC-UPD\",\"name\":\"Sucursal Actualizada\",\"city\":\"Guadalajara\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.city").value("Guadalajara"))
                .andExpect(jsonPath("$.data.name").value("Sucursal Actualizada"));

        mockMvc.perform(get("/branch/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.city").value("Guadalajara"));
    }

    @Test
    void devuelve404CuandoLaSucursalNoExiste() throws Exception {
        mockMvc.perform(get("/branch/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"));
    }

    @Test
    void desactivaSucursal() throws Exception {
        UUID id = createBranch("SUC-DEL");

        mockMvc.perform(delete("/branch/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));

        mockMvc.perform(get("/branch/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));
    }

    @Test
    void noDesactivaSucursalConAlmacenesActivos() throws Exception {
        UUID branchId = createBranch("SUC-BLOQ");
        createWarehouse("ALM-BLOQ", branchId);

        mockMvc.perform(delete("/branch/{id}", branchId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Recurso en uso"));

        mockMvc.perform(get("/branch/{id}", branchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    void acotaElLimiteA100() throws Exception {
        mockMvc.perform(get("/branch?limit=500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.limit").value(100));
    }
}
