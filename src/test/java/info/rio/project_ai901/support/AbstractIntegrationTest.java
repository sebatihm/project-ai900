package info.rio.project_ai901.support;

import com.jayway.jsonpath.JsonPath;
import info.rio.project_ai901.models.Role;
import info.rio.project_ai901.models.User;
import info.rio.project_ai901.models.enums.CustomerType;
import info.rio.project_ai901.models.enums.RoleName;
import info.rio.project_ai901.repositories.RoleRepository;
import info.rio.project_ai901.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import info.rio.project_ai901.repositories.WarehouseRepository;

/**
 * Base para pruebas end-to-end. Levanta el contexto completo contra H2 (profile
 * {@code test}) con MockMvc. Cada prueba corre en una transaccion que se revierte
 * al finalizar, por lo que no hay datos residuales entre pruebas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "e2e@ai901.local", roles = "ADMIN")
@Transactional
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected RoleRepository roleRepository;

    @Autowired
    protected WarehouseRepository warehouseRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    private UUID defaultWarehouseId;

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

    protected UUID defaultWarehouseId() {
        if (defaultWarehouseId == null) {
            try {
                String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
                UUID branchId = createBranch("B" + suffix);
                defaultWarehouseId = createWarehouse("W" + suffix, branchId);
            } catch (Exception e) {
                throw new IllegalStateException("No se pudo crear el almacen por defecto de pruebas", e);
            }
        }
        return defaultWarehouseId;
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

    protected UUID createRole(RoleName name) {
        Role role = roleRepository.findByName(name).orElseGet(() -> {
            Role nuevo = new Role();
            nuevo.setName(name);
            return roleRepository.save(nuevo);
        });
        return role.getId();
    }

    protected UUID createUser(String name, String email, String rawPassword, RoleName roleName) {
        return createUser(name, email, rawPassword, roleName, defaultWarehouseId());
    }

    protected UUID createUser(String name, String email, String rawPassword, RoleName roleName, UUID warehouseId) {
        createRole(roleName);
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException("Rol no encontrado: " + roleName));
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setWarehouse(warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new IllegalStateException("Almacen no encontrado: " + warehouseId)));
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user).getId();
    }

    protected String login(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.parse(response).read("$.data.token", String.class);
    }

    protected String getJsonWithToken(String path, String token) throws Exception {
        return mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
    }

    protected String postJsonWithToken(String path, String body, String token) throws Exception {
        return mockMvc.perform(post(path)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
    }

    protected String putJsonWithToken(String path, String body, String token) throws Exception {
        return mockMvc.perform(put(path)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
    }

    protected String deleteWithToken(String path, String token) throws Exception {
        return mockMvc.perform(delete(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
    }
}
