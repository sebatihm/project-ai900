package info.rio.project_ai901.e2e;

import com.jayway.jsonpath.JsonPath;
import info.rio.project_ai901.support.AbstractIntegrationTest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CategoryE2ETest extends AbstractIntegrationTest {

    @Test
    void creaCategoriaYLaObtienePorId() throws Exception {
        UUID id = createCategory("Bebidas");

        mockMvc.perform(get("/category/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(id.toString()))
                .andExpect(jsonPath("$.data.name").value("Bebidas"));
    }

    @Test
    void listaCategoriasPaginado() throws Exception {
        createCategory("Cat Pag 1");
        createCategory("Cat Pag 2");
        createCategory("Cat Pag 3");

        String response = getJson("/category?limit=2");
        Assertions.assertThat(response).contains("\"limit\":2");
        int total = JsonPath.parse(response).read("$.data.total", Integer.class);
        int items = JsonPath.parse(response).read("$.data.items.length()", Integer.class);
        Assertions.assertThat(total).isGreaterThanOrEqualTo(3);
        Assertions.assertThat(items).isEqualTo(2);

        mockMvc.perform(get("/category?limit=2&offset=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.offset").value(2));
    }

    @Test
    void rechazaNombreDuplicadoCon409() throws Exception {
        createCategory("Postres");

        mockMvc.perform(post("/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"postres\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Recurso duplicado"))
                .andExpect(jsonPath("$.detail").value("Ya existe una categoria con el nombre: postres"));
    }

    @Test
    void rechazaValidacionCon400() throws Exception {
        mockMvc.perform(post("/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validacion fallida"))
                .andExpect(jsonPath("$.errors[0]").value("name: El nombre es obligatorio"));
    }

    @Test
    void actualizaCategoria() throws Exception {
        UUID id = createCategory("Snacks");

        mockMvc.perform(put("/category/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Snacks Saludables\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Snacks Saludables"));

        mockMvc.perform(get("/category/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Snacks Saludables"));
    }

    @Test
    void devuelve404CuandoLaCategoriaNoExiste() throws Exception {
        mockMvc.perform(get("/category/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"));
    }

    @Test
    void eliminaCategoriaSinProductos() throws Exception {
        UUID id = createCategory("Temporal");

        mockMvc.perform(delete("/category/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());

        mockMvc.perform(get("/category/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void noEliminaCategoriaConProductos() throws Exception {
        UUID id = createCategory("Activa");
        createProduct("SKU-CAT-BLOQ", id, "10.00");

        mockMvc.perform(delete("/category/{id}", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Recurso en uso"));

        mockMvc.perform(get("/category/{id}", id))
                .andExpect(status().isOk());
    }

    @Test
    void acotaElLimiteA100() throws Exception {
        mockMvc.perform(get("/category?limit=500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.limit").value(100));
    }
}
