package info.rio.project_ai901.e2e;

import com.jayway.jsonpath.JsonPath;
import info.rio.project_ai901.models.enums.CustomerType;
import info.rio.project_ai901.support.AbstractIntegrationTest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderE2ETest extends AbstractIntegrationTest {

    @Test
    void creaOrdenConCodigoAutogeneradoYTotalCalculado() throws Exception {
        UUID branchId = createBranch("SUC-ORD-1");
        UUID categoryId = createCategory("Categoria Orden");
        UUID productId = createProduct("SKU-ORD-1", categoryId, "25.50");
        UUID customerId = createCustomer("Hotel Orden", CustomerType.HOTEL, branchId);
        UUID orderId = createOrder(customerId, branchId, productId, 2);

        String response = getJson("/order/" + orderId);
        String code = JsonPath.parse(response).read("$.data.code", String.class);

        Assertions.assertThat(code).matches("ORD-\\d{8}-\\d{4}");

        Assertions.assertThat(response)
                .contains("\"status\":\"RECEIVED\"")
                .contains("\"customerName\":\"Hotel Orden\"")
                .contains("\"branchCode\":\"SUC-ORD-1\"")
                .contains("\"quantity\":2")
                .contains("\"unitPrice\":25.50")
                .contains("\"lineTotal\":51.00")
                .contains("\"total\":51.00");
    }

    @Test
    void conservaElUnitPriceComoSnapshotDelPrecioDelProducto() throws Exception {
        UUID branchId = createBranch("SUC-ORD-2");
        UUID categoryId = createCategory("Categoria Snapshot");
        UUID productId = createProduct("SKU-ORD-2", categoryId, "25.00");
        UUID customerId = createCustomer("Tienda Snapshot", CustomerType.STORE, branchId);
        UUID orderId = createOrder(customerId, branchId, productId, 2);

        mockMvc.perform(put("/product/{id}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-ORD-2\",\"name\":\"Producto Snapshot\",\"categoryId\":\""
                                + categoryId + "\",\"unit\":\"pieza\",\"price\":99.99}"))
                .andExpect(status().isOk());

        String response = getJson("/order/" + orderId);
        Assertions.assertThat(response)
                .contains("\"unitPrice\":25.00")
                .contains("\"total\":50.00");
    }

    @Test
    void recalculaElTotalAlEditarLosItems() throws Exception {
        UUID branchId = createBranch("SUC-ORD-3");
        UUID categoryId = createCategory("Categoria Recalc");
        UUID productId = createProduct("SKU-ORD-3", categoryId, "25.00");
        UUID customerId = createCustomer("Hotel Recalc", CustomerType.HOTEL, branchId);
        UUID orderId = createOrder(customerId, branchId, productId, 2);

        mockMvc.perform(get("/order/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(50.00));

        mockMvc.perform(put("/order/{id}", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(customerId, branchId, productId, 4)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(100.00))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].quantity").value(4));
    }

    @Test
    void aplicaTransicionesDeEstadoValidas() throws Exception {
        UUID branchId = createBranch("SUC-ORD-4");
        UUID categoryId = createCategory("Categoria Estados");
        UUID productId = createProduct("SKU-ORD-4", categoryId, "10.00");
        UUID customerId = createCustomer("Hotel Estados", CustomerType.HOTEL, branchId);
        UUID orderId = createOrder(customerId, branchId, productId, 1);

        mockMvc.perform(put("/order/{id}/status", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PREPARING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PREPARING"));

        mockMvc.perform(put("/order/{id}/status", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SHIPPED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SHIPPED"));

        mockMvc.perform(put("/order/{id}/status", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DELIVERED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DELIVERED"));
    }

    @Test
    void rechazaTransicionInvalidaCon400() throws Exception {
        UUID branchId = createBranch("SUC-ORD-5");
        UUID categoryId = createCategory("Categoria Transicion");
        UUID productId = createProduct("SKU-ORD-5", categoryId, "10.00");
        UUID customerId = createCustomer("Hotel Transicion", CustomerType.HOTEL, branchId);
        UUID orderId = createOrder(customerId, branchId, productId, 1);

        mockMvc.perform(put("/order/{id}/status", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DELIVERED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Solicitud invalida"))
                .andExpect(jsonPath("$.detail")
                        .value(containsString("Transicion de estado no permitida: RECEIVED -> DELIVERED")));
    }

    @Test
    void rechazaTransicionAlMismoEstadoCon400() throws Exception {
        UUID branchId = createBranch("SUC-ORD-6");
        UUID categoryId = createCategory("Categoria Mismo Estado");
        UUID productId = createProduct("SKU-ORD-6", categoryId, "10.00");
        UUID customerId = createCustomer("Hotel Mismo Estado", CustomerType.HOTEL, branchId);
        UUID orderId = createOrder(customerId, branchId, productId, 1);

        mockMvc.perform(put("/order/{id}/status", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RECEIVED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("La orden ya se encuentra en el estado RECEIVED"));
    }

    @Test
    void soloPermiteEditarOrdenesEnEstadoReceived() throws Exception {
        UUID branchId = createBranch("SUC-ORD-7");
        UUID categoryId = createCategory("Categoria Edicion");
        UUID productId = createProduct("SKU-ORD-7", categoryId, "10.00");
        UUID customerId = createCustomer("Hotel Edicion", CustomerType.HOTEL, branchId);
        UUID orderId = createOrder(customerId, branchId, productId, 1);

        mockMvc.perform(put("/order/{id}/status", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PREPARING\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/order/{id}", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(customerId, branchId, productId, 3)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Recurso en uso"))
                .andExpect(jsonPath("$.detail", containsString("Solo se puede editar una orden en estado RECEIVED")));
    }

    @Test
    void soloPermiteEliminarOrdenesCanceladas() throws Exception {
        UUID branchId = createBranch("SUC-ORD-8");
        UUID categoryId = createCategory("Categoria Eliminar");
        UUID productId = createProduct("SKU-ORD-8", categoryId, "10.00");
        UUID customerId = createCustomer("Hotel Eliminar", CustomerType.HOTEL, branchId);
        UUID orderId = createOrder(customerId, branchId, productId, 1);

        mockMvc.perform(delete("/order/{id}", orderId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Recurso en uso"))
                .andExpect(jsonPath("$.detail",
                        containsString("Solo se puede eliminar una orden en estado CANCELED")));

        mockMvc.perform(put("/order/{id}/status", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELED"));

        mockMvc.perform(delete("/order/{id}", orderId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/order/{id}", orderId))
                .andExpect(status().isNotFound());
    }

    @Test
    void rechazaOrdenSinItemsCon400() throws Exception {
        UUID branchId = createBranch("SUC-ORD-9");
        UUID customerId = createCustomer("Hotel Sin Items", CustomerType.HOTEL, branchId);

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"" + customerId + "\",\"branchId\":\"" + branchId
                                + "\",\"items\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validacion fallida"))
                .andExpect(jsonPath("$.errors[0]").value("items: La orden debe tener al menos un item"));
    }

    @Test
    void devuelve404CuandoElProductoDeLaOrdenNoExiste() throws Exception {
        UUID branchId = createBranch("SUC-ORD-10");
        UUID customerId = createCustomer("Hotel Producto 404", CustomerType.HOTEL, branchId);

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"" + customerId + "\",\"branchId\":\"" + branchId
                                + "\",\"items\":[{\"productId\":\"" + UUID.randomUUID() + "\",\"quantity\":1}]}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail", containsString("Producto no encontrado")));
    }

    @Test
    void listaOrdenesFiltrandoPorEstadoClienteYSucursal() throws Exception {
        UUID branchIdA = createBranch("SUC-ORD-A");
        UUID branchIdB = createBranch("SUC-ORD-B");
        UUID categoryId = createCategory("Categoria Filtro");
        UUID productId = createProduct("SKU-ORD-F", categoryId, "10.00");
        UUID customerA = createCustomer("Hotel Filtro A", CustomerType.HOTEL, branchIdA);
        UUID customerB = createCustomer("Tienda Filtro B", CustomerType.STORE, branchIdB);
        UUID orderA = createOrder(customerA, branchIdA, productId, 1);
        UUID orderB = createOrder(customerB, branchIdB, productId, 2);

        mockMvc.perform(put("/order/{id}/status", orderB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PREPARING\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/order?status=PREPARING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].id", hasItem(orderB.toString())))
                .andExpect(jsonPath("$.data.items[*].id", not(hasItem(orderA.toString()))));

        mockMvc.perform(get("/order?customerId=" + customerA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].id", hasItem(orderA.toString())))
                .andExpect(jsonPath("$.data.items[*].id", not(hasItem(orderB.toString()))));

        mockMvc.perform(get("/order?branchId=" + branchIdB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].id", hasItem(orderB.toString())))
                .andExpect(jsonPath("$.data.items[*].id", not(hasItem(orderA.toString()))));
    }

    @Test
    void acotaElLimiteA100() throws Exception {
        mockMvc.perform(get("/order?limit=500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.limit").value(100));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void devuelve409AlEliminarProductoReferenciadoPorUnaOrden() throws Exception {
        UUID branchId = createBranch("SUC-INTEG");
        UUID categoryId = createCategory("Categoria Integridad");
        UUID productId = createProduct("SKU-INTEG", categoryId, "30.00");
        UUID customerId = createCustomer("Cliente Integridad", CustomerType.HOTEL, branchId);
        UUID orderId = createOrder(customerId, branchId, productId, 1);

        try {
            mockMvc.perform(delete("/product/{id}", productId))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title").value("Conflicto de integridad"));
        } finally {
            limpiaDatosDeLaPruebaDeIntegridad(orderId, customerId, branchId, productId, categoryId);
        }
    }

    private void limpiaDatosDeLaPruebaDeIntegridad(UUID orderId, UUID customerId, UUID branchId, UUID productId,
            UUID categoryId) {
        try {
            mockMvc.perform(put("/order/{id}/status", orderId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"CANCELED\"}"));
            mockMvc.perform(delete("/order/{id}", orderId));
            mockMvc.perform(delete("/product/{id}", productId));
            mockMvc.perform(delete("/customer/{id}", customerId));
            mockMvc.perform(delete("/category/{id}", categoryId));
            mockMvc.perform(delete("/branch/{id}", branchId));
        } catch (Exception ignored) {
            // la limpieza no debe enmascarar el resultado de la prueba
        }
    }
}
