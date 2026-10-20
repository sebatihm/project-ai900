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

class InventoryE2ETest extends AbstractIntegrationTest {

    @Test
    void creaInventarioYLoObtienePorId() throws Exception {
        UUID branchId = createBranch("SUC-INV");
        UUID warehouseId = createWarehouse("ALM-INV", branchId);
        UUID categoryId = createCategory("Categoria Inv");
        UUID productId = createProduct("SKU-INV", categoryId, "12.00");
        UUID inventoryId = createInventory(warehouseId, productId, 8);

        mockMvc.perform(get("/inventory/{id}", inventoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(inventoryId.toString()))
                .andExpect(jsonPath("$.data.warehouseId").value(warehouseId.toString()))
                .andExpect(jsonPath("$.data.warehouseCode").value("ALM-INV"))
                .andExpect(jsonPath("$.data.branchId").value(branchId.toString()))
                .andExpect(jsonPath("$.data.productId").value(productId.toString()))
                .andExpect(jsonPath("$.data.productSku").value("SKU-INV"))
                .andExpect(jsonPath("$.data.quantity").value(8));
    }

    @Test
    void rechazaParAlmacenProductoDuplicadoCon409() throws Exception {
        UUID branchId = createBranch("SUC-INV2");
        UUID warehouseId = createWarehouse("ALM-INV2", branchId);
        UUID categoryId = createCategory("Categoria Inv 2");
        UUID productId = createProduct("SKU-INV2", categoryId, "12.00");
        createInventory(warehouseId, productId, 3);

        mockMvc.perform(post("/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"warehouseId\":\"" + warehouseId + "\",\"productId\":\"" + productId
                                + "\",\"quantity\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Recurso duplicado"))
                .andExpect(jsonPath("$.detail").value(
                        "Ya existe inventario para el almacen ALM-INV2 y el producto SKU-INV2"));
    }

    @Test
    void devuelve404CuandoElAlmacenNoExiste() throws Exception {
        UUID categoryId = createCategory("Categoria Inv 3");
        UUID productId = createProduct("SKU-INV3", categoryId, "12.00");

        mockMvc.perform(post("/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"warehouseId\":\"" + UUID.randomUUID() + "\",\"productId\":\"" + productId
                                + "\",\"quantity\":1}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"));
    }

    @Test
    void devuelve404CuandoElProductoNoExiste() throws Exception {
        UUID branchId = createBranch("SUC-INV4");
        UUID warehouseId = createWarehouse("ALM-INV4", branchId);

        mockMvc.perform(post("/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"warehouseId\":\"" + warehouseId + "\",\"productId\":\"" + UUID.randomUUID()
                                + "\",\"quantity\":1}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void devuelve400CuandoElAlmacenNoEstaActivo() throws Exception {
        UUID branchId = createBranch("SUC-INV5");
        UUID warehouseId = createWarehouse("ALM-INV5", branchId);
        UUID categoryId = createCategory("Categoria Inv 5");
        UUID productId = createProduct("SKU-INV5", categoryId, "12.00");

        mockMvc.perform(delete("/warehouse/{id}", warehouseId))
                .andExpect(status().isOk());

        mockMvc.perform(post("/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"warehouseId\":\"" + warehouseId + "\",\"productId\":\"" + productId
                                + "\",\"quantity\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Solicitud invalida"))
                .andExpect(jsonPath("$.detail").value("El almacen no esta activo"));
    }

    @Test
    void rechazaValidacionCon400() throws Exception {
        mockMvc.perform(post("/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"warehouseId\":\"" + UUID.randomUUID() + "\",\"productId\":\""
                                + UUID.randomUUID() + "\",\"quantity\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validacion fallida"))
                .andExpect(jsonPath("$.errors[0]").value("quantity: La cantidad no puede ser negativa"));
    }

    @Test
    void listaInventarioFiltrandoPorAlmacenYProducto() throws Exception {
        UUID branchId = createBranch("SUC-INV6");
        UUID warehouseId = createWarehouse("ALM-INV6", branchId);
        UUID otroWarehouseId = createWarehouse("ALM-INV6-B", branchId);
        UUID categoryId = createCategory("Categoria Inv 6");
        UUID productId = createProduct("SKU-INV6-A", categoryId, "12.00");
        UUID otroProductId = createProduct("SKU-INV6-B", categoryId, "15.00");
        createInventory(warehouseId, productId, 4);
        createInventory(otroWarehouseId, otroProductId, 6);

        mockMvc.perform(get("/inventory?warehouseId=" + warehouseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].warehouseId", hasItem(warehouseId.toString())))
                .andExpect(jsonPath("$.data.items[*].warehouseId", not(hasItem(otroWarehouseId.toString()))));

        mockMvc.perform(get("/inventory?productId=" + otroProductId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].productId", hasItem(otroProductId.toString())))
                .andExpect(jsonPath("$.data.items[*].productId", not(hasItem(productId.toString()))));

        mockMvc.perform(get("/inventory?warehouseId=" + warehouseId + "&productId=" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void actualizaCantidadDeInventario() throws Exception {
        UUID branchId = createBranch("SUC-INV7");
        UUID warehouseId = createWarehouse("ALM-INV7", branchId);
        UUID categoryId = createCategory("Categoria Inv 7");
        UUID productId = createProduct("SKU-INV7", categoryId, "12.00");
        UUID inventoryId = createInventory(warehouseId, productId, 2);

        mockMvc.perform(put("/inventory/{id}", inventoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":25}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quantity").value(25));

        mockMvc.perform(get("/inventory/{id}", inventoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quantity").value(25));
    }

    @Test
    void eliminaInventario() throws Exception {
        UUID branchId = createBranch("SUC-INV8");
        UUID warehouseId = createWarehouse("ALM-INV8", branchId);
        UUID categoryId = createCategory("Categoria Inv 8");
        UUID productId = createProduct("SKU-INV8", categoryId, "12.00");
        UUID inventoryId = createInventory(warehouseId, productId, 1);

        mockMvc.perform(delete("/inventory/{id}", inventoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());

        mockMvc.perform(get("/inventory/{id}", inventoryId))
                .andExpect(status().isNotFound());
    }

    @Test
    void acotaElLimiteA100() throws Exception {
        mockMvc.perform(get("/inventory?limit=500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.limit").value(100));
    }
}
