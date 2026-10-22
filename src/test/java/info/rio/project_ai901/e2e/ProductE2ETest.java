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

class ProductE2ETest extends AbstractIntegrationTest {

    @Test
    void creaProductoYLoObtienePorId() throws Exception {
        UUID categoryId = createCategory("Lacteos");
        UUID id = createProduct("SKU-001", categoryId, "25.50");

        mockMvc.perform(get("/product/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(id.toString()))
                .andExpect(jsonPath("$.data.sku").value("SKU-001"))
                .andExpect(jsonPath("$.data.unit").value("pieza"))
                .andExpect(jsonPath("$.data.price").value(25.50))
                .andExpect(jsonPath("$.data.categoryId").value(categoryId.toString()))
                .andExpect(jsonPath("$.data.categoryName").value("Lacteos"));
    }

    @Test
    void listaProductosPaginado() throws Exception {
        UUID categoryId = createCategory("Categoria Paginada");
        createProduct("SKU-PAG-1", categoryId, "10.00");
        createProduct("SKU-PAG-2", categoryId, "20.00");
        createProduct("SKU-PAG-3", categoryId, "30.00");

        String response = getJson("/product?limit=2");
        int total = JsonPath.parse(response).read("$.data.total", Integer.class);
        int items = JsonPath.parse(response).read("$.data.items.length()", Integer.class);
        Assertions.assertThat(total).isGreaterThanOrEqualTo(3);
        Assertions.assertThat(items).isEqualTo(2);

        mockMvc.perform(get("/product?limit=2&offset=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.offset").value(2));
    }

    @Test
    void rechazaSkuDuplicadoCon409() throws Exception {
        UUID categoryId = createCategory("Categoria SKU");
        createProduct("SKU-DUP", categoryId, "10.00");

        mockMvc.perform(post("/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"sku-dup\",\"name\":\"Otro\",\"categoryId\":\"" + categoryId
                                + "\",\"unit\":\"caja\",\"price\":99.99}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Recurso duplicado"))
                .andExpect(jsonPath("$.detail").value("Ya existe un producto con el SKU: sku-dup"));
    }

    @Test
    void devuelve404CuandoLaCategoriaNoExiste() throws Exception {
        mockMvc.perform(post("/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-SINCAT\",\"name\":\"Producto\",\"categoryId\":\"" + UUID.randomUUID()
                                + "\",\"unit\":\"pieza\",\"price\":10.00}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"));
    }

    @Test
    void rechazaValidacionCon400() throws Exception {
        UUID categoryId = createCategory("Categoria Validacion");

        mockMvc.perform(post("/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"\",\"name\":\"Producto\",\"categoryId\":\"" + categoryId
                                + "\",\"unit\":\"pieza\",\"price\":10.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validacion fallida"))
                .andExpect(jsonPath("$.errors[0]").value("sku: El SKU es obligatorio"));

        mockMvc.perform(post("/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-CERO\",\"name\":\"Producto\",\"categoryId\":\"" + categoryId
                                + "\",\"unit\":\"pieza\",\"price\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value("price: El precio debe ser mayor a 0"));
    }

    @Test
    void actualizaProducto() throws Exception {
        UUID categoryId = createCategory("Categoria Upd");
        UUID otherCategoryId = createCategory("Categoria Upd 2");
        UUID id = createProduct("SKU-UPD", categoryId, "10.00");

        mockMvc.perform(put("/product/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-UPD-2\",\"name\":\"Producto Actualizado\",\"categoryId\":\""
                                + otherCategoryId + "\",\"unit\":\"caja\",\"price\":55.50}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sku").value("SKU-UPD-2"))
                .andExpect(jsonPath("$.data.price").value(55.50))
                .andExpect(jsonPath("$.data.categoryName").value("Categoria Upd 2"));

        mockMvc.perform(get("/product/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Producto Actualizado"));
    }

    @Test
    void devuelve404CuandoElProductoNoExiste() throws Exception {
        mockMvc.perform(get("/product/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"));
    }

    @Test
    void eliminaProductoSinInventario() throws Exception {
        UUID categoryId = createCategory("Categoria Del");
        UUID id = createProduct("SKU-DEL", categoryId, "10.00");

        mockMvc.perform(delete("/product/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());

        mockMvc.perform(get("/product/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void noEliminaProductoConInventario() throws Exception {
        UUID branchId = createBranch("SUC-PROD");
        UUID warehouseId = createWarehouse("ALM-PROD", branchId);
        UUID categoryId = createCategory("Categoria Bloq");
        UUID id = createProduct("SKU-BLOQ", categoryId, "10.00");
        createInventory(warehouseId, id, 5);

        mockMvc.perform(delete("/product/{id}", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Recurso en uso"));

        mockMvc.perform(get("/product/{id}", id))
                .andExpect(status().isOk());
    }

    @Test
    void acotaElLimiteA100() throws Exception {
        mockMvc.perform(get("/product?limit=500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.limit").value(100));
    }
}
