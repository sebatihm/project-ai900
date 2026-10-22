package info.rio.project_ai901.support;

import com.jayway.jsonpath.JsonPath;
import info.rio.project_ai901.models.enums.CustomerType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base para pruebas end-to-end. Levanta el contexto completo contra H2 (profile
 * {@code test}) con MockMvc. Cada prueba corre en una transaccion que se revierte
 * al finalizar, por lo que no hay datos residuales entre pruebas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected String getJson(String path) throws Exception {
        return mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
    }

    protected String postJson(String path, String body) throws Exception {
        return mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
    }

    protected String dataField(String response, String field) {
        return JsonPath.parse(response).read("$.data." + field, String.class);
    }

    protected UUID createBranch(String code) throws Exception {
        String response = postJson("/branch",
                "{\"code\":\"" + code + "\",\"name\":\"Sucursal " + code + "\",\"city\":\"CDMX\"}");
        return UUID.fromString(dataField(response, "branchId"));
    }

    protected UUID createWarehouse(String code, UUID branchId) throws Exception {
        String response = postJson("/warehouse",
                "{\"code\":\"" + code + "\",\"name\":\"Almacen " + code + "\",\"branchId\":\"" + branchId + "\"}");
        return UUID.fromString(dataField(response, "warehouseId"));
    }

    protected UUID createCategory(String name) throws Exception {
        String response = postJson("/category", "{\"name\":\"" + name + "\"}");
        return UUID.fromString(dataField(response, "id"));
    }

    protected UUID createProduct(String sku, UUID categoryId, String price) throws Exception {
        String response = postJson("/product",
                "{\"sku\":\"" + sku + "\",\"name\":\"Producto " + sku + "\",\"categoryId\":\"" + categoryId
                        + "\",\"unit\":\"pieza\",\"price\":" + price + "}");
        return UUID.fromString(dataField(response, "id"));
    }

    protected UUID createCustomer(String name, CustomerType type, UUID branchId) throws Exception {
        String email = name.toLowerCase().replaceAll("[^a-z0-9]+", ".") + "@test.com";
        return createCustomerWithEmail(name, email, type, branchId);
    }

    protected UUID createCustomerWithEmail(String name, String email, CustomerType type, UUID branchId)
            throws Exception {
        String response = postJson("/customer",
                "{\"name\":\"" + name + "\",\"type\":\"" + type + "\",\"email\":\"" + email
                        + "\",\"phone\":\"5551234567\",\"branchId\":\"" + branchId + "\"}");
        return UUID.fromString(dataField(response, "id"));
    }

    protected UUID createInventory(UUID warehouseId, UUID productId, int quantity) throws Exception {
        String response = postJson("/inventory",
                "{\"warehouseId\":\"" + warehouseId + "\",\"productId\":\"" + productId
                        + "\",\"quantity\":" + quantity + "}");
        return UUID.fromString(dataField(response, "id"));
    }

    protected UUID createOrder(UUID customerId, UUID branchId, UUID productId, int quantity) throws Exception {
        String response = postJson("/order", orderBody(customerId, branchId, productId, quantity));
        return UUID.fromString(dataField(response, "id"));
    }

    protected String orderBody(UUID customerId, UUID branchId, UUID productId, int quantity) {
        return "{\"customerId\":\"" + customerId + "\",\"branchId\":\"" + branchId + "\","
                + "\"items\":[{\"productId\":\"" + productId + "\",\"quantity\":" + quantity + "}]}";
    }

    protected UUID createTicket(UUID customerId, UUID orderId, String subject, String channel) throws Exception {
        String orderPart = orderId != null ? "\"orderId\":\"" + orderId + "\"," : "";
        String response = postJson("/ticket",
                "{\"customerId\":\"" + customerId + "\"," + orderPart
                        + "\"subject\":\"" + subject + "\",\"channel\":\"" + channel + "\"}");
        return UUID.fromString(dataField(response, "id"));
    }

    protected UUID createResolvedTicket(UUID customerId, UUID orderId, String subject) throws Exception {
        UUID ticketId = createTicket(customerId, orderId, subject, "EMAIL");
        mockMvc.perform(put("/ticket/{id}/status", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\"}"))
                .andExpect(status().isOk());
        return ticketId;
    }

    protected UUID addTicketMessage(UUID ticketId, String author, String content) throws Exception {
        String response = postJson("/ticket/" + ticketId + "/message",
                "{\"author\":\"" + author + "\",\"content\":\"" + content + "\"}");
        return UUID.fromString(dataField(response, "id"));
    }
}
